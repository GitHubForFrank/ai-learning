package com.gitgui.infrastructure.persistence.mybatis;

import com.gitgui.core.util.TimeUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * SQLite LocalDateTime 类型处理器。
 * <p>SQLite 无原生日期类型，时间以 TEXT 格式 "yyyy-MM-dd HH:mm:ss" 存储。
 * 该处理器负责 Java {@link LocalDateTime} 与 SQLite TEXT 之间的双向转换。</p>
 *
 * @author FrankKang
 * @since 2026-07-23
 */
public class SqliteLocalDateTimeTypeHandler extends BaseTypeHandler<LocalDateTime> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDateTime parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, TimeUtil.format(parameter));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return TimeUtil.parse(rs.getString(columnName));
    }

    @Override
    public LocalDateTime getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return TimeUtil.parse(rs.getString(columnIndex));
    }

    @Override
    public LocalDateTime getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return TimeUtil.parse(cs.getString(columnIndex));
    }
}
