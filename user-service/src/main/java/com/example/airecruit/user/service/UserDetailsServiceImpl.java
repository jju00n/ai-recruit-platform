package com.example.airecruit.user.service;

import com.example.airecruit.user.domain.User;
import com.example.airecruit.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private static final String ROLE_PREFIX = "ROLE_";

    private final UserRepository userRepository;

    public UserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));
        return toUserDetails(user);
    }

    private UserDetails toUserDetails(User user) {
        String roleWithoutPrefix = user.getRole() != null && user.getRole().startsWith(ROLE_PREFIX)
                ? user.getRole().substring(ROLE_PREFIX.length())
                : user.getRole();
        String password = user.getUserPw() != null ? user.getUserPw() : "";
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserId())
                .password(password)
                .roles(roleWithoutPrefix)
                .build();
    }
}
