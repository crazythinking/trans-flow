package net.engining.control.core.dispatch;


import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import net.engining.control.api.ContextKey;
import net.engining.control.api.FlowDispatcher;
import net.engining.control.core.flow.AbstractFlow;
import net.engining.control.core.flow.FlowDefinition;

/**
 * FlowTrans Dispatcher的简单实现，主要是完成必须的初始化和Flow的执行方法调用
 * @author Eric Lu
 *
 */
public class SimpleFlowDispatcher implements FlowDispatcher, InitializingBean {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private List<AbstractFlow> flows;
	
	private Map<String, AbstractFlow> flowMap;

	@Override
	public Map<Class<? extends ContextKey<?>>, Object> process(String flowCode, Map<Class<? extends ContextKey<?>>, Object> request) {
		AbstractFlow flowTrans = flowMap.get(flowCode);
		// TODO 改为签名判断，主要是考虑安全性，具体怎么用，还没想清楚
		checkNotNull(flowTrans);
		return flowTrans.execute(flowCode, request);
	}
	
	@Override
	public void afterPropertiesSet() throws Exception {
		
		Map<String, AbstractFlow> map = Maps.newHashMap();
		for (AbstractFlow transaction : flows)
		{
			Class<? extends AbstractFlow> clazz = transaction.getClass();
			FlowDefinition fd = clazz.getAnnotation(FlowDefinition.class);
			checkNotNull(fd);

			String code = fd.code();
			if (StringUtils.isBlank(code))
			{
				code = clazz.getSimpleName();
			}
			
			if (map.containsKey(code))
			{
				logger.warn("[{}]对应的流程代码[{}]已经存在，忽略。", clazz.getCanonicalName(), code);
				continue;
			}
			map.put(code, transaction);
			
			logger.info("映射流程代码[{}]到流程处理类[{}]", code, clazz.getCanonicalName());
		}	
		
		flowMap = ImmutableMap.copyOf(map);
	}
}
