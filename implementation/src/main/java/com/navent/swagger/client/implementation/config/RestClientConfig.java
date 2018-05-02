package com.navent.swagger.client.implementation.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public abstract class RestClientConfig {

	private long futureTimeout = 60;
	private String basePath;
}
