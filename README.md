# servlet-logging-filter
Servlet filter for logging requests and responses

## Usage
```java
@WebFilter(value = "/*", displayName = "loggingFilter")
public class LoggingFilter extends AbstractLoggingFilter {

	private static final Logger log = LoggerFactory.getLogger(LoggingFilter.class);

	public LoggingFilter() {
		super(log);
	}
}
```

## Customization
There are few methods for rewrite if you want:

### Main filter processing
```java
org.librucha.utils.logging.filter.AbstractLoggingFilter.doFilterInternal
```

### Creating description of request. Dafault is create JSON object.
```java
org.librucha.utils.logging.filter.AbstractLoggingFilter.getRequestDescription
```

### Creating description of response. Dafault is create JSON object.
```java
org.librucha.utils.logging.filter.AbstractLoggingFilter.getResponseDescription
```