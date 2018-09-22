package net.engining.control.core.invoker;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import net.engining.control.core.flow.FlowContext;

/**
 * @author luxue
 *
 */
public abstract class AbstractSkippableInvoker implements Invoker, Skippable {

	@Override
	public boolean skippable(Map<String, String> parameters) {
		// 之前步骤操作成功的情况下，不跳过
		String junSuccess = parameters.get(FlowContext.CONS_PARAMETERS.SUCCESS.toString());
		if (!(StringUtils.isNotBlank(junSuccess) && FlowContext.CONS_PARAMETERS.SUCCESS.toString().equals(junSuccess))) {
			return true;
		}
		return false;
	}

}
