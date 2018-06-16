package net.engining.control.core.invoker;

import org.springframework.stereotype.Service;

import net.engining.control.core.flow.FlowContext;

/**
 * 
 * @author Eric Lu
 *
 */
@Service
@InvokerDefinition(name="事务分割器")
public class TransactionSeperator implements Invoker {

	@Override
	public void invoke(FlowContext ctx) {
		
	}
}
