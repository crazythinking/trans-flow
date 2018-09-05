package net.engining.control.core.transactional;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSON;
import com.google.common.base.Optional;
import com.maihaoche.starter.mq.annotation.MQTransactionProducer;
import com.maihaoche.starter.mq.base.AbstractMQTransactionProducer;
import com.maihaoche.starter.mq.config.MQProperties;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import net.engining.control.api.key.SvPrIdKey;
import net.engining.control.api.key.TargetBizDateKey;
import net.engining.control.api.key.TransCodeKey;
import net.engining.control.entity.enums.TransStatusDef;
import net.engining.control.entity.model.CtInboundJournal;
import net.engining.control.entity.model.QCtInboundJournal;
import net.engining.pg.support.core.exception.ErrorCode;
import net.engining.pg.support.core.exception.ErrorMessageException;
import net.engining.pg.support.utils.ExceptionUtilsExt;
import net.engining.pg.web.AsynInd;

/**
 * @author luxue
 *
 */
@MQTransactionProducer(producerGroup = "${spring.application.name}-transaction-group")
public class CommonRocketMqTransactionalProducer extends AbstractMQTransactionProducer {

	private static final Logger log = LoggerFactory.getLogger(CommonRocketMqTransactionalProducer.class);
	
	@PersistenceContext 
	private EntityManager em;
	
	@Autowired
	MQProperties mqProperties;
	
	@Override
	@Transactional
	@SuppressWarnings("unchecked")
	public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
		try {
			Map<String, Serializable> argMap = (Map<String, Serializable>) arg;
			
			// 插入交易流水表作为回查数据，与业务操作在同一个本地事务内
			// 如果数据操作失败，需要回滚，会返回RocketMQ一个失败消息ROLLBACK_MESSAGE，意味着 消费者无法消费到这条失败的消息
			// 如果成功，就要返回rocketMQ成功的消息COMMIT_MESSAGE，意味着消费者将读取到这条消息
			// TODO 考虑重试事务消息时的逻辑，直接更新该条事务交易流水
			CtInboundJournal ctInboundJournal = new CtInboundJournal();
			ctInboundJournal.setAsynInd(AsynInd.A);
			//根据RocketMQ Producer最佳实践，一个应用只用一个Topic，故使用topic作为标识自身的标志
			ctInboundJournal.setChannelId(msg.getTopic());
			ctInboundJournal.setProcessTime(new Date());
			ctInboundJournal.setRequestMsg(JSON.toJSONString(msg));
			ctInboundJournal.setConfirmCount(new Integer(0));
			ctInboundJournal.setRetryCount(new Integer(0));
			ctInboundJournal.setSvPrId(msg.getUserProperty(SvPrIdKey.class.getCanonicalName()));
			ctInboundJournal.setTransCode(msg.getUserProperty(TransCodeKey.class.getCanonicalName()));
			DateTimeFormatter format = DateTimeFormat.forPattern("yyyy-MM-dd");
			ctInboundJournal.setTgBizDate(LocalDate.parse(msg.getUserProperty(TargetBizDateKey.class.getCanonicalName()), format).toDate());
			ctInboundJournal.setTransStatus(TransStatusDef.S);
			//ctInboundJournal.setTransVersion(msg.getUserProperty(TxnVersionKey.class.getCanonicalName()));
			ctInboundJournal.setTxnDatetime(new Date());
			ctInboundJournal.setTxnSerialNo(msg.getKeys());
			ctInboundJournal.setMqMsgId(msg.getTransactionId());
			ctInboundJournal.fillDefaultValues();
			em.persist(ctInboundJournal);
			
			//到这里half-message已经发送，事务成功就应该返回事务消息成功
			log.info("transaction message key: {}===> 本地事务执行成功，交易流水已记录，发送确认消息", msg.getKeys());
			return LocalTransactionState.COMMIT_MESSAGE;
		}
		catch (Exception e) {
			//到这里half-message已经发送，事务成功就应该返回事务消息成功
			log.error("transaction message key: {}===> 本地事务执行失败", msg.getKeys());
			ExceptionUtilsExt.dump(e);
			throw new ErrorMessageException(ErrorCode.SystemError, "transaction message key: {}===> 本地事务执行失败");

		}
	}

	/**
	 * 事务的回查机制；<br>
	 * 由于网络闪断、生产者应用重启等原因，导致某条事务消息的二次确认丢失，MQ Broker端通过扫描发现某条消息长期处于“半消息”时，需要主动向消息生产者询问该消息的最终状态（Commit 或是 Rollback），该过程即消息回查；<br>
	 * Broker会每60s会对Half(Prepare) Message的topic主题为RMQ_SYS_TRANS_HALF_TOPIC的消息进行回查，即调用该回查逻辑；
	 */
	@Override
	@Transactional
	public LocalTransactionState checkLocalTransaction(MessageExt msg) {
		QCtInboundJournal qCtInboundJournal = QCtInboundJournal.ctInboundJournal;
		Tuple tuple = new JPAQueryFactory(em).select(qCtInboundJournal.inboundId, qCtInboundJournal.confirmCount).where(qCtInboundJournal.txnSerialNo.eq(msg.getKeys())).fetchOne();
		//FIXME 这里缺少一个逻辑控制回查次数，达到阈值直接返回LocalTransactionState.ROLLBACK_MESSAGE；需要借助Redis或其他介质记录该值
		if(Optional.fromNullable(tuple).isPresent()){
			CtInboundJournal ctInboundJournal = em.find(CtInboundJournal.class, tuple.get(qCtInboundJournal.inboundId));
			ctInboundJournal.setConfirmCount(ctInboundJournal.getConfirmCount()+1);
			return LocalTransactionState.COMMIT_MESSAGE;
		}
		else {
			//没有找到记录说明可能事务还未提交，或事务已经失败; 
			//为了避免单个消息多次检查,导致一半队列消息积累,检查单个消息默认15次,但可以在Broker端改变“transactionCheckMax”参数；
			//如果一个消息检查超过transactionCheckMax，Broker会丢弃该消息和打印一个错误日志；
			return LocalTransactionState.UNKNOW;
		}
	}

}
