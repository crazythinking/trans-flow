package net.engining.control.api.key;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;

@KeyDefinition(
		name = "请求端URL"
	)
public interface RequestUrlKey extends ContextKey<String>{

}
