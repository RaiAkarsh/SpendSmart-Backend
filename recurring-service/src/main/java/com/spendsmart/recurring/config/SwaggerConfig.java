package com.spendsmart.recurring.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        final String securitySchemeName = "bearerAuth";
        return new OpenAPI()
            .info(new Info()
                .title("SpendSmart Recurring Service API")
                .description("Manages recurring transaction rules for auto-generating expenses and income.\n\n" +
                             "**Scheduler:** @Scheduled job runs daily at midnight (00:00:00). " +
                             "Finds rules with nextDueDate <= today, calls expense-service or income-service " +
                             "to create the real transaction, then advances nextDueDate by one frequency period.\n\n" +
                             "**Manual trigger for testing:** POST /recurring/process  - runs the scheduler immediately " +
                             "without waiting for midnight.\n\n" +
                             "Frequencies: DAILY / WEEKLY / MONTHLY / QUARTERLY / YEARLY\n\n" +
                             "**How to use:** Login at auth-service (8081)  ->  copy token  ->  " +
                             "click Authorize  ->  paste 'Bearer <token>'")
                .version("1.0.0")
                .contact(new Contact().name("SpendSmart Team")))
            .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
            .components(new Components()
                .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                    .name(securitySchemeName)
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("Enter JWT token from POST /auth/login on port 8081")));
    }
}