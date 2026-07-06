package com.lg.hq.hqserver.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final JdbcTemplate hqJdbc;

    public UserDetailsServiceImpl(@Qualifier("hqJdbc") JdbcTemplate hqJdbc) {
        this.hqJdbc = hqJdbc;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        try {
            Map<String, Object> user = hqJdbc.queryForMap(
                    "SELECT * FROM portal_user WHERE username = ? AND enabled = 1", username);

            return User.builder()
                    .username((String) user.get("username"))
                    .password((String) user.get("password"))
                    .authorities(Collections.singletonList(
                            new SimpleGrantedAuthority((String) user.get("role"))))
                    .build();

        } catch (Exception e) {
            throw new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + username);
        }
    }
}