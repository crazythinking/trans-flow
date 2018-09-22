package net.engining.control.core.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.RandomUtils;
import org.apache.rocketmq.common.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import net.engining.control.api.ContextKey;
import net.engining.control.api.key.SvPrIdKey;
import net.engining.control.api.key.TransCodeKey;
import net.engining.control.api.key.TxnVersionKey;
import net.engining.control.core.dispatch.FlowListener;
import net.engining.control.core.invoker.Invoker;
import net.engining.control.core.invoker.InvokerDefinition;
import net.engining.control.core.invoker.RocketMqTransactional;
import net.engining.control.core.invoker.Skippable;
import net.engining.control.core.invoker.TransactionSeperator;
import net.engining.control.core.transactional.CommonRocketMqTransactionalProducer;
import net.engining.pg.support.core.context.ApplicationContextHolder;
import net.engining.pg.support.core.exception.ErrorCode;
import net.engining.pg.support.core.exception.ErrorMessageException;

/**
 * FlowTrans的Flow抽象，主要用于对FlowTrans中定义的Invoker按事务分隔分组，并依次调用执行
 * @author Eric Lu
 *
 */
public abstract class AbstractFlow implements InitializingBean
{
	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private ConfigurableListableBeanFactory beanFactory;
	
	@Autowired(required = false)
	private FlowListener listeners[] = new FlowListener[0];
	
	@Autowired
	private Environment environment;
	
	/**
	 * Invoker列表，在容器初始化时根据 {@link FlowDefinition} 来设置的实例列表
	 */
	protected List<Invoker> invokers;
	
	/**
	 * InvokerDefinition列表
	 */
	protected Map<Invoker, InvokerDefinition> invokerDefinitions = Maps.newHashMap();

	/**
	 * 用于记录每个invoker对应的必填字段，以便于处理流程中的判断
	 */
	private Map<Class<? extends Invoker>, Set<Class<? extends ContextKey<?>>>> invokerRequires;
	
	/**
	 * 预先Invoker分组
	 */
	protected List<List<Invoker>> invokerGroups;

	protected FlowDefinition flowDefinition;

	@Override
	public void afterPropertiesSet() throws Exception {
		
		// 初始化元数据缓存,@FlowDefinition注解
		flowDefinition = getClass().getAnnotation(FlowDefinition.class);
		
		checkNotNull(flowDefinition, "AbstractFlow - %s 必须定义 @FlowDefintion", getClass().getCanonicalName());
		
		// 出于防御性考虑，建立一个不可变map，记录FlowTrans包含的所有Invokers以及其必输的上下文属性
		List<Class<? extends Invoker>> keys4InvokerClass = Lists.newArrayList();
		Builder<Class<? extends Invoker>, Set<Class<? extends ContextKey<?>>>> builder = ImmutableMap.builder();
		for (Class<? extends Invoker> invokerClass : flowDefinition.invokers()) {
			InvokerDefinition invokerDefinition = invokerClass.getAnnotation(InvokerDefinition.class);

			checkNotNull(invokerDefinition, "Invoker - %s 必须定义 @InvokerDefinition", invokerClass.getCanonicalName());

			// invoker的requires定义不为空
			if(invokerDefinition.requires().length > 0){
				// 相同的invoker不被处理
				if(!keys4InvokerClass.contains(invokerClass)){
					builder.put(invokerClass, ImmutableSet.copyOf(invokerDefinition.requires()));
					keys4InvokerClass.add(invokerClass);
				}
			}
			
		}

		invokerRequires = builder.build();
		logger.debug("Trans Flow: [{}]的必输字段{}", flowDefinition.desc(), JSON.toJSONString(invokerRequires));

		// 建立专用的invoker实例，每个FlowTrans一套
		invokers = Lists.newArrayList();
		invokerGroups = Lists.newArrayList();
		List<Invoker> group = Lists.newArrayList();
		for (Class<? extends Invoker> clazz : flowDefinition.invokers())
		{
			logger.debug("在Spring容器中创建Invoker Bean:[{}]", clazz);
			Invoker invoker;
			if (beanFactory.getBeanNamesForType(clazz).length > 0) {
				invoker = beanFactory.getBean(clazz);
			} else {
				invoker = (Invoker) beanFactory.createBean(clazz, AutowireCapableBeanFactory.AUTOWIRE_BY_TYPE, true);
			}
			invokers.add(invoker);
			
			invokerDefinitions.put(invoker, clazz.getAnnotation(InvokerDefinition.class));

			// 按由TransactionSeperator分开的invoker分事务组依次执行
			if (invoker instanceof TransactionSeperator)
			{
				invokerGroups.add(ImmutableList.copyOf(group));
				group = Lists.newArrayList();
			}
			else
			{
				group.add(invoker);
			}
		}
		//最后一组
		invokerGroups.add(group);
		
		invokerGroups = ImmutableList.copyOf(invokerGroups);
	}

	/**
	 * 执行Flow，依次执行包含的Invoker
	 * @param flowCode
	 * @param request
	 * @return
	 */
	public Map<Class<? extends ContextKey<?>>, Object> execute(String flowCode, Map<Class<? extends ContextKey<?>>, Object> request) {

		// 建立上下文
		FlowContext context = new FlowContext();
		context.setFlowCode(flowCode);
		context.setFlowName(flowDefinition.name());
		context.setFlowDesc(flowDefinition.desc());
		
		// 放入请求数据
		context.setContextKeyValueMap(Maps.newHashMap(request));

		// 放入参数
		String[] parameters = flowDefinition.parameters();
		if(parameters.length % 2 != 0){
			throw new ErrorMessageException(ErrorCode.CheckError, "FlowDefinition定义的parameters必须成对出现，详见parameters的注释");
		}
		HashMap<String, String> paramMap = new HashMap<String, String>();
		for (int i = 0; i < parameters.length; i += 2) {
			paramMap.put(parameters[i], parameters[i + 1]);
		}
		context.setParameters(paramMap);

		//流程前的回调
		for (FlowListener listener : listeners)
		{
			listener.beforeFlow(flowCode, flowDefinition, context);
		}

		for (List<Invoker> group : invokerGroups)
		{
			try
			{
				doExecute(flowCode, group, context);
			}
			catch (RuntimeException e)
			{
				// 不需要了，在listener内打印error日志
				// logger.error("出现RuntimeException:事务回滚[{}:{}]",
				// e.getClass().getCanonicalName(), e.getMessage());
				// ExceptionUtilsExt.dump(e);
				context.addLastExceptions(e);
			}
			catch (Exception e)//@Transactional 默认只对RuntimeException进行回滚
			{
				// logger.error("出现Checked Exception:事务提交[{}:{}]",
				// e.getClass().getCanonicalName(), e.getMessage());
				// ExceptionUtilsExt.dump(e);
				context.addLastExceptions(e);
			}
		}
		
		for (FlowListener listener : listeners)
		{
			listener.afterFlow(flowCode, flowDefinition, context);
		}

		// 生成响应报文
		Map<Class<? extends ContextKey<?>>, Object> response = Maps.newHashMap();

		for (Class<? extends ContextKey<?>> key : flowDefinition.response()) {
			// TODO 判断响应报文是否够字段

			@SuppressWarnings("unchecked")
			Object data = context.get((Class<? extends ContextKey<Object>>) key);
			response.put(key, data);
		}

		return response;
	}

	@SuppressWarnings("unchecked")
	@Transactional
	private void doExecute(String flowCode, List<Invoker> invokers, FlowContext context)
	{
		for (Invoker invoker : invokers)
		{
			InvokerDefinition invokerDefinition = invoker.getClass().getAnnotation(InvokerDefinition.class);

			if (invoker instanceof Skippable && ((Skippable) invoker).skippable(context.getParameters()))
			{
				for (FlowListener listener : listeners)
				{
					listener.skippedInvoker(flowCode, invokerDefinition, context);
				}
				
				continue;
			}

			for (FlowListener listener : listeners)
			{
				listener.beforeInvoker(flowCode, invokerDefinition, context);
			}
			
			// 检查invokerRequires，必输的上下文属性是否都有值
			if(invokerRequires.get(invoker.getClass()) != null){
				for(Class<? extends ContextKey<?>> ckey: invokerRequires.get(invoker.getClass())){
					checkNotNull(context.get((Class<? extends ContextKey<Object>>) ckey), "检查必输的上下文属性, [%s]不可为空", ckey.getCanonicalName());
				}
			}
			//具体调用invoker
			invoker.invoke(context);
			
			// 支持RocketMq分布式事务消息
			if (invoker instanceof RocketMqTransactional)
			{
				((RocketMqTransactional) invoker).convertTransactionalMessage(context.getParameters());
				Preconditions.checkNotNull(context.getParameters().get(FlowContext.CONS_PARAMETERS.ROCKETMQ_TRANS_MSG.toString()), "该invoker需要分布式事务支持，FlowContext.parameters中必须包含ROCKETMQ_TRANS_MSG");
				Preconditions.checkNotNull(context.getParameters().get(FlowContext.CONS_PARAMETERS.TRANS_MSG_SERIALID.toString()), "该invoker需要分布式事务支持，FlowContext.parameters中必须包含TRANS_MSG_SERIALID");
				Message msg = JSONObject.parseObject(context.getParameters().get(FlowContext.CONS_PARAMETERS.ROCKETMQ_TRANS_MSG.toString()), Message.class);
				String serialId = context.getParameters().get(FlowContext.CONS_PARAMETERS.TRANS_MSG_SERIALID.toString());
				//检查message的基本消息，必须包含Topic，tag
				Preconditions.checkNotNull(msg.getTopic(), "RocketMq Transactional message must have topic");
				Preconditions.checkNotNull(msg.getTags(), "RocketMq Transactional message must have tag");
				Preconditions.checkNotNull(msg.getBody(), "RocketMq Transactional message must have body");
				//唯一标识事务消息的key，通常用业务流水号；为确保幂等性，及能够显示出与原始交易的父子关系，其后加上“-[0,100]”
				if(!serialId.equals(msg.getKeys())){
					msg.setKeys(serialId+"-"+RandomUtils.nextInt(0, 100));
				}
				// 使用spring.application.name作为SvPrId
				Preconditions.checkNotNull(environment.getProperty("spring.application.name"), "spring.application.name must be set");
				msg.putUserProperty(SvPrIdKey.class.getCanonicalName(), environment.getProperty("spring.application.name"));
				msg.putUserProperty(TransCodeKey.class.getCanonicalName(), flowCode);
				msg.putUserProperty(TxnVersionKey.class.getCanonicalName(), environment.getProperty("info.version"));
				
				Map<String, Serializable> arg = JSONObject.parseObject(context.getParameters().get(FlowContext.CONS_PARAMETERS.ROCKETMQ_TRANS_MSG_ARG.toString()), Map.class);
				
				CommonRocketMqTransactionalProducer commonRocketMqTransactionProducer = ApplicationContextHolder.getBean(CommonRocketMqTransactionalProducer.class);
				// send transactional message
				Preconditions.checkNotNull(commonRocketMqTransactionProducer, "CommonRocketMqTransactionProducer 需要被注入ApplicationContext");
				commonRocketMqTransactionProducer.sendMessageInTransaction(msg, arg);
				
			}

			for (FlowListener listener : listeners)
			{
				listener.afterInvoker(flowCode, invokerDefinition, context);
			}
		}
	}
	
	public List<Invoker> getInvokers() {
		return invokers;
	}

	public void setInvokers(List<Invoker> invokers) {
		this.invokers = invokers;
	}

	public Map<Class<? extends Invoker>, Set<Class<? extends ContextKey<?>>>> getInvokerRequires() {
		return invokerRequires;
	}
}
