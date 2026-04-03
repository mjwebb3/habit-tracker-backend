package com.tp1.habittracker.service;

import com.tp1.habittracker.config.SemanticCompareProperties;
import com.tp1.habittracker.dto.semantic.SemanticCompareRequest;
import com.tp1.habittracker.dto.semantic.SemanticCompareResponse;
import com.tp1.habittracker.exception.UpstreamBadResponseException;
import com.tp1.habittracker.exception.UpstreamServiceUnavailableException;
import java.time.Duration;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.Exceptions;

@Service
@Slf4j
public class SemanticCompareService {

    private final WebClient webClient;
    private final SemanticCompareProperties properties;

    public SemanticCompareService(@Qualifier("semanticCompareWebClient") WebClient webClient,
                                  SemanticCompareProperties properties) {
        this.webClient = webClient;
        this.properties = properties;
    }

    public SemanticCompareResponse compareTexts(SemanticCompareRequest request) {
        Objects.requireNonNull(request, "request must not be null");

        log.info(
                "Semantic compare request received: text1Length={}, text2Length={}",
                safeLength(request.text1()),
                safeLength(request.text2())
        );

        long startNanos = System.nanoTime();

        try {
            UpstreamSemanticCompareResponse response = webClient.post()
                    .uri(properties.getWebhookUrl())
                    .bodyValue(request)
                    .retrieve()
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse -> clientResponse.bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .map(body -> new UpstreamServiceUnavailableException(
                                    "Semantic compare provider returned status "
                                            + clientResponse.statusCode().value()
                                            + (body.isBlank() ? "" : (": " + truncate(body, 200)))
                            )))
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse -> clientResponse.bodyToMono(String.class)
                    .defaultIfEmpty("")
                    .map(body -> new UpstreamBadResponseException(
                        "Semantic compare provider returned status "
                            + clientResponse.statusCode().value()
                            + (body.isBlank() ? "" : (": " + truncate(body, 200)))
                    )))
                .bodyToMono(UpstreamSemanticCompareResponse.class)
                    .block(Duration.ofMillis(properties.getReadTimeoutMs()));

            if (response == null || response.similar() == null) {
                throw new UpstreamBadResponseException("Semantic compare provider returned empty response");
            }

            long elapsedMs = Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
            log.info("Semantic compare response received: similar={}, elapsedMs={}", response.similar(), elapsedMs);
            return new SemanticCompareResponse(response.similar());
        } catch (UpstreamBadResponseException | UpstreamServiceUnavailableException ex) {
            throw ex;
        } catch (WebClientRequestException ex) {
            throw new UpstreamServiceUnavailableException("Semantic compare provider is unreachable", ex);
        } catch (WebClientResponseException ex) {
            if (ex.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE || ex.getStatusCode().is5xxServerError()) {
            throw new UpstreamServiceUnavailableException(
                "Semantic compare provider returned status " + ex.getStatusCode().value(),
                ex
            );
            }
            throw new UpstreamBadResponseException(
                "Semantic compare provider returned status " + ex.getStatusCode().value(),
                ex
            );
        } catch (RuntimeException ex) {
            Throwable unwrapped = Exceptions.unwrap(ex);
            if (unwrapped instanceof java.util.concurrent.TimeoutException) {
                throw new UpstreamServiceUnavailableException("Semantic compare provider timed out", ex);
            }
            if (unwrapped instanceof UpstreamServiceUnavailableException upstreamServiceUnavailableException) {
                throw upstreamServiceUnavailableException;
            }
            if (unwrapped instanceof UpstreamBadResponseException upstreamBadResponseException) {
                throw upstreamBadResponseException;
            }
            throw new UpstreamBadResponseException("Semantic compare provider returned invalid response", ex);
        } catch (Exception ex) {
            throw new UpstreamServiceUnavailableException("Semantic compare provider failed unexpectedly", ex);
        }
    }

    private int safeLength(String value) {
        return value == null ? 0 : value.length();
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength) + "...";
    }

    private record UpstreamSemanticCompareResponse(Boolean similar) {
    }
}
