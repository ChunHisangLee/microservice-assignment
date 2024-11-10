package com.jack.userservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${openapi.title:Default API Title}")
    private String title;

    @Value("${openapi.version:v1}")
    private String version;

    @Value("${openapi.description:This API handles user management.}")
    private String description;

    @Value("${openapi.termsOfService:https://example.com/terms}")
    private String termsOfService;

    @Value("${openapi.contact.name:Support}")
    private String contactName;

    @Value("${openapi.contact.email:support@example.com}")
    private String contactEmail;

    @Value("${openapi.license.name:Apache 2.0}")
    private String licenseName;

    @Value("${openapi.license.url:https://www.apache.org/licenses/LICENSE-2.0.html}")
    private String licenseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(version)
                        .description(description)
                        .termsOfService(termsOfService)
                        .contact(new Contact().name(contactName).email(contactEmail))
                        .license(new License().name(licenseName).url(licenseUrl)))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
