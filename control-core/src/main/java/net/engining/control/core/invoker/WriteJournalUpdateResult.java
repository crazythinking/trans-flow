package net.engining.control.core.invoker;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;

import net.engining.control.api.FinalResult;
import net.engining.control.api.ResponseData;
import net.engining.control.api.key.FinalResultKey;
import net.engining.control.api.key.ResponseDataKey;
import net.engining.control.api.key.TransIdKey;
import net.engining.control.core.flow.FlowContext;
import net.engining.control.entity.enums.TransStatusDef;
import net.engining.control.entity.model.CtErrorJournal;
import net.engining.control.entity.model.CtInboundJournal;
import net.engining.pg.web.WebCommonResponse;

@InvokerDefinition(
		name = "更新联机日志处理结果",
		requires = {
				TransIdKey.class
			}, 
		optional = {
				ResponseDataKey.class
			}, 
		results = {
				ResponseDataKey.class
			}
		)
public class WriteJournalUpdateResult implements Invoker, Skippable {

	private static final Logger log = LoggerFactory.getLogger(WriteJournalUpdateResult.class);

	@PersistenceContext
	private EntityManager em;

	@Override
	public void invoke(FlowContext ctx) {

		CtErrorJournal ctErrorJournal = new CtErrorJournal();
		String transId = ctx.get(TransIdKey.class);
		CtInboundJournal ctInboundJournal = em.find(CtInboundJournal.class, transId);
		String responseMsg = null;
		ResponseData responseData = ctx.get(ResponseDataKey.class);
		/**
		 * 判断response是否存在
		 * 1、response存在会出现交易异常
		 * 2、response不存在会出现系统异常，或者本身没有下游系统
		 */
		if (responseData != null) {
			responseMsg = JSON.toJSONString(responseData.getReturnData());
			if (responseData.getReturnCode().equals(WebCommonResponse.CODE_OK)) {
				ctInboundJournal.setTransStatus(TransStatusDef.S);
				ctInboundJournal.setRequestMsg(responseMsg);
			} else {
				ctInboundJournal.setTransStatus(TransStatusDef.F);
				ctErrorJournal.setErrorCode(responseData.getReturnCode());
				ctErrorJournal.setErrorReason(responseData.getReturnDesc());
				ctErrorJournal.setInboundId(ctInboundJournal.getInboundId());
				ctErrorJournal.setCreateTime(ctInboundJournal.getCreateTime());
				ctErrorJournal.setUpdateTime(ctInboundJournal.getUpdateTime());
				ctErrorJournal.setJpaVersion(ctInboundJournal.getJpaVersion());
				if(ctx.getLastException()!=null) {
					ctErrorJournal.setExceptionRec(ctx.getLastException().getMessage());
				}
				em.persist(ctErrorJournal);
			}
		} else {
			FinalResult resultFlag = ctx.get(FinalResultKey.class);

			if (resultFlag.equals(FinalResult.Success)) {
				ctInboundJournal.setTransStatus(TransStatusDef.S);
			} else {
				ctInboundJournal.setTransStatus(TransStatusDef.F);
				ctErrorJournal.setInboundId(ctInboundJournal.getInboundId());
				ctErrorJournal.setCreateTime(ctInboundJournal.getCreateTime());
				ctErrorJournal.setUpdateTime(ctInboundJournal.getUpdateTime());
				ctErrorJournal.setJpaVersion(ctInboundJournal.getJpaVersion());
				if(ctx.getLastException()!=null) {
					ctErrorJournal.setExceptionRec(ctx.getLastException().getMessage());
				}
				em.persist(ctErrorJournal);
			}
		}
	}

	@Override
	public boolean skippable(Map<String, String> parameters) {
		String skip = parameters.get(FlowContext.CONS_PARAMETERS.SKIP.toString());
		if (FlowContext.SKIP_TRUE.equals(skip)) {
			return true;
		}
		return false;
	}

}
