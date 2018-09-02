package net.engining.control.api;

import java.io.Serializable;
import java.util.Map;

import net.engining.pg.web.BaseResponseBean;

public class ResponseData extends BaseResponseBean {

	private static final long serialVersionUID = 1L;
	
	/**
	 * 返回结果
	 */
	private Map<String, Serializable> returnData;

	public Map<String, Serializable> getReturnData() {
		return returnData;
	}

	public void setReturnData(Map<String, Serializable> returnData) {
		this.returnData = returnData;
	}
	
}
