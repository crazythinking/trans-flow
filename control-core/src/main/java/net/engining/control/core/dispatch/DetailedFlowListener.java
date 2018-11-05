package net.engining.control.core.dispatch;

import java.util.Map.Entry;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

import net.engining.control.api.ContextKey;
import net.engining.control.core.flow.FlowContext;
import net.engining.control.core.flow.FlowDefinition;
import net.engining.control.core.invoker.InvokerDefinition;
import net.engining.pg.support.core.exception.ErrorCode;
import net.engining.pg.support.core.exception.ErrorMessageException;
import net.engining.pg.support.utils.ExceptionUtilsExt;

/**
 * FlowTrans的监听类，主要从FlowTrans执行的前后，以及其内部各Invoker执行的前后，增强处理
 * @author Eric Lu
 *
 */
public class DetailedFlowListener implements FlowListener {
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	/**
	 * Flow执行前是否打印输入的上下文属性
	 */
	private boolean dumpRequest = true;
	
	/**
	 * Flow执行前是否打印输出的上下文属性
	 */
	private boolean dumpResponse = true;
	
	/**
	 * Invoker执行过程中的上下文属性
	 */
	private boolean dumpProcedure = true;
	
	@Override
	public void beforeFlow(String flowCode, FlowDefinition definition, FlowContext context){
		logger.info("开始交易流程[TRADE_TYPE:{}]", flowCode);
		if (dumpRequest && logger.isDebugEnabled())
		{
			dumpContext("输入的上下文", context);
		}
	}

	@Override
	public void afterFlow(String flowCode, FlowDefinition definition, FlowContext context) {
		String stauts = "S";
		if(Optional.fromNullable(context.getLastExceptions()).isPresent()){
			stauts = "F";
		}
		logger.info("结束交易流程[TRADE_TYPE:{}, TRADE_STATUS:{}]", flowCode, stauts);
		if (dumpResponse && logger.isDebugEnabled())
		{
			dumpContext("输出的上下文", context);
		}
		
		if(Optional.fromNullable(context.getLastExceptions()).isPresent()){
			for(Exception ex: context.getLastExceptions()){
				if(ex instanceof RuntimeException && !(ex instanceof ErrorMessageException)){//ErrorMessageException这类异常通常是业务检查异常，可以被监控忽略
					logger.error("执行过程中存在异常：[{}:{}], ERROR_CODE:{} ", ex.getClass().getCanonicalName(), ex.getMessage(), ErrorCode.SystemError.getValue());
				}
				else{
					logger.error("执行过程中存在异常：[{}:{}]", ex.getClass().getCanonicalName(), ex.getMessage());
				}
				ExceptionUtilsExt.dump(ex);
			}
		}
		
	}

	@Override
	public void beforeInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context) {
		if (dumpProcedure && logger.isDebugEnabled()){
			logger.debug("开始交易流程[{}]内-Invoker[{}]", flowCode, invokerDefinition.name());
			dumpContext("执行前的上下文", context);
		}
	}

	@Override
	public void afterInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context) {
		if (dumpProcedure && logger.isDebugEnabled()){
			logger.debug("结束交易流程[{}]内-Invoker[{}]", flowCode, invokerDefinition.name());
			dumpContext("执行后的上下文", context);
		}
	}

	@Override
	public void skippedInvoker(String flowCode, InvokerDefinition invokerDefinition, FlowContext context) {
		if (dumpProcedure && logger.isDebugEnabled()){
			logger.debug("交易流程[{}]内-Invoker[{}]，符合条件被跳过执行！", flowCode, invokerDefinition.name());
			
		}
	}

	private void dumpContext(String title, FlowContext context)
	{
		StringBuilder sb = new StringBuilder();
		sb.append('\n');
		sb.append(title);
		sb.append('\n');
		sb.append("-------------------------------\n");
		for (Entry<Class<? extends ContextKey<?>>, Object> entry : context.getContextKeyValueMap().entrySet())
		{
			sb.append("|\t");
			sb.append(StringUtils.remove(entry.getKey().getSimpleName(), "Key"));
			sb.append(" : ");
			sb.append(String.valueOf(entry.getValue()));
			sb.append('\n');
		}
		for(String key : context.getParameters().keySet()){
			sb.append("|\t");
			sb.append(key);
			sb.append(" : ");
			sb.append(context.getParameters().get(key));
			sb.append('\n');
		}
		sb.append("-------------------------------\n");
		logger.debug(sb.toString());
	}

	public boolean isDumpRequest() {
		return dumpRequest;
	}

	public void setDumpRequest(boolean dumpRequest) {
		this.dumpRequest = dumpRequest;
	}

	public boolean isDumpResponse() {
		return dumpResponse;
	}

	public void setDumpResponse(boolean dumpResponse) {
		this.dumpResponse = dumpResponse;
	}

	public boolean isDumpProcedure() {
		return dumpProcedure;
	}

	public void setDumpProcedure(boolean dumpProcedure) {
		this.dumpProcedure = dumpProcedure;
	}

}
