package org.rogarithm.presize.service;

import org.rogarithm.presize.service.dto.ImgUpscaleDto;
import org.rogarithm.presize.web.response.ImgUpscaleResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Component
public class ExternalApiRequester {

    private static final Logger log = LoggerFactory.getLogger(ExternalApiRequester.class);

    @Value("${ai.model.url.upscale}")
    private String upscaleUrl;
    @Value("${spring.codec.max-in-memory-size}")
    private String maxInMemorySize;

    @Autowired
    private WebClient webClient;

    @Transactional
    public ImgUpscaleResponse upscaleImg(ImgUpscaleDto dto) {
        WebClient.ResponseSpec retrieve = webClient.post()
                .uri(upscaleUrl)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .bodyValue(dto)
                .retrieve();

        Mono<ImgUpscaleResponse> response = retrieve.bodyToMono(ImgUpscaleResponse.class);

        ImgUpscaleResponse imgUpscaleResponse = null;

        try {
            imgUpscaleResponse = response.block();
        } catch (WebClientResponseException e) {
            log.error("WebClientResponseException: ", e);
            log.error("Full error cause: ", e.getCause());
            String errorMsg = e.getCause().getMessage();
            if (errorMsg.split(":").length >= 2) {
                String problematicSize = errorMsg.split(":")[1].replaceAll(" ", "");
                log.error("Current response's buffer size({}) is higher than current max codec size({}).\n" +
                                "To resolve, edit max codec size higher than {} both in WebClient configuration and application properties!",
                        problematicSize, parseSizeToBytes(maxInMemorySize), problematicSize);
            }
        }

        if (imgUpscaleResponse == null) {
            throw new RuntimeException("Failed to retrieve a upscale response from the AI model");
        }

        if (imgUpscaleResponse.isSuccess()) {
            return imgUpscaleResponse;
        }

        throw new RuntimeException("Upscale error from AI model: " + imgUpscaleResponse.getMessage());
    }

    private String parseSizeToBytes(String size) {
        if (size.endsWith("KB")) {
            return Integer.toString(Integer.parseInt(size.replace("KB", "").trim()) * 1024);
        } else if (size.endsWith("MB")) {
            return Integer.toString(Integer.parseInt(size.replace("MB", "").trim()) * 1024 * 1024);
        }
        return size;
    }
}
