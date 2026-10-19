package com.fms.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class PasswordConfig {

    /**
     * Encodes new passwords with BCrypt ("{bcrypt}..."). Still accepts imported
     * "{noop}..." plain-text passwords so existing users can log in once;
     * they are re-hashed on that login (see UserService.updatePassword).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
