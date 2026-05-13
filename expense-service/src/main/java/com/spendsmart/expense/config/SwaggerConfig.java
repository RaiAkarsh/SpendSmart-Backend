package com.spendsmart.expense.config;

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
                .title("SpendSmart  -  Expense Service API")
                .description("CRUD for expense transactions. Supports filtering by month, category, " +
                             "payment method (CASH/CARD/UPI/BANK_TRANSFER/WALLET), date range, and keyword search.\n\n" +
                             "Also calls budget-service automatically to sync spentAmount when an expense is added or deleted.\n\n" +
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