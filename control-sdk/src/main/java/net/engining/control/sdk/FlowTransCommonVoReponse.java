package net.engining.control.sdk;

import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import net.engining.pg.web.BaseResponseBean;

/**
 * @author luxue
 *
 */
public class FlowTransCommonVoReponse<T extends AbstractFlowTransPayload> extends BaseResponseBean{
	
	private static final long serialVersionUID = 1L;
	
	public FlowTransCommonVoReponse(T response){
		this.dataMap = response.getDataMap().entrySet()
		        .stream()
		        .collect(Collectors.toMap(k -> StringUtils.substringAfterLast(k.getKey().toString(), "."), Map.Entry::getValue));
		        
	}
	
	Map<String, Object> dataMap;

	/**
	 * @return the dataMap
	 */
	public Map<String, Object> getDataMap() {
		return dataMap;
	}

	/**
	 * @param dataMap the dataMap to set
	 */
	public void setDataMap(Map<String, Object> dataMap) {
		this.dataMap = dataMap;
	}
	
//	public static void main(String[] args) {
//		TestResponse t = new TestResponse("");
//		FlowTransCommonVoReponse<TestResponse> t1 = new FlowTransCommonVoReponse<TestResponse>(t);
//		for(Entry<String, Object> key :t1.getDataMap().entrySet()){
//			System.out.println(key.getKey());
//			System.out.println(key.getValue());
//		}
//		
//	}
//
//	static class TestResponse extends AbstractFlowTransPayload{
//
//		/**
//		 * @param code
//		 */
//		protected TestResponse(String code) {
//			super("TestResponse");
//			this.dataMap.put(AsynIndKey.class, AsynInd.A);
//			this.dataMap.put(ChannelKey.class, "qqqqqq");
//		}
//		
//	}
}
