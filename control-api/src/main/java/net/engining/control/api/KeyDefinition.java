package net.engining.control.api;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * 上下文属性Key的定义
 * @author Eric Lu
 *
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface KeyDefinition {
	
	/**
	 * 字段名
	 */
	String name();
	
	/**
	 * 字段格式，如"AN6"等
	 */
	String format() default "";
	
	/**
	 * 字段描述
	 */
	String desc() default "";
}
