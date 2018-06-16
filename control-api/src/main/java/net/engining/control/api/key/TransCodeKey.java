package net.engining.control.api.key;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
@KeyDefinition(
		name = "网关定义的交易处理码"
	)
public interface TransCodeKey extends ContextKey<String>{

}
