package com.zmz.sdd.fullstack.app.domain.repository;

import com.zmz.sdd.fullstack.app.domain.model.AppUser;

import java.time.LocalDateTime;
import java.util.Optional;

/** [SDD-SPEC: 03-技术方案.md §4] */
public interface AppUserRepository {
    Optional<AppUser> findByUsername(String username);
    Optional<AppUser> findById(Long id);
    void incrementFailedCount(Long id);
    void resetFailedCountAndUnlock(Long id);
    void lockUntil(Long id, LocalDateTime until);
}
