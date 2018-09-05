package net.engining.control.core.invoker;

import java.util.Date;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;

import com.google.common.base.Preconditions;

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
import net.engining.pg.web.AsynInd;

@InvokerDefinition(
	name = "记录网关接收的交易流水",
	requires = {
			OnlineDataKey.class,
			ChannelRequestSeqKey.class,
	},
	optional = {
			SvPrIdKey.class,
			ChannelKey.class,
			AsynIndKey.class,
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
	
	@Autowired
	private Environment environment;
	
	@Override
	public void invoke(FlowContext ctx) {
		Preconditions.checkNotNull(environment.getProperty("spring.application.name"), "spring.application.name must be set");
		
		// 插入交易流水表	
		CtInboundJournal ctInboundJournal = new CtInboundJournal();
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(SvPrIdKey.class))){
			ctInboundJournal.setSvPrId(ctx.get(SvPrIdKey.class));
		}
		else {
			// 默认用spring.application.name
			ctInboundJournal.setSvPrId(environment.getProperty("spring.application.name"));
		}
		ctInboundJournal.setTxnSerialNo(ctx.get(ChannelRequestSeqKey.class));
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(ChannelKey.class))){
			ctInboundJournal.setChannelId(ctx.get(ChannelKey.class));
		}
		else {
			// 默认用Unknow
			ctInboundJournal.setChannelId("Unknow");
		}
		if(ValidateUtilExt.isNotNullOrEmpty(ctx.get(AsynIndKey.class))){
			ctInboundJournal.setAsynInd(ctx.get(AsynIndKey.class));
		}
		else {
			// 默认用AsynInd.S|同步
			ctInboundJournal.setAsynInd(AsynInd.S);
		}
		
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
