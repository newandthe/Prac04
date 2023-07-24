package com.example.demo.auth;

import com.example.demo.repository.MemberDao;
import com.example.demo.repository.Prac04Repository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.example.demo.model.Member;
//import com.mysql.cj.xdevapi.XDevAPIError;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalDetailsService implements UserDetailsService {

    private final MemberDao memberDao;

//    public PrincipalDetailsService(MemberDao memberRepository){
//        this.memberDao = memberRepository;
//    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member userEntity = MemberDao.findByUserId(username);
        if (userEntity == null) {
             return null;
        }
        return new PrincipalDetails(userEntity);
    }

}