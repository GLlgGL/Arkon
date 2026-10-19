package com.fms.service;

import com.fms.domain.AppConfig;
import com.fms.repo.AppConfigRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {

    private final AppConfigRepository repository;

    public ConfigService(AppConfigRepository repository) {
        this.repository = repository;
    }

    /** The settings row; falls back to defaults if the row is missing. */
    @Transactional(readOnly = true)
    public AppConfig get() {
        return repository.findById(AppConfig.SINGLETON_ID).orElseGet(AppConfig::new);
    }

    @Transactional
    public AppConfig save(AppConfig config) {
        config.setId(AppConfig.SINGLETON_ID);
        return repository.save(config);
    }
}
