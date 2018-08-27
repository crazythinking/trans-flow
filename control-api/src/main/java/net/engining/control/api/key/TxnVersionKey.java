package net.engining.control.api.key;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
@KeyDefinition(
		name = "交易版本号"
	)
public interface TxnVersionKey extends ContextKey<String>{

}
