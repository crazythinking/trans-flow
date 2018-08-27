package net.engining.control.core.invoker;

import java.util.Map;

/**
 * 指定invoker需要RocketMQ分布式事务支持
 * 
 * @author luxue
 *
 */
public interface RocketMqTransactional {

	/**
	 * 设置支持分布式事务的消息对象到FlowContext.parameters
	 * @param context 
	 */
	public void setupTransactionalMessage(Map<String, String> parameters);

}
