package net.engining.control.core.invoker;

import java.util.Map;

/**
 * 可跳过invoker
 * @author binarier
 *
 */
public interface Skippable {
	
	/**
	 * 通过parameters内的传值来判断是否可以跳过该Invoker;
	 * 
	 * @param parameters
	 * @return false 表示不跳过该invoke
	 */
	boolean skippable(Map<String, String> parameters);
}
