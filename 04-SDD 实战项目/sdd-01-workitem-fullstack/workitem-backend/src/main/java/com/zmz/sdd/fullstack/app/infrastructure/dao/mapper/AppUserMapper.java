package com.zmz.sdd.fullstack.app.infrastructure.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zmz.sdd.fullstack.app.infrastructure.dao.entity.AppUserEntity;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface AppUserMapper extends BaseMapper<AppUserEntity> {

    /**
     * [SDD-SPEC: conventions/db-conventions.md §9.1] 自定义 SQL 必须**手工**加 deleted=0
     */
    @Select("SELECT * FROM app_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    AppUserEntity findByUsername(@Param("username") String username);

    /**
     * 用于 DefaultAdminInitializer:绕开 MyBatis Plus 逻辑删除字段过滤。
     * 逻辑删除字段（deleted）由 MyBatis Plus 自动追加条件，但此处显式手写 deleted=0
     * 以保证 DefaultAdminInitializer 在表为空时也能正确判断初始状态。
     */
    @Select("SELECT * FROM app_user WHERE username = #{username} AND deleted = 0 LIMIT 1")
    AppUserEntity findByUsernameIncludingPlaceholder(@Param("username") String username);

    @Update("UPDATE app_user SET password_hash = #{hash}, updated_at = NOW() WHERE id = #{id}")
    int updatePasswordHashById(@Param("id") Long id, @Param("hash") String hash);

    @Update("UPDATE app_user SET failed_login_count = failed_login_count + 1, updated_at = NOW() WHERE id = #{id}")
    int incrementFailedCount(@Param("id") Long id);

    @Update("UPDATE app_user SET failed_login_count = 0, locked_until = NULL, updated_at = NOW() WHERE id = #{id}")
    int resetFailedCountAndUnlock(@Param("id") Long id);

    @Update("UPDATE app_user SET locked_until = #{until}, failed_login_count = 0, updated_at = NOW() WHERE id = #{id}")
    int lockUntil(@Param("id") Long id, @Param("until") java.time.LocalDateTime until);
}
