package com.zmz.sdd.fullstack.app.infrastructure.dao.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** [SDD-SPEC: 03-技术方案.md §5.1bis V3] login_log 表实体(只 INSERT,无逻辑删除) */
@Data
@TableName("login_log")
public class LoginLogEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long appUserId;
    private String usernameAttempted;
    private String loginType;
    private String loginIp;
    private String userAgent;
    private String traceparent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
