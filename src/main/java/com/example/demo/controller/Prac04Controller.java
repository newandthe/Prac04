package com.example.demo.controller;

import com.example.demo.model.Member;
import com.example.demo.service.Prac04Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

@Controller
public class Prac04Controller {



    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public Prac04Controller(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Autowired
    private Prac04Service service;

    @GetMapping("/")
    public String index() {
        return "login";
    }

    @PostMapping("/loginAf")
    @ResponseBody
    public String loginFormSubmit(@ModelAttribute Member member){

        if(member.getUsername().length() > 20){
            throw new DataIntegrityViolationException("입력한 아이디의 길이가 너무 깁니다.");
        }

        Member trylogin_mem = service.getUser(member.getUsername());

//      System.out.println(trylogin_mem.toString());
        if(trylogin_mem == null) {  // 입력한 아이디가 존재하지 않을경우 실패처리.
            return "fail";
        }

        // 입력한 비밀번호를 복호화했을때 동일하다면 로그인이 성공.
        if (trylogin_mem != null && bCryptPasswordEncoder.matches(member.getPassword(), trylogin_mem.getPassword())) {
            // 비밀번호가 일치하는 경우

            // 인증 객체 생성
            Authentication authentication = new UsernamePasswordAuthenticationToken(member, null, null);
            // 현재 스레드의 SecurityContext에 인증 객체 설정
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return "success";
        } else {
            // 인증 실패
            return "fail";
        }
    }

    @GetMapping("/searchlist")
    public String searchlist() {
        // 향후 page, choice, search 고려해서 만들자 ! @RequestParam 혹은 model 만들기.




        return "searchlist";
    }




}
