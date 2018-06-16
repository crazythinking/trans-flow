package net.engining.control.api.key;

import java.util.Date;

import net.engining.control.api.ContextKey;
import net.engining.control.api.KeyDefinition;
@KeyDefinition(
		name = "交易日期时间"
	)
public interface TxnDateTimeKey extends ContextKey<Date>{

}
