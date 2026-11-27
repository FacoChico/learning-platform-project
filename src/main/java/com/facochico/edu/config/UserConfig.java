package com.facochico.edu.config;

import com.facochico.edu.user.UserRepository;
import com.facochico.edu.user.UserService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class UserConfig {

    @Bean
    UserService userService(UserRepository users) {
        return new UserService(users);
    }
}
