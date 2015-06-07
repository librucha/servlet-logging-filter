package org.librucha.utils.logging.wrapper;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.*;
import java.util.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class LoggingHttpServletResponseWrapper extends HttpServletResponseWrapper {

	private final LoggingServletOutpuStream loggingServletOutpuStream = new LoggingServletOutpuStream();

	private final HttpServletResponse delegate;

	public LoggingHttpServletResponseWrapper(HttpServletResponse response) {
		super(response);
		delegate = response;
	}

	@Override
	public ServletOutputStream getOutputStream() throws IOException {
		return loggingServletOutpuStream;
	}

	@Override
	public PrintWriter getWriter() throws IOException {
		return new PrintWriter(loggingServletOutpuStream.baos);
	}

	public Map<String, String> getHeaders() {
		Map<String, String> headers = new HashMap<>(0);
		for (String headerName : getHeaderNames()) {
			headers.put(headerName, getHeader(headerName));
		}
		return headers;
	}

	public String getContent() {
		try {
			String responseEncoding = delegate.getCharacterEncoding();
			return loggingServletOutpuStream.baos.toString(responseEncoding != null ? responseEncoding : UTF_8.name());
		} catch (UnsupportedEncodingException e) {
			return "[UNSUPPORTED ENCODING]";
		}
	}

	public byte[] getContentAsBytes() {
		return loggingServletOutpuStream.baos.toByteArray();
	}

	private class LoggingServletOutpuStream extends ServletOutputStream {

		private ByteArrayOutputStream baos = new ByteArrayOutputStream();

		@Override
		public boolean isReady() {
			return true;
		}

		@Override
		public void setWriteListener(WriteListener writeListener) {
		}

		@Override
		public void write(int b) throws IOException {
			baos.write(b);
		}

		@Override
		public void write(byte[] b) throws IOException {
			baos.write(b);
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			baos.write(b, off, len);
		}
	}
}
