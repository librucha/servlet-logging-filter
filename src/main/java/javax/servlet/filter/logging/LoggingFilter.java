package javax.servlet.filter.logging;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.filter.logging.entity.LoggingRequest;
import javax.servlet.filter.logging.entity.LoggingResponse;
import javax.servlet.filter.logging.wrapper.LoggingHttpServletRequestWrapper;
import javax.servlet.filter.logging.wrapper.LoggingHttpServletResponseWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.slf4j.LoggerFactory.getLogger;

public class LoggingFilter implements Filter {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private Logger log = getLogger(getClass());

	private int maxContentSize;

	private Set<String> excludedPaths = emptySet();

	private String requestPrefix;

	private String responsePrefix;

	static {
		OBJECT_MAPPER.setSerializationInclusion(Include.NON_EMPTY);
	}

	public LoggingFilter() {
		this(Builder.create());
	}

	public LoggingFilter(Builder builder) {
		requireNonNull(builder, "builder must not be null");

		if (isNotBlank(builder.loggerName)) {
			this.log = getLogger(builder.loggerName);
		}
		this.maxContentSize = builder.maxContentSize;
		this.excludedPaths = builder.excludedPaths;
		this.requestPrefix = builder.requestPrefix;
		this.responsePrefix = builder.responsePrefix;
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

		String loggerName = filterConfig.getInitParameter("loggerName");
		if (isNotBlank(loggerName)) {
			this.log = getLogger(getClass());
		}

		String maxContentSize = filterConfig.getInitParameter("maxContentSize");
		if (maxContentSize != null) {
			this.maxContentSize = Integer.parseInt(maxContentSize);
		}

		String excludedPaths = filterConfig.getInitParameter("excludedPaths");
		if (isNotBlank(excludedPaths)) {
			String[] paths = excludedPaths.split("\\s*,\\s*");
			this.excludedPaths = new HashSet<>(asList(paths));
		}

		String requestPrefix = filterConfig.getInitParameter("requestPrefix");
		if (isNotBlank(requestPrefix)) {
			this.requestPrefix = requestPrefix;
		}

		String responsePrefix = filterConfig.getInitParameter("responsePrefix");
		if (isNotBlank(responsePrefix)) {
			this.responsePrefix = responsePrefix;
		}
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
		if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
			throw new ServletException("LoggingFilter just supports HTTP requests");
		}
		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (!log.isDebugEnabled()) {
			filterChain.doFilter(httpRequest, httpResponse);
			return;
		}
		for (String excludedPath : excludedPaths) {
			String requestURI = httpRequest.getRequestURI();
			if (requestURI.startsWith(excludedPath)) {
				filterChain.doFilter(httpRequest, httpResponse);
				return;
			}
		}

		LoggingHttpServletRequestWrapper requestWrapper = new LoggingHttpServletRequestWrapper(httpRequest);
		LoggingHttpServletResponseWrapper responseWrapper = new LoggingHttpServletResponseWrapper(httpResponse);

		log.debug(requestPrefix + getRequestDescription(requestWrapper));
		filterChain.doFilter(requestWrapper, responseWrapper);
		log.debug(responsePrefix + getResponseDescription(responseWrapper));
		httpResponse.getOutputStream().write(responseWrapper.getContentAsBytes());
	}

	@Override
	public void destroy() {
	}

	protected String getRequestDescription(LoggingHttpServletRequestWrapper requestWrapper) {
		LoggingRequest loggingRequest = new LoggingRequest();
		loggingRequest.setSender(requestWrapper.getLocalAddr());
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

	protected String getResponseDescription(LoggingHttpServletResponseWrapper responseWrapper) {
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

	public static class Builder {

		private String loggerName = LoggingFilter.class.getName();

		private int maxContentSize = 1024;

		private Set<String> excludedPaths = emptySet();

		private String requestPrefix = "REQUEST: ";

		private String responsePrefix = "RESPONSE: ";

		public static Builder create() {
			return new Builder();
		}

		public void loggerName(String loggerName) {
			requireNonNull(loggerName, "loggerName must not be null");
			this.loggerName = loggerName;
		}

		public Builder maxContentSize(int maxContentSize) {
			this.maxContentSize = maxContentSize;
			return this;
		}

		public Builder excludedPaths(String... excludedPaths) {
			requireNonNull(excludedPaths, "excludedPaths must not be null");
			this.excludedPaths = Stream.of(excludedPaths).collect(toSet());
			return this;
		}

		public void requestPrefix(String requestPrefix) {
			requireNonNull(requestPrefix, "requestPrefix must not be null");
			this.requestPrefix = requestPrefix;
		}

		public void responsePrefix(String responsePrefix) {
			requireNonNull(responsePrefix, "responsePrefix must not be null");
			this.responsePrefix = responsePrefix;
		}
	}
}