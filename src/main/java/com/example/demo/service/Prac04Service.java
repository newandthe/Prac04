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

    public boolean checkDuplicateUsername(String username) {
        int n = repository.checkDuplicateUsername(username);
        return n>0?true:false;
    }

    public boolean regiAf(Member member) {

        int n = repository.regiAf(member);

        return n>0?true:false;

    }


}
