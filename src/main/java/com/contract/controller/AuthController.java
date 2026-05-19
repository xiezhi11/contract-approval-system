package com.contract.controller;

import com.contract.common.Result;
import com.contract.dto.LoginDTO;
import com.contract.entity.SysUser;
import com.contract.repository.SysUserRepository;
import com.contract.security.JwtTokenUtil;
import com.contract.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private SecurityUtil securityUtil;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Validated @RequestBody LoginDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String token = jwtTokenUtil.generateToken(dto.getUsername());

        SysUser user = userRepository.findByUsername(dto.getUsername()).orElse(null);

        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("roles", user.getRoles().stream()
                .map(role -> role.getRoleCode())
                .collect(Collectors.toList()));

        return Result.success(result);
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        SecurityContextHolder.clearContext();
        return Result.success();
    }

    @GetMapping("/userinfo")
    public Result<Map<String, Object>> getUserInfo() {
        SysUser user = securityUtil.getCurrentUser();
        if (user == null) {
            return Result.error("用户未登录");
        }

        Map<String, Object> result = new HashMap<>();
        result.put("id", user.getId());
        result.put("username", user.getUsername());
        result.put("realName", user.getRealName());
        result.put("roles", user.getRoles().stream()
                .map(role -> role.getRoleCode())
                .collect(Collectors.toList()));

        return Result.success(result);
    }
}
