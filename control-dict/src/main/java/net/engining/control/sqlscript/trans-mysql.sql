SET SESSION FOREIGN_KEY_CHECKS=0;

/* Drop Tables */

DROP TABLE IF EXISTS CT_ERROR_JOURNAL;
DROP TABLE IF EXISTS CT_INBOUND_JOURNAL;
DROP TABLE IF EXISTS CT_INBOUND_JOURNAL_HST;




/* Create Tables */

-- 交易流水处理异常表
CREATE TABLE CT_ERROR_JOURNAL
(
	-- ###uuid2###
	ERROR_ID varchar(64) NOT NULL COMMENT '序号 : ###uuid2###',
	-- ###uuid2###
	INBOUND_ID varchar(64) NOT NULL COMMENT 'ID_NO : ###uuid2###',
	ERROR_CODE varchar(10) NOT NULL COMMENT '错误码',
	ERROR_REASON varchar(500) COMMENT '错误原因',
	EXCEPTION_REC mediumtext COMMENT '异常记录',
	CREATE_TIME timestamp DEFAULT NOW() NOT NULL COMMENT 'CREATE_TIME',
	UPDATE_TIME timestamp DEFAULT NOW() NOT NULL COMMENT 'UPDATE_TIME',
	JPA_VERSION int NOT NULL COMMENT '乐观锁版本号',
	PRIMARY KEY (ERROR_ID)
) COMMENT = '交易流水处理异常表';


-- 请求交易流水表
CREATE TABLE CT_INBOUND_JOURNAL
(
	-- ###uuid2###
	INBOUND_ID varchar(64) NOT NULL COMMENT '序号编号 : ###uuid2###',
	SV_PR_ID varchar(10) NOT NULL COMMENT '服务提供系统标识',
	CHANNEL_ID varchar(10) NOT NULL COMMENT '渠道ID(请求系统标识)',
	TG_BIZ_DATE date COMMENT '对方系统业务日期',
	TXN_SERIAL_NO varchar(64) NOT NULL COMMENT '交易流水号',
	TXN_DATETIME timestamp DEFAULT NOW() NOT NULL COMMENT '交易时间',
	SIGN_TOKEN varchar(500) COMMENT '签名TOKEN',
	-- ///
	-- @net.engining.pg.web.AsynInd
	ASYN_IND varchar(1) NOT NULL COMMENT '异步接口标识 : ///
@net.engining.pg.web.AsynInd',
	-- 由程序内定义flow trans code
	TRANS_CODE varchar(100) COMMENT '交易代码 : 由程序内定义flow trans code',
	-- ///
	-- P|待处理
	-- R|重试中
	-- D|批量处理中
	-- S|处理成功
	-- F|处理失败
	-- 
	TRANS_STATUS varchar(2) COMMENT '交易状态 : ///
P|待处理
R|重试中
D|批量处理中
S|处理成功
F|处理失败
',
	REQUEST_MSG mediumtext COMMENT '请求报文',
	RESPONSE_MSG mediumtext COMMENT '响应报文',
	PROCESS_TIME timestamp DEFAULT NOW() NOT NULL COMMENT '系统处理请求时间',
	MQ_MSG_ID varchar(50) COMMENT 'MQ_MSG_ID',
	RETRY_COUNT int COMMENT '重试次数',
	TRANS_VERSION varchar(10) COMMENT '交易版本号',
	REQUEST_IP varchar(20) COMMENT '交易请求IP',
	REQUEST_URL varchar(100) COMMENT '请求URL',
	CONFIRM_COUNT int COMMENT '分布式事务确认次数',
	CREATE_TIME timestamp DEFAULT NOW() NOT NULL COMMENT 'CREATE_TIME',
	UPDATE_TIME timestamp DEFAULT NOW() NOT NULL COMMENT 'UPDATE_TIME',
	BIZ_DATE date NOT NULL COMMENT '系统业务日期',
	JPA_VERSION int NOT NULL COMMENT '乐观锁版本号',
	PRIMARY KEY (INBOUND_ID),
	UNIQUE (TXN_SERIAL_NO)
) COMMENT = '请求交易流水表';


-- 请求交易流水历史表
CREATE TABLE CT_INBOUND_JOURNAL_HST
(
	INBOUND_ID varchar(64) NOT NULL COMMENT '序号编号',
	SV_PR_ID varchar(10) NOT NULL COMMENT '服务提供系统标识',
	CHANNEL_ID varchar(10) NOT NULL COMMENT '渠道ID(请求系统标识)',
	TG_BIZ_DATE date COMMENT '对方系统业务日期',
	TXN_SERIAL_NO varchar(64) NOT NULL COMMENT '交易流水号',
	TXN_DATETIME timestamp DEFAULT NOW() NOT NULL COMMENT '交易时间',
	SIGN_TOKEN varchar(500) COMMENT '签名TOKEN',
	-- ///
	-- @net.engining.pg.web.AsynInd
	ASYN_IND varchar(1) NOT NULL COMMENT '异步接口标识 : ///
@net.engining.pg.web.AsynInd',
	-- 由程序内定义flow trans code
	TRANS_CODE varchar(100) NOT NULL COMMENT '交易代码 : 由程序内定义flow trans code',
	-- ///
	-- P|待处理
	-- R|重试中
	-- D|批量处理中
	-- S|处理成功
	-- F|处理失败
	-- 
	TRANS_STATUS varchar(2) NOT NULL COMMENT '交易状态 : ///
P|待处理
R|重试中
D|批量处理中
S|处理成功
F|处理失败
',
	REQUEST_MSG mediumtext COMMENT '请求报文',
	RESPONSE_MSG mediumtext COMMENT '响应报文',
	PROCESS_TIME timestamp DEFAULT NOW() NOT NULL COMMENT '系统处理请求时间',
	MQ_MSG_ID varchar(50) COMMENT 'MQ_MSG_ID',
	RETRY_COUNT int COMMENT '重试次数',
	TRANS_VERSION varchar(10) COMMENT '交易版本号',
	REQUEST_IP varchar(20) COMMENT '交易请求IP',
	REQUEST_URL varchar(100) COMMENT '请求URL',
	CONFIRM_COUNT int COMMENT '分布式事务确认次数',
	CREATE_TIME timestamp DEFAULT NOW() NOT NULL COMMENT 'CREATE_TIME',
	UPDATE_TIME timestamp DEFAULT NOW() NOT NULL COMMENT 'UPDATE_TIME',
	BIZ_DATE date NOT NULL COMMENT '系统业务日期',
	JPA_VERSION int NOT NULL COMMENT '乐观锁版本号',
	PRIMARY KEY (INBOUND_ID),
	UNIQUE (TXN_SERIAL_NO)
) COMMENT = '请求交易流水历史表';



