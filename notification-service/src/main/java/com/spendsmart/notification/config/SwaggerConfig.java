package com.spendsmart.notification.config;

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
                .title("SpendSmart  -  Notification Service API")
                .description("In-app notification system for budget alerts and recurring reminders.\n\n" +
                             "**Notification Types:** BUDGET_ALERT, BUDGET_EXCEEDED, RECURRING_DUE, SYSTEM\n\n" +
                             "**Priority Levels:** HIGH (budget exceeded), MEDIUM (budget warning), LOW (info/system)\n\n" +
                             "**Auto-alerts:** @Scheduled job checks budget status every hour and fires alerts " +
                             "when spentAmount crosses alertThreshold% or exceeds limitAmount.\n\n" +
                             "**Manual trigger:** POST /notifications/check-budgets to trigger the budget check immediately.\n\n" +
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