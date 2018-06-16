package net.engining.control.core.test.flow00;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.engining.control.core.flow.FlowContext;
import net.engining.control.core.invoker.Invoker;
import net.engining.control.core.invoker.InvokerDefinition;
import net.engining.control.core.invoker.Skippable;
import net.engining.control.core.test.support.IntValue2Key;

/**
 * 把 {@link IntValue2Key} 作平方后写入 {@link IntValue2Key}
 * @author binarier
 *
 */
@InvokerDefinition(
	name = "square invoker2",
	requires = IntValue2Key.class,
	results = IntValue2Key.class
)
public class SampleInvoker2 implements Invoker, Skippable
{
	@Override
	public void invoke(FlowContext ctx)
	{
		int value = ctx.get(IntValue2Key.class);
		value = value * value;
		
		ctx.put(IntValue2Key.class, value);
	}

	@Override
	public boolean skippable(Map<String, String> parameters) {
		if(StringUtils.isNoneBlank(parameters.get("skip"))){
			return true;
		}
		return false;
	}


}
