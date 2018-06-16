package net.engining.control.api.key;

import net.engining.control.api.ContextKey;
import net.engining.control.api.FinalResult;
import net.engining.control.api.KeyDefinition;

@KeyDefinition(
	name = "处理结果"
)
public interface FinalResultKey extends ContextKey<FinalResult> {

}
