package com.bloomberglp.blpapi.examples.model;

public class CoreDataResponse<T> extends DataSource {

	private String msg;

	private int status = -1;

	private T data;

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public T getData() {
		return data;
	}

	public void setData(T data) {
		this.data = data;
	}

	@Override
	public int getStatus() {
		return status;
	}

//	@Override
//	public boolean isSuccess() {
//		return status == 0;
//	}

	@Override
	public String getMsg() {
		return msg;
	}

	public void setStatus(int status) {
		this.status = status;
	}

}
