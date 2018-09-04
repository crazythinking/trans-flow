package net.engining.control.core.invoker;

import java.util.Date;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import net.engining.control.api.key.AsynIndKey;
import net.engining.control.api.key.ChannelKey;
import net.engining.control.api.key.ChannelRequestSeqKey;
import net.engining.control.api.key.ChannelSignTokenKey;
import net.engining.control.api.key.OnlineDataKey;
import net.engining.control.api.key.RequestIpKey;
import net.engining.control.api.key.RequestUrlKey;
import net.engining.control.api.key.SvPrIdKey;
import net.engining.control.api.key.TargetBizDateKey;
import net.engining.control.api.key.TransIdKey;
import net.engining.control.api.key.TxnDateTimeKey;
import net.engining.control.api.key.TxnVersionKey;
import net.engining.control.core.flow.FlowContext;
import net.engining.control.entity.enums.TransStatusDef;
import net.engining.control.entity.model.CtInboundJournal;
import net.engining.pg.support.utils.ValidateUtilExt;

@InvokerDefinition(
	name = "记录网关接收的交易流水",
	requires = {
			SvPrIdKey.class,
			ChannelKey.class,
			OnlineDataKey.class,
			ChannelRequestSeqKey.class,
			AsynIndKey.class,
	},
	optional = {
			RequestIpKey.class,
			RequestUrlKey.class,
			ChannelSignTokenKey.class,
			TargetBizDateKey.class,
			TxnVersionKey.class,
			TxnDateTimeKey.class
	},
	results = {
		TransIdKey.class,
		OnlineDataKey.class
	}
)
public class WriteInboundJournal implements Invoker, Skippable {
	
	@PersistenceContext 
	private EntityManager em;
	
	@Override
	public void invoke(FlowContext ctx) {
		// 插入交易流水表	
		CtInboundJournal ctInboundJournal = new CtInboundJournal();
		ctInboundJournal.setSvPrId(ctx.get(SvPrIdKey.class));//TODO 默认用spring.application.name
		ctInboundJournal.setTxnSerialNo(ctx.get(ChannelRequestSeqKey.class));
		ctInboundJournal.setChannelId(ctx.get(ChannelKey.class));
		ctInboundJournal.setAsynInd(ctx.get(AsynIndKey.class));
		ctInboundJournal.setTransStatus(TransStatusDef.P);
		ctInboundJournal.setRequestMsg(ctx.get(OnlineDataKey.class));
		ctInboundJournal.setTransCode(ctx.getFlowCode());
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(TargetBizDateKey.class))){
			ctInboundJournal.setTgBizDate(ctx.get(TargetBizDateKey.class));
		}
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(ChannelSignTokenKey.class))){
			ctInboundJournal.setSignToken(ctx.get(ChannelSignTokenKey.class));
		}
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(TxnDateTimeKey.class))){
			ctInboundJournal.setTxnDatetime(ctx.get(TxnDateTimeKey.class));
		} 
		else {
			ctInboundJournal.setTxnDatetime(new Date());
		}
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(RequestIpKey.class))){
			ctInboundJournal.setRequestIp(ctx.get(RequestIpKey.class));
		}
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(RequestUrlKey.class))){
			ctInboundJournal.setRequestUrl(ctx.get(RequestUrlKey.class));
		}
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(TxnVersionKey.class))){
			ctInboundJournal.setTransVersion(ctx.get(TxnVersionKey.class));
		}
		ctInboundJournal.fillDefaultValues();
		em.persist(ctInboundJournal);
		
		ctx.put(TransIdKey.class, ctInboundJournal.getInboundId());
		ctx.put(OnlineDataKey.class, ctInboundJournal.getRequestMsg());
	}

	@Override
	public boolean skippable(Map<String, String> parameters) {
		String skip = parameters.get(FlowContext.CONS_PARAMETERS.SKIP.toString());
		if(FlowContext.SKIP_TRUE.equals(skip)){
			return true;
		}
		return false;
	}

}
