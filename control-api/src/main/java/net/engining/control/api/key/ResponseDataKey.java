package net.engining.control.api.key;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
import net.engining.control.api.ResponseData;

@KeyDefinition(
		name = "联机交易结果"
	)
public interface ResponseDataKey extends ContextKey<ResponseData>{

}
