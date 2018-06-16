package net.engining.control.core.invoker;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import net.engining.control.api.ContextKey;

@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface InvokerDefinition {

	/**
	 * Invoker的名称
	 * @return
	 */
	String name () default "";
	
	/**
	 * 必须输入的上下文属性
	 * @return
	 */
	Class<? extends ContextKey<?>>[] requires() default {};
	
	/**
	 * 可选输入的上下文属性
	 * @return
	 */
	Class<? extends ContextKey<?>>[] optional() default {};
	
	/**
	 * 输出的上下文属性
	 * @return
	 */
	Class<? extends ContextKey<?>>[] results() default {};
	
}
