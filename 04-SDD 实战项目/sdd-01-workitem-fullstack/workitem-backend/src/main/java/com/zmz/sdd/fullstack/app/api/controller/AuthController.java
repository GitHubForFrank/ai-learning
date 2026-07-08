package com.zmz.sdd.fullstack.app.api.controller;

import com.zmz.sdd.fullstack.app.api.controller.dto.LoginReq;
import com.zmz.sdd.fullstack.app.api.controller.dto.LoginVO;
import com.zmz.sdd.fullstack.app.api.controller.dto.MeVO;
import com.zmz.sdd.fullstack.app.application.service.AuthService;
import com.zmz.sdd.fullstack.app.domain.model.AppUser;
import com.zmz.sdd.fullstack.core.common.Result;
import com.zmz.sdd.fullstack.core.config.TraceparentFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * [SDD-TASK: Task001]
 * [SDD-SPEC: 02-功能规范.md §3.6 + §3.7]
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** [SDD-SPEC: §3.6 API-06] 公开,JwtFilter 已放行 /api/login */
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginReq req, HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        String tp = (String) request.getAttribute(TraceparentFilter.HEADER);
        AuthService.LoginResult r = authService.login(req.getUsername(), req.getPassword(), ip, ua, tp);
        return Result.success(new LoginVO(r.token(), r.expiresInSeconds(), r.username()));
    }

    /** [SDD-SPEC: §3.7 API-07] 需 JWT(JwtFilter 已校验,attribute 注入了 appUserId) */
    @GetMapping("/me")
    public Result<MeVO> me(HttpServletRequest request) {
        Long appUserId = (Long) request.getAttribute("appUserId");
        AppUser u = authService.getById(appUserId);
        return Result.success(MeVO.from(u));
    }

    /**
     * [SDD-SPEC: conventions/security-conventions.md §13.5]
     * 客户端 IP:优先 X-Forwarded-For 链 last-but-one(防伪);本教学项目接受首段
     */
    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) return realIp.trim();
        return request.getRemoteAddr();
    }
}
