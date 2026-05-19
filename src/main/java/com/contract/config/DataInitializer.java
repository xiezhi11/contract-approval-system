package com.contract.config;

import com.contract.entity.SysRole;
import com.contract.entity.SysUser;
import com.contract.repository.SysRoleRepository;
import com.contract.repository.SysUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashSet;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private SysRoleRepository roleRepository;

    @Autowired
    private SysUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        initRoles();
        initUsers();
    }

    private void initRoles() {
        createRoleIfNotExists("ADMIN", "系统管理员");
        createRoleIfNotExists("CREATOR", "合同创建人");
        createRoleIfNotExists("APPROVER", "审批人");
        createRoleIfNotExists("ARCHIVIST", "归档管理员");
    }

    private void createRoleIfNotExists(String roleCode, String roleName) {
        if (!roleRepository.existsByRoleCode(roleCode)) {
            SysRole role = new SysRole();
            role.setRoleCode(roleCode);
            role.setRoleName(roleName);
            role.setDescription(roleName);
            role.setEnabled(true);
            roleRepository.save(role);
        }
    }

    private void initUsers() {
        SysRole adminRole = roleRepository.findByRoleCode("ADMIN").get();
        SysRole creatorRole = roleRepository.findByRoleCode("CREATOR").get();
        SysRole approverRole = roleRepository.findByRoleCode("APPROVER").get();
        SysRole archivistRole = roleRepository.findByRoleCode("ARCHIVIST").get();

        createUserIfNotExists("admin", "admin123", "系统管理员", adminRole, creatorRole, approverRole, archivistRole);
        createUserIfNotExists("zhangsan", "123456", "张三", creatorRole);
        createUserIfNotExists("lisi", "123456", "李四", approverRole);
        createUserIfNotExists("wangwu", "123456", "王五", archivistRole);
        createUserIfNotExists("zhaoliu", "123456", "赵六", creatorRole, approverRole);
    }

    private void createUserIfNotExists(String username, String password, String realName, SysRole... roles) {
        if (!userRepository.existsByUsername(username)) {
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setRealName(realName);
            user.setEmail(username + "@example.com");
            user.setEnabled(true);
            user.setRoles(new HashSet<>());
            for (SysRole role : roles) {
                user.getRoles().add(role);
            }
            userRepository.save(user);
        }
    }
}
