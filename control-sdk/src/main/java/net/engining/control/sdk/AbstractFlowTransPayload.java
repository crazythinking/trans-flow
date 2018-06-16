package net.engining.control.sdk;

import java.util.HashMap;
import java.util.Map;

import net.engining.control.api.ContextKey;

/**
 * 用于统一定义FlowTrans的Request和Response抽象类，便于生成插件使用，同时便于{@link FlowTransProcessorTemplate}使用
 * @author Eric Lu
 *
 */
public abstract class AbstractFlowTransPayload {

	/**
	 * Flow定义的Code，默认可以用Flow定义类名的骆驼命名作为Code；由生成插件产生；
	 */
	private String code;

	/**
	 * TODO 签名这么用，还没想清楚，暂时放着
	 */
	private String signature;

	/**
	 * Request输入或Response输出的上下文属性
	 */
	protected Map<Class<? extends ContextKey<?>>, Object> dataMap = new HashMap<Class<? extends ContextKey<?>>, Object>();

	/**
	 * FlowCode通过代码生成插件默认赋值Flow的类名
	 * @param code FlowCode
	 */
	protected AbstractFlowTransPayload(String code) {
		this.code = code;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public Map<Class<? extends ContextKey<?>>, Object> getDataMap() {
		return dataMap;
	}

	public void setDataMap(Map<Class<? extends ContextKey<?>>, Object> dataMap) {
		this.dataMap = dataMap;
	}

}