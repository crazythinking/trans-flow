package net.engining.control.core.test.flow00;

import net.engining.control.core.flow.AbstractFlow;
import net.engining.control.core.flow.FlowDefinition;
import net.engining.control.core.invoker.TransactionSeperator;
import net.engining.control.core.test.support.IntValue2Key;

@FlowDefinition(
	code = "sample",
	name = "示列流程",
	desc = "示列流程. 用于测试",
	invokers = {
		SampleInvoker1.class,
		TransactionSeperator.class,
		SampleInvoker2.class
	},
	response = {
		IntValue2Key.class
	}
)
public class SampleFlow extends AbstractFlow
{
}
