package com.zmz.sdd.fullstack.app.infrastructure.repository.translator;

import com.zmz.sdd.fullstack.app.domain.model.AppUser;
import com.zmz.sdd.fullstack.app.domain.model.UserStatus;
import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.AppUserEntity;
import org.springframework.stereotype.Component;

@Component
public class AppUserTranslator {

    public AppUser toModel(AppUserEntity e) {
        if (e == null) return null;
        return AppUser.builder()
                .id(e.getId())
                .username(e.getUsername())
                .passwordHash(e.getPasswordHash())
                .status(e.getStatus() == null ? null : UserStatus.valueOf(e.getStatus()))
                .failedLoginCount(e.getFailedLoginCount() == null ? 0 : e.getFailedLoginCount())
                .lockedUntil(e.getLockedUntil())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
