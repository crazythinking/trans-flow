package net.engining.control.core.invoker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.engining.control.api.FinalResult;
import net.engining.control.api.key.ErrorMessagesKey;
import net.engining.control.api.key.FinalResultKey;
import net.engining.control.core.flow.FlowContext;
import net.engining.pg.support.core.exception.ErrorCode;
import net.engining.pg.support.core.exception.ErrorMessageException;

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
		if (ctx.getLastException() != null)
		{
			ctx.put(FinalResultKey.class, FinalResult.Failed);

			Exception e = ctx.getLastException();
			if (e instanceof ErrorMessageException)
			{
				ctx.putMap(ErrorMessagesKey.class, ((ErrorMessageException) e).getErrorCode(), e.getMessage());
			}
			else
			{
				ctx.putMap(ErrorMessagesKey.class, ErrorCode.SystemError, ctx.getLastException().getMessage());
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
			}else{
				if(ctx.get(FinalResultKey.class).equals(FinalResult.Failed) &&
						ctx.getLastException() == null ){
					ctx.putMap(ErrorMessagesKey.class, ErrorCode.SystemError, "系统内部错误");
				}
			}
		}
		
		logger.info("交易处理结束");
		logger.info("返回交易结果：" + ctx.get(FinalResultKey.class));
	}

}
