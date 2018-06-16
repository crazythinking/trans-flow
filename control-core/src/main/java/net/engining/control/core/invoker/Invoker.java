package net.engining.control.core.invoker;

import net.engining.control.core.flow.FlowContext;

/**
 * Invoker必须实现的接口
 * @author Eric Lu
 *
 */
public interface Invoker {

	void invoke(FlowContext ctx);
	
}
