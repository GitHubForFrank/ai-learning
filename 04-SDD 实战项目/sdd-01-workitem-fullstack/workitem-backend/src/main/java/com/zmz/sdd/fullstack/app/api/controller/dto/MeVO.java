package com.zmz.sdd.fullstack.app.api.controller.dto;

import com.zmz.sdd.fullstack.app.domain.model.AppUser;
import com.zmz.sdd.fullstack.app.domain.model.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

/** [SDD-SPEC: 02-功能规范.md §3.7] 不含 passwordHash 等敏感字段 */
@Data
@AllArgsConstructor
public class MeVO {
    private Long id;
    private String username;
    private UserStatus status;

    public static MeVO from(AppUser u) {
        return new MeVO(u.getId(), u.getUsername(), u.getStatus());
    }
}
