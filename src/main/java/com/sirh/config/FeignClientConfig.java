package com.sirh.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

// Config Feign dédiée au client file-storage, passée via
// @FeignClient(configuration = ...). Volontairement PAS annotée
// @Configuration : cette classe vit dans com.sirh.config, couvert par le
// @ComponentScan de SIRHApplication — si elle était @Configuration, Spring
// Cloud OpenFeign l'appliquerait par défaut à TOUS les futurs @FeignClient
// (cf. doc Spring Cloud OpenFeign, section "overriding defaults"), pas
// seulement à FileStorageClient.
public class FeignClientConfig {

  @Bean
  public RequestInterceptor apiKeyRequestInterceptor(
      @Value("${filestorage.api.key}") String apiKey) {
    return requestTemplate -> requestTemplate.header("X-Api-Key", apiKey);
  }
}
