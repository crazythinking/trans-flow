package net.engining.control.core.invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.engining.control.api.FinalResult;
import net.engining.control.api.key.ErrorMessagesKey;
import net.engining.control.api.key.FinalResultKey;
import net.engining.control.core.flow.FlowContext;
import net.engining.pg.support.core.exception.ErrorCode;
import net.engining.pg.support.core.exception.ErrorMessageException;
import net.engining.pg.support.utils.ValidateUtilExt;

@InvokerDefinition(
	name = "结果处理",
	results = {
			ErrorMessagesKey.class,
			FinalResultKey.class
			}
	)
public class DetermineFinalResult implements Invoker {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void invoke(FlowContext ctx)
	{
		if (ValidateUtilExt.isNotNullOrEmpty(ctx.getLastExceptions()))
		{
			ctx.put(FinalResultKey.class, FinalResult.Failed);

			for(Exception e : ctx.getLastExceptions()){
				if (e instanceof ErrorMessageException)
				{
					ctx.putMap(ErrorMessagesKey.class, ((ErrorMessageException) e).getErrorCode(), e.getClass().getCanonicalName()+":"+e.getMessage());
				}
				else
				{
					ctx.putMap(ErrorMessagesKey.class, ErrorCode.SystemError, e.getClass().getCanonicalName()+":"+e.getMessage());
				}
			}
		}
		else
		{
			if (!ctx.contains(FinalResultKey.class))
			{
				if (ctx.contains(ErrorMessagesKey.class))
				{
					ctx.put(FinalResultKey.class, FinalResult.Failed);
				}
				else
				{
					ctx.put(FinalResultKey.class, FinalResult.Success);
				}
			}
			else{
				if(ctx.get(FinalResultKey.class).equals(FinalResult.Failed)){
					ctx.putMap(ErrorMessagesKey.class, ErrorCode.UnknowFail, ErrorCode.UnknowFail.getLabel());
				}
			}
		}
		
		logger.info("交易处理结束");
		logger.info("返回交易结果：" + ctx.get(FinalResultKey.class));
	}

}
