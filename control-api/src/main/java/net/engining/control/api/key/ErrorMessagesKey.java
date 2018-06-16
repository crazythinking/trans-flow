package net.engining.control.api.key;

import java.util.Map;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
import net.engining.pg.support.core.exception.ErrorCode;

@KeyDefinition(
	name = "出错信息"
)
public interface ErrorMessagesKey extends ContextKey<Map<ErrorCode, String>> {

}
