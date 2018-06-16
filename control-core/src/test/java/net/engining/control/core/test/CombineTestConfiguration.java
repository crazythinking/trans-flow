package net.engining.control.core.test;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import net.engining.control.core.dispatch.DetailedFlowListener;
import net.engining.control.core.dispatch.MDCFlowListener;
import net.engining.control.core.dispatch.SimpleFlowDispatcher;
import net.engining.control.core.test.flow00.SampleFlow;
import net.engining.pg.support.core.context.ApplicationContextHolder;

/**
 * 
 * @author Eric Lu
 *
 */
@Configuration
public class CombineTestConfiguration {
	
	/**
	 * ApplicationContext的静态辅助Bean，建议项目必须注入
	 * @return
	 */
	@Bean
	@Lazy(value=false)
	public ApplicationContextHolder applicationContextHolder(){
		return new ApplicationContextHolder();
	}
	
	@Bean
	public SimpleFlowDispatcher simpleFlowDispatcher(){
		return new SimpleFlowDispatcher();
		
	}
	
	@Bean
	public SampleFlow sampleFlow(){
		return new SampleFlow();
	}
	
	@Bean
	public MDCFlowListener mdcFlowListener(){
		return new MDCFlowListener();
	}
	
	@Bean
	public DetailedFlowListener detailedFlowListener(){
		DetailedFlowListener detailedFlowListener = new DetailedFlowListener();
		detailedFlowListener.setDumpProcedure(true);
		return detailedFlowListener;
	}
}
