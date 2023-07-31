package com.example.demo.auth;

import com.example.demo.model.Member;
import com.example.demo.repository.Prac04Repository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

//import com.mysql.cj.xdevapi.XDevAPIError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PrincipalDetailsService implements UserDetailsService {

    private final Prac04Repository memberdao;

    public PrincipalDetailsService(Prac04Repository memberRepository) {
        this.memberdao = memberRepository;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member userEntity = memberdao.findByUsername(username);
        if (userEntity == null) {
            throw new UsernameNotFoundException("User not found with username: " + username);
        }
        return new PrincipalDetails(userEntity);
    }

}