package org.librucha.utils.logging.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.librucha.utils.logging.entity.*;
import org.librucha.utils.logging.wrapper.*;
import org.slf4j.Logger;

import javax.servlet.*;
import javax.servlet.http.*;
import java.io.IOException;

import static java.util.Objects.requireNonNull;

public abstract class AbstractLoggingFilter implements Filter {

	private final Logger log;

	private final int maxContentSize;

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public AbstractLoggingFilter(Logger log, int maxContentSize) {
		requireNonNull(log, "log must not be null");
		this.log = log;
		this.maxContentSize = maxContentSize;
	}

	public AbstractLoggingFilter(Logger log) {
		requireNonNull(log, "log must not be null");
		this.log = log;
		maxContentSize = 512;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
			HttpServletRequest httpServletRequest = (HttpServletRequest) request;
			HttpServletResponse httpServletResponse = (HttpServletResponse) response;

			doFilterInternal(httpServletRequest, httpServletResponse, filterChain);
		}
	}

	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
		if (!log.isDebugEnabled()) {
			filterChain.doFilter(request, response);
			return;
		}

		LoggingHttpServletRequestWrapper requestWrapper = new LoggingHttpServletRequestWrapper(request);
		LoggingHttpServletResponseWrapper responseWrapper = new LoggingHttpServletResponseWrapper(response);

		log.debug("REQUEST: " + getRequestDescription(requestWrapper));
		filterChain.doFilter(requestWrapper, responseWrapper);
		log.debug("RESPONSE: " + getResponseDescription(responseWrapper));
		response.getOutputStream().write(responseWrapper.getContentAsBytes());
	}

	private String getRequestDescription(LoggingHttpServletRequestWrapper requestWrapper) {
		LoggingRequest loggingRequest = new LoggingRequest();
		loggingRequest.setMethod(requestWrapper.getMethod());
		loggingRequest.setPath(requestWrapper.getRequestURI());
		loggingRequest.setParams(requestWrapper.isFormPost() ? null : requestWrapper.getParameters());
		loggingRequest.setHeaders(requestWrapper.getHeaders());
		String content = requestWrapper.getContent();
		if (log.isTraceEnabled()) {
			loggingRequest.setBody(content);
		} else {
			loggingRequest.setBody(content.substring(0, Math.min(content.length(), maxContentSize)));
		}

		try {
			return OBJECT_MAPPER.writeValueAsString(loggingRequest);
		} catch (JsonProcessingException e) {
			log.warn("Cannot serialize Request to JSON", e);
			return null;
		}
	}

	private String getResponseDescription(LoggingHttpServletResponseWrapper responseWrapper) {
		LoggingResponse loggingResponse = new LoggingResponse();
		loggingResponse.setStatus(responseWrapper.getStatus());
		loggingResponse.setHeaders(responseWrapper.getHeaders());
		String content = responseWrapper.getContent();
		if (log.isTraceEnabled()) {
			loggingResponse.setBody(content);
		} else {
			loggingResponse.setBody(content.substring(0, Math.min(content.length(), maxContentSize)));
		}

		try {
			return OBJECT_MAPPER.writeValueAsString(loggingResponse);
		} catch (JsonProcessingException e) {
			log.warn("Cannot serialize Response to JSON", e);
			return null;
		}
	}

	public int getMaxContentSize() {
		return maxContentSize;
	}
}