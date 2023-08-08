package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
//        http.authorizeRequests()
//                .antMatchers("/login", "/logout").permitAll()
//                .anyRequest().authenticated()
//                .and()
//                .formLogin()
//                .loginPage("/")
//                .loginProcessingUrl("/login")
//                .defaultSuccessUrl("/searchlist")
//                .permitAll();

        http.authorizeRequests().anyRequest().permitAll().and().formLogin().loginPage("/").loginProcessingUrl("/login").defaultSuccessUrl("/searchlist");
        http.logout().logoutUrl("/logout").logoutSuccessUrl("/");
        // login page 내에 넣기	// loginProcessurl 은 주소값 // 로그인이 성공되었을때 어디로 이동할지..

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder encode() {
        return new BCryptPasswordEncoder();
    }


}