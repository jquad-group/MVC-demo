package de.datev.refsys.aggregation.processing.config.filter;

import de.datev.refsys.aggregation.processing.util.LoggingUtil;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.util.UriTemplate;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
public class LoggingContextFilter implements WebFilter {
    private static final String REQUEST_ID_HEADER = "Request-Id";
    private static final String CORRELATION_ID_HEADER = "x-correlation-id";
    private static final String DATEV_CLIENT_ID_HEADER = "X-Datev-Client-ID";
    public static final UriTemplate BASE_URI_TEMPLATE = new UriTemplate("/aggregation-processing/consultants/{consultant}/clients/{client}/");
    public static final String DEFAULT_BASE_DELTA_VERSION = "0(default)";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        Map<String, String> loggingContext = new HashMap<>();
        String requestId = exchange.getRequest().getHeaders().getFirst(REQUEST_ID_HEADER);
        String customRequestId = requestId == null ? UUID.randomUUID().toString() : requestId;
        loggingContext.put(LoggingUtil.REQUEST_ID_KEY, customRequestId);
        String xCorrelationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        String customCorrelationId = xCorrelationId == null ? customRequestId : xCorrelationId;
        loggingContext.put(LoggingUtil.CORRELATION_ID_KEY, customCorrelationId);

        String datevClientIdHeader = exchange.getRequest().getHeaders().getFirst(DATEV_CLIENT_ID_HEADER);
        if (StringUtils.hasLength(datevClientIdHeader)) {
            loggingContext.put(LoggingUtil.DATEV_CLIENT_ID_KEY, datevClientIdHeader);
        }

        String requestUri = exchange.getRequest().getURI().toString();
        Map<String, String> pathParameters = BASE_URI_TEMPLATE.match(requestUri);
        loggingContext.put(LoggingUtil.CONSULTANT_KEY, pathParameters.get(LoggingUtil.CONSULTANT_KEY));
        loggingContext.put(LoggingUtil.CLIENT_KEY, pathParameters.get(LoggingUtil.CLIENT_KEY));

        List<String> fiscalYearList = exchange.getRequest().getQueryParams().get("fiscal-year");
        if (fiscalYearList != null) {
            Optional<String> fiscalYear = fiscalYearList.stream().findFirst();
            fiscalYear.ifPresent(s -> loggingContext.put(LoggingUtil.FISCAL_YEAR_KEY, s));
        }

        Optional.ofNullable(exchange.getRequest().getQueryParams().get("base-version"))
                .ifPresentOrElse(s -> loggingContext.put(LoggingUtil.BASE_VERSION_KEY, s.get(0)),
                                 () -> loggingContext.put(LoggingUtil.BASE_VERSION_KEY, DEFAULT_BASE_DELTA_VERSION));

        Optional.ofNullable(exchange.getRequest().getQueryParams().get("delta-version"))
                .ifPresentOrElse(s -> loggingContext.put(LoggingUtil.DELTA_VERSION_KEY, s.get(0)),
                                 () -> loggingContext.put(LoggingUtil.DELTA_VERSION_KEY, DEFAULT_BASE_DELTA_VERSION));

        exchange.getResponse().getHeaders()
                .putAll(Map.of(REQUEST_ID_HEADER, List.of(customRequestId), CORRELATION_ID_HEADER, List.of(customCorrelationId)));

        return chain.filter(exchange).contextWrite(LoggingUtil.createInitialContext(loggingContext, customCorrelationId));
    }
}

