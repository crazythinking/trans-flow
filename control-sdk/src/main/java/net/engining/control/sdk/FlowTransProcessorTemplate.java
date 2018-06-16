package net.engining.control.sdk;

import java.util.Map;

import net.engining.control.api.ContextKey;
import net.engining.control.api.FlowDispatcher;

/**
 * FlowTrans 处理模版类
 * @author Eric Lu
 *
 */
public class FlowTransProcessorTemplate {

	private FlowDispatcher flowDispatcher;

	public FlowDispatcher getTransactionProcessor() {
		return flowDispatcher;
	}

	public void setTransactionProcessor(FlowDispatcher transactionProcessor) {
		this.flowDispatcher = transactionProcessor;
	}

	public <REQ extends AbstractFlowTransPayload, RES extends AbstractFlowTransPayload> RES process(REQ request, Class<RES> responseClass) {
		Map<Class<? extends ContextKey<?>>, Object> responseMap = flowDispatcher.process(request.getCode(), request.getDataMap());
		try {
			RES response = responseClass.newInstance();
			response.setCode(request.getCode());
			//TODO 考虑安全性，应加上签名
			response.setDataMap(responseMap);
			return response;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
