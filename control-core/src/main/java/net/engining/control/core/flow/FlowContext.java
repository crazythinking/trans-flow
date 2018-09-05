package net.engining.control.core.flow;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.engining.control.api.ContextKey;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Flow的上下文容器，用于存放FlowTrans执行过程中数据
 * 
 * @author Eric Lu
 *
 */
public class FlowContext {
	
	public static enum CONS_PARAMETERS {
		SKIP,
		FAIL,
		SUCCESS,
		ROCKETMQ_TRANS_MSG,//RocketMq事务消息，以json string保存
		ROCKETMQ_TRANS_MSG_ARG,//RocketMq事务消息对应参数，以json string保存
		TRANS_MSG_SERIALID//事务消息唯一流水ID
	}
	
	public static String SKIP_TRUE = "TRUE";
	
	/**
	 * Flow的代码
	 */
	private String flowCode;

	/**
	 * Flow的名称
	 */
	private String flowName;

	/**
	 * Flow的描述
	 */
	private String flowDesc;

	/**
	 * 上下文属性，包括输入和输出属性
	 */
	private Map<Class<? extends ContextKey<?>>, Object> contextKeyValueMap;

	/**
	 * FlowTrans执行过程中使用的参数
	 */
	private Map<String, String> parameters;

	/**
	 * 执行过程中最终产生的异常堆栈
	 */
	private List<Exception> lastExceptions = Lists.newArrayList();

	/**
	 * 上下文属性对应的ContextKey
	 * 
	 * @return
	 */
	public Set<Class<? extends ContextKey<?>>> keySet() {
		return contextKeyValueMap.keySet();
	}

	public String getFlowCode() {
		return flowCode;
	}

	public void setFlowCode(String flowCode) {
		this.flowCode = flowCode;
	}

	public String getFlowName() {
		return flowName;
	}

	public void setFlowName(String flowName) {
		this.flowName = flowName;
	}

	public void setContextKeyValueMap(Map<Class<? extends ContextKey<?>>, Object> map) {
		this.contextKeyValueMap = map;
	}

	public Map<Class<? extends ContextKey<?>>, Object> getContextKeyValueMap() {
		return contextKeyValueMap;
	}

	/**
	 * 根据ContextKey，获取属性对象
	 * 
	 * @param keyClass
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T get(Class<? extends ContextKey<T>> keyClass) {
		Object obj = contextKeyValueMap.get(keyClass);

		return (T) obj;
	}

	/**
	 * 根据ContextKey，设置属性值
	 * 
	 * @param keyClass
	 * @param value
	 */
	public <T> void put(Class<? extends ContextKey<T>> keyClass, T value) {
		contextKeyValueMap.put(keyClass, value);
	}

	/**
	 * 根据ContextKey，设置List<T>型的属性值
	 * 
	 * @param keyClass
	 * @param value
	 */
	public <T> void addList(Class<? extends ContextKey<? extends List<T>>> keyClass, T value) {
		@SuppressWarnings("unchecked")
		List<T> obj = (List<T>) contextKeyValueMap.get(keyClass);
		if (obj == null) {
			obj = Lists.newArrayList();
			contextKeyValueMap.put(keyClass, obj);
		}

		obj.add(value);
	}

	/**
	 * 根据ContextKey，设置Map<K, V>型的属性值写入; 如果没有，则建一个 {@link LinkedHashMap}
	 * 
	 * @param keyClass
	 * @param key
	 * @param value
	 */
	public <K, V> void putMap(Class<? extends ContextKey<? extends Map<K, V>>> keyClass, K key, V value) {
		checkNotNull(key);
		@SuppressWarnings("unchecked")
		Map<K, V> target = (Map<K, V>) contextKeyValueMap.get(keyClass);
		if (target == null) {
			target = Maps.newLinkedHashMap();
			contextKeyValueMap.put(keyClass, target);
		}

		target.put(key, value);
	}

	/**
	 * 判断是否包含某个ContextKey
	 * 
	 * @param keyClass
	 * @return
	 */
	public boolean contains(Class<? extends ContextKey<?>> keyClass) {
		return contextKeyValueMap.containsKey(keyClass);
	}

	public Map<String, String> getParameters() {
		return parameters;
	}

	public void setParameters(Map<String, String> parameters) {
		this.parameters = parameters;
	}

	public String getFlowDesc() {
		return flowDesc;
	}

	public void setFlowDesc(String flowDesc) {
		this.flowDesc = flowDesc;
	}

	/**
	 * @return the lastExceptions
	 */
	public List<Exception> getLastExceptions() {
		return lastExceptions;
	}

	/**
	 * @param lastExceptions the lastExceptions to set
	 */
	public void addLastExceptions(Exception lastException) {
		this.lastExceptions.add(lastException);
	}

}
