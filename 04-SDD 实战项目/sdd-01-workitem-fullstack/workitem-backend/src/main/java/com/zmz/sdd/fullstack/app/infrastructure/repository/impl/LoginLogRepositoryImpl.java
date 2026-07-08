package com.zmz.sdd.fullstack.app.infrastructure.repository.impl;

import com.zmz.sdd.fullstack.app.domain.model.LoginLog;
import com.zmz.sdd.fullstack.app.domain.repository.LoginLogRepository;
import com.zmz.sdd.fullstack.app.infrastructure.dao.mapper.LoginLogMapper;
import com.zmz.sdd.fullstack.app.infrastructure.repository.translator.LoginLogTranslator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class LoginLogRepositoryImpl implements LoginLogRepository {

    private final LoginLogMapper mapper;
    private final LoginLogTranslator translator;

    @Override
    public void save(LoginLog log) {
        mapper.insert(translator.toEntity(log));
    }
}
