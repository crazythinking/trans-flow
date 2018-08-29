package net.engining.control.api;

import java.util.Map;

import net.engining.pg.web.BaseResponseBean;

public class ResponseData extends BaseResponseBean {

	private static final long serialVersionUID = 1L;
	/**
	 * 返回结果
	 */
	private Map<String, Object> returnData;

	public Map<String, Object> getReturnData() {
		return returnData;
	}

	public void setReturnData(Map<String, Object> returnData) {
		this.returnData = returnData;
	}
	
}
