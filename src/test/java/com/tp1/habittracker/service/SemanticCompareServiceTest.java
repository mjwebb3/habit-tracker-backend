package com.tp1.habittracker.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tp1.habittracker.config.SemanticCompareProperties;
import com.tp1.habittracker.dto.semantic.SemanticCompareRequest;
import com.tp1.habittracker.dto.semantic.SemanticCompareResponse;
import com.tp1.habittracker.exception.UpstreamBadResponseException;
import com.tp1.habittracker.exception.UpstreamServiceUnavailableException;
import java.io.IOException;
import java.net.URI;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

class SemanticCompareServiceTest {

    private SemanticCompareProperties properties;

    @BeforeEach
    void setUp() {
        properties = new SemanticCompareProperties();
        properties.setWebhookUrl("http://localhost:5678/webhook/compare-texts");
        properties.setReadTimeoutMs(5_000);
        properties.setConnectTimeoutMs(2_000);
    }

    @Test
    void compareTextsReturnsProviderResponseWhenValidPayloadIsReturned() {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body("{\"similar\":true}")
                        .build()
        );

        SemanticCompareService service = new SemanticCompareService(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        SemanticCompareResponse response = service.compareTexts(new SemanticCompareRequest("hola mundo", "hola mundo"));

        assertEquals(true, response.similar());
    }

    @Test
    void compareTextsThrowsServiceUnavailableWhenProviderIsUnreachable() {
        ExchangeFunction exchangeFunction = request -> Mono.error(new WebClientRequestException(
                new IOException("Connection refused"),
                HttpMethod.POST,
                URI.create("http://localhost:5678/webhook/compare-texts"),
                HttpHeaders.EMPTY
        ));

        SemanticCompareService service = new SemanticCompareService(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        assertThrows(
                UpstreamServiceUnavailableException.class,
                () -> service.compareTexts(new SemanticCompareRequest("hola", "mundo"))
        );
    }

    @Test
    void compareTextsThrowsBadResponseWhenSimilarFieldIsMissing() {
        ExchangeFunction exchangeFunction = request -> Mono.just(
                ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body("{}")
                        .build()
        );

        SemanticCompareService service = new SemanticCompareService(
                WebClient.builder().exchangeFunction(exchangeFunction).build(),
                properties
        );

        assertThrows(
                UpstreamBadResponseException.class,
                () -> service.compareTexts(new SemanticCompareRequest("texto1", "texto2"))
        );
    }
}
