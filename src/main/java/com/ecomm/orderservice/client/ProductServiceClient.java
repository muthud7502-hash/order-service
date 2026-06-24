package com.ecomm.orderservice.client;

import com.ecomm.orderservice.dto.ProductDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Component
public class ProductServiceClient {

    private final RestTemplate restTemplate;

    @Value("${product.service.url}")
    private String productServiceUrl;

    public ProductServiceClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public ProductDto getProduct(Long productId) {
        try {
            return restTemplate.getForObject(
                productServiceUrl + "/api/products/" + productId, ProductDto.class);
        } catch (HttpClientErrorException.NotFound ex) {
            return null;
        } catch (ResourceAccessException ex) {
            throw new RuntimeException("product-service unreachable: " + ex.getMessage());
        }
    }

    public boolean reserveStock(Long productId, Integer quantity) {
        try {
            String url = productServiceUrl + "/api/products/" + productId
                       + "/reserve?quantity=" + quantity;
            ResponseEntity<ProductDto> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                null,
                ProductDto.class
            );
            return response.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException.Conflict ex) {
            return false;
        } catch (HttpClientErrorException.NotFound ex) {
            return false;
        } catch (ResourceAccessException ex) {
            throw new RuntimeException("product-service unreachable: " + ex.getMessage());
        }
    }
}