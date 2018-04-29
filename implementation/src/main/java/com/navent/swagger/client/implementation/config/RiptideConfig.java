package com.navent.swagger.client.implementation.config;

import lombok.Data;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
@Data
@PropertySource("riptide")
public class RiptideConfig {
    private String baseUrl;
}
