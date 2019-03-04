package javax.servlet.filter.logging;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

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

	private Set<String> excludedPaths;

	private String requestPrefix;

	private String responsePrefix;

	private Marker requestMarker;

	private Marker responseMarker;

	private boolean disableMarker;

	private boolean disablePrefix;

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
		this.requestMarker = builder.requestMarker;
		this.responseMarker = builder.responseMarker;
		this.disableMarker = builder.disableMarker;
		this.disablePrefix = builder.disablePrefix;
	}

	@Override
	public void init(FilterConfig filterConfig) {

		String loggerName = filterConfig.getInitParameter("loggerName");
		if (isNotBlank(loggerName)) {
			this.log = getLogger(getClass());
		}

		String maxContentSizeParam = filterConfig.getInitParameter("maxContentSize");
		if (maxContentSizeParam != null) {
			this.maxContentSize = Integer.parseInt(maxContentSizeParam);
		}

		String excludedPathsParam = filterConfig.getInitParameter("excludedPaths");
		if (isNotBlank(excludedPathsParam)) {
			String[] paths = excludedPathsParam.split("\\s*,\\s*");
			this.excludedPaths = new HashSet<>(asList(paths));
		}

		String requestPrefixParam = filterConfig.getInitParameter("requestPrefix");
		if (isNotBlank(requestPrefixParam)) {
			this.requestPrefix = requestPrefixParam;
		}

		String responsePrefixParam = filterConfig.getInitParameter("responsePrefix");
		if (isNotBlank(responsePrefixParam)) {
			this.responsePrefix = responsePrefixParam;
		}

		String requestMarkerParam = filterConfig.getInitParameter("requestMarker");
		if (isNotBlank(requestMarkerParam)) {
			this.requestMarker = MarkerFactory.getMarker(requestMarkerParam);
		}

		String responseMarkerParam = filterConfig.getInitParameter("responseMarker");
		if (isNotBlank(responseMarkerParam)) {
			this.responseMarker = MarkerFactory.getMarker(responseMarkerParam);
		}

		String disablePrefixParam = filterConfig.getInitParameter("disablePrefix");
		if (isNotBlank(disablePrefixParam)) {
			this.disablePrefix = Boolean.valueOf(disablePrefixParam);
		}

		String disableMarkerParam = filterConfig.getInitParameter("disableMarker");
		if (isNotBlank(disableMarkerParam)) {
			this.disableMarker = Boolean.valueOf(disableMarkerParam);
		}
	}

	@Override
	@SuppressWarnings({"squid:S3457", "squid:S2629"})
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

		String resolvedRequestPrefix = disablePrefix ? "" : requestPrefix;
		String resolvedResponsePrefix = disablePrefix ? "" : responsePrefix;

		if (disableMarker) {
			log.debug(resolvedRequestPrefix + getRequestDescription(requestWrapper));
		} else {
			log.debug(requestMarker, resolvedRequestPrefix + getRequestDescription(requestWrapper));
		}

		filterChain.doFilter(requestWrapper, responseWrapper);

		if (disableMarker) {
			log.debug(resolvedResponsePrefix + getResponseDescription(responseWrapper));
		} else {
			log.debug(responseMarker, resolvedResponsePrefix + getResponseDescription(responseWrapper));
		}

		httpResponse.getOutputStream().write(responseWrapper.getContentAsBytes());
	}

	@Override
	public void destroy() {
		// nothing special
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

		private Marker requestMarker = MarkerFactory.getMarker("REQUEST");
		private String requestPrefix = requestMarker.getName() + ": ";

		private Marker responseMarker = MarkerFactory.getMarker("RESPONSE");
		private String responsePrefix = responseMarker.getName() + ": ";

		private boolean disableMarker;
		private boolean disablePrefix;

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

		public Builder requestMarker(String marker) {
			requireNonNull(marker, "marker must not be null");
			this.requestMarker = MarkerFactory.getMarker(marker);
			return this;
		}

		public Builder requestPrefix(String requestPrefix) {
			requireNonNull(requestPrefix, "requestPrefix must not be null");
			this.requestPrefix = requestPrefix;
			return this;
		}

		public Builder responsePrefix(String responsePrefix) {
			requireNonNull(responsePrefix, "responsePrefix must not be null");
			this.responsePrefix = responsePrefix;
			return this;
		}

		public Builder responseMarker(String marker) {
			requireNonNull(marker, "marker must not be null");
			this.responseMarker = MarkerFactory.getMarker(marker);
			return this;
		}

		public Builder disableMarker(boolean disable) {
			this.disableMarker = disable;
			return this;
		}

		public Builder disablePrefix(boolean disable) {
			this.disablePrefix = disable;
			return this;
		}

		public LoggingFilter build() {
			return new LoggingFilter(this);
		}
	}
}