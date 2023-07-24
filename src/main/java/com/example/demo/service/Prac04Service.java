package com.example.demo.service;

import com.example.demo.model.Member;
import com.example.demo.repository.Prac04Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class Prac04Service {

    private final Prac04Repository repository;

    @Autowired
    public Prac04Service(Prac04Repository repository) {this.repository = repository;}



    public Member getUser(String username) {
        // 사용자가 로그인하려고 하는 아이디가 존재하는지 ?
        Member member = repository.getUser(username);
        if(member.getUsername() == null){
            return null;
        }
        else {
            return member;
        }
    }
}
