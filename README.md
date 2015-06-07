# servlet-logging-filter
Servlet filter for logging requests and responses

Usage:
```java
@WebFilter(value = "/*", displayName = "loggingFilter")
public class LoggingFilter extends AbstractLoggingFilter {

	private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

	public LoggingFilter() {
		super(log);
	}
}
```