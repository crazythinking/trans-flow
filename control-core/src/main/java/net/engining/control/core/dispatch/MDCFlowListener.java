package net.engining.control.core.dispatch;

import org.slf4j.MDC;

import net.engining.control.core.flow.FlowContext;
import net.engining.control.core.flow.FlowDefinition;
import net.engining.control.core.invoker.InvokerDefinition;

/**
 * FlowTrans监听类，对日志输出MDC增强
 * @author Eric Lu
 *
 */
public class MDCFlowListener implements FlowListener {

	@Override
	public void beforeFlow(String flowCode, FlowDefinition definition, FlowContext context) {
		MDC.put("flow", flowCode);
	}

	@Override
	public void afterFlow(String flowCode, FlowDefinition definition, FlowContext context) {
		MDC.remove("skipped invoker");
		MDC.remove("invoker");
		MDC.remove("flow");
	}

	@Override
	public void beforeInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context) {
		MDC.put("invoker", invokerDefinition.name());
	}

	@Override
	public void afterInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context) {
		MDC.remove("invoker");
	}

	@Override
	public void skippedInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context) {
		MDC.put("skipped invoker", invokerDefinition.name());
	}

}
