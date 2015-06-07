package org.librucha.utils.logging.entity;

import java.io.Serializable;
import java.util.Map;

public class LoggingResponse implements Serializable {

	private static final long serialVersionUID = -6692682176015358216L;

	private int status;

	private Map<String, String> headers;

	private String body;

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Map<String, String> getHeaders() {
		return headers;
	}

	public void setHeaders(Map<String, String> headers) {
		this.headers = headers;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}
}
