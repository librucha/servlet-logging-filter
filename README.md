# Servlet logging filter
Java 8 Servlet filter for logging requests and responses

Web page equivalent is [here](http://librucha.github.io/servlet-logging-filter)

## Usage
Filter implements **javax.servlet.Filter** from Servlet API 3.1.0
You can register the filter using **web.xml** descriptor.
```xml
<filter>
	<filter-name>LoggingFilter</filter-name>
	<filter-class>javax.servlet.filter.logging.LoggingFilter</filter-class>
</filter>
<filter-mapping>
	<filter-name>LoggingFilter</filter-name>
	<url-pattern>/*</url-pattern>
</filter-mapping>
```
or **javax.servlet.ServletContext**
```java
public void onStartup(ServletContext servletContext) throws ServletException {
	Dynamic registration = servletContext.addFilter("LoggingFilter", new LoggingFilter());
	registration.addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*");
}
```
### Init params
|Name          |Default   |Description                                              |
|--------------|----------|---------------------------------------------------------|
|loggerName    |class name|Logger name for output                                   |
|maxContentSize|1024 bytes|Maximal logged body size in bytes                        |
|excludedPaths |empty     |Comma sepparated list of URL prefixes e.g.: "/api,/admin"|
|requestPrefix |REQUEST:  |First word on request output line                        |
|responsePrefix|RESPONSE: |First word on response output line                       |

## Customization
There are few methods for rewrite if you want:

### Main filter processing
```java
javax.servlet.filter.logging.LoggingFilter.doFilter
```

### Creating description of request. Default is create JSON object.
```java
javax.servlet.filter.logging.LoggingFilter.getRequestDescription
```

### Creating description of response. Default is create JSON object.
```java
javax.servlet.filter.logging.LoggingFilter.getResponseDescription
```

## Output
```
REQUEST:{"sender": "127.0.0.1", "method": "GET", "path": "http://localhost:8080/test", "params": {"param1": "1000"}, "headers": {"Accept": "application/json", "Content-Type":"text/plain"}, "body": "Test request body"}
RESPONSE: {"status":200,"headers":{"Content-Type":"text/plain"},"body":"Test response body"}
```
