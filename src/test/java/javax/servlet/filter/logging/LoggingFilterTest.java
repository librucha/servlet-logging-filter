package javax.servlet.filter.logging;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.context.support.HttpRequestHandlerServlet;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LoggingFilterTest {

	@InjectMocks
	private LoggingFilter loggingFilter = new LoggingFilter();

	@Mock
	private Logger logger;

	private MockHttpServletRequest httpServletRequest;

	private MockHttpServletResponse httpServletResponse;

	private MockFilterChain filterChain;

	@Before
	public void setUp() throws Exception {

		httpServletRequest = new MockHttpServletRequest("GET", "http://localhost:8080/test");
		httpServletRequest.addHeader("Accept", "application/json");
		httpServletRequest.addParameter("param1", "1000");
		httpServletRequest.setContent("Test request body".getBytes());
		httpServletRequest.setContentType(MediaType.TEXT_PLAIN_VALUE);

		httpServletResponse = new MockHttpServletResponse();
		httpServletResponse.setContentType(MediaType.TEXT_PLAIN_VALUE);

		filterChain = new MockFilterChain(new HttpRequestHandlerServlet(), new TestFilter());
	}

	@Test
	public void testDoFilter_Full() throws Exception {

		when(logger.isDebugEnabled()).thenReturn(true);
		when(logger.isTraceEnabled()).thenReturn(true);

		loggingFilter.doFilter(httpServletRequest, httpServletResponse, filterChain);

		verify(logger).isDebugEnabled();
		verify(logger).debug("REQUEST: {\"sender\":\"127.0.0.1\",\"method\":\"GET\",\"path\":\"http://localhost:8080/test\",\"params\":{\"param1\":\"1000\"},\"headers\":{\"Accept\":\"application/json\",\"Content-Type\":\"text/plain\"},\"body\":\"Test request body\"}");
		verify(logger).debug("RESPONSE: {\"status\":200,\"headers\":{\"Content-Type\":\"text/plain\"},\"body\":\"Test response body\"}");
	}

	private class TestFilter implements Filter {

		@Override
		public void init(FilterConfig filterConfig) throws ServletException {
		}

		@Override
		public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
			response.getOutputStream().write("Test response body".getBytes());
		}

		@Override
		public void destroy() {
		}
	}
}