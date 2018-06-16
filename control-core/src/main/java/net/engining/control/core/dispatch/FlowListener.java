package net.engining.control.core.dispatch;

import net.engining.control.core.flow.FlowContext;
import net.engining.control.core.flow.FlowDefinition;
import net.engining.control.core.invoker.InvokerDefinition;

public interface FlowListener {

	void beforeFlow(String flowCode, FlowDefinition definition, FlowContext context);

	void afterFlow(String flowCode, FlowDefinition definition, FlowContext context);

	void beforeInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context);

	void afterInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context);
	
	void skippedInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context);
}
