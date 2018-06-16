package net.engining.control.core.test.flow00;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Maps;

import net.engining.control.api.ContextKey;
import net.engining.control.api.FlowDispatcher;
import net.engining.control.core.test.support.AbstractTestCaseTemplate;
import net.engining.control.core.test.support.IntValue1Key;
import net.engining.control.core.test.support.IntValue2Key;

public class NormalFlowTest extends AbstractTestCaseTemplate {
	@Autowired
	private FlowDispatcher dispatcher;

	@Override
	public void initTestData() {
		
		Map<Class<? extends ContextKey<?>>, Object> req = Maps.newHashMap();

		req.put(IntValue1Key.class, 4);
		req.put(IntValue2Key.class, 0);
		
		testIncomeDataContext.put("request", req);

	}

	@Override
	public void assertResult() {
		Map<Class<? extends ContextKey<?>>, Object> resp = (Map<Class<? extends ContextKey<?>>, Object>) testAssertDataContext.get("response");
		assertThat((Integer) resp.get(IntValue2Key.class), equalTo(4 * 4));

	}

	@Override
	public void testProcess() {
		Map<Class<? extends ContextKey<?>>, Object> resp = dispatcher.process("sample", (Map<Class<? extends ContextKey<?>>, Object>) testIncomeDataContext.get("request"));

		testAssertDataContext.put("response", resp);
		
	}

	@Override
	public void end() {
		// TODO Auto-generated method stub
		
	}

}
