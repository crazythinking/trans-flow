package net.engining.control.api.key;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
import net.engining.pg.web.AsynInd;

@KeyDefinition(
		name = "同异步标识"
	)
public interface AsynIndKey extends ContextKey<AsynInd>{

}
