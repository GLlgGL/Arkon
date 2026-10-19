package com.fms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under the "fms" prefix in application.yml.
 *
 * @param storageDir folder for invoice PDFs and the logo
 */
@ConfigurationProperties(prefix = "fms")
public record FmsProperties(String storageDir) {
    public FmsProperties {
        if (storageDir == null || storageDir.isBlank()) {
            storageDir = "./data";
        }
    }
}
