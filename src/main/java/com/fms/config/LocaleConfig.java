package com.fms.config;

import java.time.Duration;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

/**
 * Interface language: English (default) or Albanian. Any page link with
 * ?lang=sq or ?lang=en switches; the choice is kept in a cookie for a year.
 * Texts live in messages.properties (English) and messages_sq.properties.
 */
@Configuration
public class LocaleConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver r = new CookieLocaleResolver("IMS_LANG");
        r.setDefaultLocale(Locale.ENGLISH);
        r.setCookieMaxAge(Duration.ofDays(365));
        return r;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor i = new LocaleChangeInterceptor();
        i.setParamName("lang");
        registry.addInterceptor(i);
    }
}
