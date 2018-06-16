package net.engining.control.api;

/**
 * <p>
 * 交易流程中如有不需要中断整个流程的业务异常抛出，则用此异常或此异常的子类进行包装后抛出。
 * </p>
 * 
 * <p>
 * 交易框架目前的异常处理流程如下，事务组中抛出的继承自{@link BussinessException}的异常会被框架捕获，中断掉当前的数据库事务并回滚，然后继续执行后续的数据库事务，
 * 并将其放在context中的lastExcetion中，此属性会被下一个抛出的此类异常替代。<br/>
 * 如需断掉整个交易流程，则应抛出由客户化包中，继承自{@link RuntimeException}的其他异常，如{@link IllegalArgumentException}或其他自定义异常。<br/>
 * 交易处理框架会将此类异常直接抛出到调用方（客户端），由调用方决定如何处理此类异常。不推荐在产品包中抛出此类不受控的异常。
 * </p>
 * 
 * @author zhangkun
 *
 */
@SuppressWarnings("serial")
public class BussinessException extends RuntimeException {

	public BussinessException() {
		super();
	}

	public BussinessException(String msg) {
		super(msg);
	}

	public BussinessException(Exception cause) {
		super(cause);
	}

}
