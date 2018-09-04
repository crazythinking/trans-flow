package net.engining.control.core.flow;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import net.engining.control.api.ContextKey;
import net.engining.control.core.invoker.Invoker;

/**
 * 用于注解Flow的定义，主要包括Flow定义的编码，组成FlowTrans的Invokers，返回的属性，以及内部使用的参数；
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
public @interface FlowDefinition {
	
	/**
	 * 编码
	 * @return
	 */
	@Deprecated
	String code() default "";
	
	/**
	 * 名称
	 * @return
	 */
	String name() default "";
	
	/**
	 * 描述
	 * @return
	 */
	String desc() default "";
	
	/**
	 * 调用者列表
	 * @return
	 */
	Class<? extends Invoker>[] invokers();

	/**
	 * 需返回的所有属性列表
	 * @return
	 */
	Class<? extends ContextKey<?>>[] response();
	
	/**
	 * FlowTrans中使用的参数，用于存放控制FlowTrans调用Invoker的标志，如跳过某个Invoker的控制参数；</br>
	 * 在{@link FlowContext}中转为Map<String,String>，可以在Flow定义是指定默认参数，也可以在某个Invoker中添加或改变；</br>
	 * 参数定义成对出现，即前一个是key，后一个是value，例如：</br>
	 * ["aaa","A09", "bbb","B01"]，相当于aaa=A09，bbb=B01
	 * @return
	 */
	String [] parameters() default {};
}
