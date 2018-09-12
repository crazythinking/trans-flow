package net.engining.control.core.invoker;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import net.engining.control.api.FinalResult;
import net.engining.control.api.ResponseData;
import net.engining.control.api.key.ErrorMessagesKey;
import net.engining.control.api.key.FinalResultKey;
import net.engining.control.api.key.ResponseDataKey;
import net.engining.control.api.key.TransIdKey;
import net.engining.control.core.flow.FlowContext;
import net.engining.control.entity.enums.TransStatusDef;
import net.engining.control.entity.model.CtErrorJournal;
import net.engining.control.entity.model.CtInboundJournal;
import net.engining.pg.support.core.exception.ErrorCode;
import net.engining.pg.support.utils.ValidateUtilExt;

@InvokerDefinition(
		name = "更新联机交易日志流水",
		requires = {
				TransIdKey.class
			}, 
		optional = {
				ResponseDataKey.class,
				ErrorMessagesKey.class
			}, 
		results = {
				ResponseDataKey.class
			}
		)
public class WriteJournalUpdateResult implements Invoker {

	private static final Logger log = LoggerFactory.getLogger(WriteJournalUpdateResult.class);

	@PersistenceContext
	private EntityManager em;

	@Override
	public void invoke(FlowContext ctx) {

		String transId = ctx.get(TransIdKey.class);
		CtInboundJournal ctInboundJournal = em.find(CtInboundJournal.class, transId);
		ResponseData responseData = ctx.get(ResponseDataKey.class);
		
		CtErrorJournal ctErrorJournal = new CtErrorJournal();
		if (responseData != null) {
			if (responseData.getReturnCode().equals(ErrorCode.Success.getValue())) {
				ctInboundJournal.setTransStatus(TransStatusDef.S);
				ctInboundJournal.setResponseMsg(JSON.toJSONString(responseData));
			} 
			else {
				ctInboundJournal.setTransStatus(TransStatusDef.F);
				ctInboundJournal.setResponseMsg(JSON.toJSONString(responseData));
				
				//记录异常
				ctErrorJournal.setErrorCode(responseData.getReturnCode());
				ctErrorJournal.setErrorReason(responseData.getReturnDesc());
				ctErrorJournal.setInboundId(ctInboundJournal.getInboundId());
				if(ValidateUtilExt.isNotNullOrEmpty(ctx.getLastExceptions())) {
					Map<ErrorCode, String> errors = ctx.get(ErrorMessagesKey.class);
					ctErrorJournal.setExceptionRec(JSON.toJSONString(errors));
				}
				ctErrorJournal.fillDefaultValues();
				em.persist(ctErrorJournal);
			}
		} 
		else {
			FinalResult resultFlag = ctx.get(FinalResultKey.class);

			if (resultFlag.equals(FinalResult.Success)) {
				ctInboundJournal.setTransStatus(TransStatusDef.S);
			} 
			else {
				ctInboundJournal.setTransStatus(TransStatusDef.F);
				
				//记录异常
				ctErrorJournal.setInboundId(ctInboundJournal.getInboundId());
				if(ValidateUtilExt.isNotNullOrEmpty(ctx.getLastExceptions())) {
					Map<ErrorCode, String> errors = ctx.get(ErrorMessagesKey.class);
					ctErrorJournal.setExceptionRec(JSON.toJSONString(errors));
				}
				ctErrorJournal.fillDefaultValues();
				em.persist(ctErrorJournal);
			}
		}
	}

}
