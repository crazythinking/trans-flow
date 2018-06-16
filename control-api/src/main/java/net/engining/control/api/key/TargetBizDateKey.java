package net.engining.control.api.key;

import java.util.Date;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
@KeyDefinition(
		name = "对手方系统业务日期"
	)
public interface TargetBizDateKey extends ContextKey<Date>{

}
