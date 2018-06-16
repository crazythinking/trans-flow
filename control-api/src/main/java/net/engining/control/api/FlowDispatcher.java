package net.engining.control.api;

import java.util.Map;

/**
 * FlowTrans的
 * @author Eric Lu
 *
 */
public interface FlowDispatcher {

	/**
	 * 处理交易的方法
	 * 
	 * @param flowCode 交易码
	 * @param request
	 * @return
	 */
	public Map<Class<? extends ContextKey<?>>, Object> process(String flowCode, Map<Class<? extends ContextKey<?>>, Object> request);
		
}
