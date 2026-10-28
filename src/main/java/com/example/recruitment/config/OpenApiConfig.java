package com.example.recruitment.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI talentBridgeOpenAPI() {
        final String securitySchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title("TalentBridge ATS REST API")
                        .description("Production-grade Recruitment and Applicant Tracking System REST API. " +
                                "Provides programmatic endpoints for job lifecycle, applicant pipelines, interview tracking, and analytics.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("TalentBridge Platform Engineering")
                                .email("engineering@talentbridge.internal"))
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("basic")
                                        .description("HTTP Basic authentication with username and password")));
    }
}
