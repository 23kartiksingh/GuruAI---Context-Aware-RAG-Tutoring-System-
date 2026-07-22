package com.guruai.gateway;

import com.guruai.common.security.InternalAccessProperties;
import com.guruai.gateway.config.GatewaySecurityProperties;
import com.guruai.gateway.config.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, GatewaySecurityProperties.class, InternalAccessProperties.class})
public class ApiGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
