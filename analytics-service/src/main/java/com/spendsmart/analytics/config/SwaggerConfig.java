package com.spendsmart.analytics.config;

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
                .title("SpendSmart Analytics Service API")
                .description("Aggregates data from expense-service (8082), income-service (8083), " +
                             "and budget-service (8085) to produce insights.\n\n" +
                             "**Has no database of its own** all data is fetched live via Feign clients.\n\n" +
                             "**Endpoints:**\n" +
                             "- Monthly Summary: totalIncome, totalExpenses, netSavings, savingsRate\n" +
                             "- Category Breakdown: spending grouped by categoryId (pie chart data)\n" +
                             "- Spending Trends: last N months income vs expense (bar chart data)\n" +
                             "- Financial Health Score: 0-100 composite score with grade A+ to F\n" +
                             "- Income vs Expense Comparison: SURPLUS or DEFICIT status\n" +
                             "- Yearly Summary: all 12 months aggregated\n" +
                             "- All-Time Totals: lifetime income, expenses, net position\n\n" +
                             "**Requires:** expense-service (8082) and income-service (8083) must be running.\n\n" +
                             "**How to use:** Login at auth-service (8081)  copy token " +
                             "click Authorize  paste 'Bearer <token>'")
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
