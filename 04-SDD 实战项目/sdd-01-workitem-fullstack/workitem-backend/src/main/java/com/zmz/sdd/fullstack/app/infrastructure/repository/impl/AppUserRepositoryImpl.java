package com.zmz.sdd.fullstack.app.infrastructure.repository.impl;

import com.zmz.sdd.fullstack.app.domain.model.AppUser;
import com.zmz.sdd.fullstack.app.domain.repository.AppUserRepository;
import com.zmz.sdd.fullstack.app.infrastructure.dao.mapper.AppUserMapper;
import com.zmz.sdd.fullstack.app.infrastructure.repository.translator.AppUserTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class AppUserRepositoryImpl implements AppUserRepository {

    private final AppUserMapper mapper;
    private final AppUserTranslator translator;

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return Optional.ofNullable(mapper.findByUsername(username)).map(translator::toModel);
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return Optional.ofNullable(mapper.selectById(id)).map(translator::toModel);
    }

    @Override
    public void incrementFailedCount(Long id) {
        mapper.incrementFailedCount(id);
    }

    @Override
    public void resetFailedCountAndUnlock(Long id) {
        mapper.resetFailedCountAndUnlock(id);
    }

    @Override
    public void lockUntil(Long id, LocalDateTime until) {
        mapper.lockUntil(id, until);
    }
}
