package com.example.demo.controller;

import com.example.demo.model.*;
import com.example.demo.model.RequestParam;
import com.example.demo.service.ElasticsearchService;
import com.example.demo.service.Prac04Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.io.IOException;
import java.util.List;

@Controller
public class Prac04Controller {



    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Autowired
    public Prac04Controller(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Autowired
    private Prac04Service service;

    @Autowired
    private ElasticsearchService esservice;



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
    public String searchlist(@ModelAttribute RequestParam requestParam, @ModelAttribute ReDiscover reDiscover, Model model) throws IOException {
//        System.out.println(reDiscover);

        // 로그인 인증세션
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() == "anonymousUser") {
            return "login";
        }

//        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
//        UserDetails userDetails = (UserDetails)principal;
//        String username = ((UserDetails) principal).getUsername();
//        System.out.println(userDetails);
//        System.out.println(username);





        // 인기 검색어 10개
        List<TopKeyword> topKeywordList = esservice.top_search_log();

        // 재검색
        requestParam = esservice.researchClear(requestParam, reDiscover);
//        System.out.println("아래가 Test");
//        System.out.println(requestParam);
//        System.out.println(reDiscover);

        // 재검색을 고려한 검색 시작.
        ArticleEnt art = esservice.sampleQuery(requestParam);

        if(art.getHits() != null) {     // 추후 service로 빼기 할일 태산 ..
            int totalPages = (int) Math.ceil(Double.parseDouble(art.getHits().getTotal().getValue()) / 5);
            model.addAttribute("totalPages", totalPages);
        }


//        System.out.println("art!!!!!!!!!!!!!!");
//        System.out.println(art);
        model.addAttribute("data", art);                        // 검색결과 전달
        model.addAttribute("searchparameter", requestParam);    // 페이징 검색어 등등 ... 전달
        model.addAttribute("topKeywordList", topKeywordList);   // 인기 검색어 최대 10개 전달..

        System.out.println(requestParam);
//        System.out.println(requestParam);

        return "searchlist";
    }

    @ResponseBody
    @GetMapping("/getAutoComplete")
    public List<String> getAutoComplete(String searchdata) throws IOException {

        List<String> result = esservice.getAutoComplete(searchdata);
        System.out.println("result: " + result);

        return result;
    }

    @GetMapping("/searchlist/{nttId}")
    public String searchlistDetail(@PathVariable("nttId") String nttId){

        // 로그인 인증세션
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() == "anonymousUser" || authentication.getPrincipal() == null) {
            System.out.println("비인가 회원");
            return "redirect:/login/sessionout";
        }



        return "";
    }






}
