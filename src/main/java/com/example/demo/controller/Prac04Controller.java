package com.example.demo.controller;

import com.example.demo.auth.PrincipalDetails;
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

        // System.out.println(trylogin_mem.toString());
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

//            System.out.println("test!!!!!!");
//            PrincipalDetails test = (PrincipalDetails) authentication.getPrincipal();
//            System.out.println(test.getMember());
//            System.out.println(test.getUsername());


            return "success";
        } else {
            // 인증 실패
            return "fail";
        }
    }

    @GetMapping("/regi")
    public String regi() {return "regi";}

    @PostMapping("/regiAf")
    public String regiAf(@org.springframework.web.bind.annotation.RequestParam("username") String username, @org.springframework.web.bind.annotation.RequestParam("password") String password) {
//        System.out.println(username);
//        System.out.println(password);

        if(username.trim() == "" || username == null) {
            System.out.println("username isNull!!");
            return "login";
        } else if(password.trim() == "" | password == null) {
            System.out.println("password isNull!!");
            return "login";
        }

        Member member = new Member();

        member.setUsername(username);
        String encodedPassword = bCryptPasswordEncoder.encode(password);
        member.setPassword(encodedPassword);

        boolean isSuccess = service.regiAf(member);

        System.out.println(isSuccess);



        return "login";
    }

    @PostMapping("/checkDuplicateUsername")
    @ResponseBody
    public String checkDuplicateUsername(@org.springframework.web.bind.annotation.RequestParam("username") String username) {
//	    System.out.println(username);

        if(username.trim() == "" || username == null) {
            return "blankusername";
        }

        boolean isDuplicate = service.checkDuplicateUsername(username);
        if (isDuplicate) {
            return "duplicate";
        } else {
            return "notduplicate";
        }
    }

    @GetMapping("/searchlist")
    public String searchlist(@ModelAttribute RequestParam requestParam, @ModelAttribute ReDiscover reDiscover, Model model) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication.getPrincipal() == "anonymousUser") {
            return "login";
        }

        // 금칙어 검사 및 처리
        if (handleBannedKeywords(requestParam, model)) {
            return "searchlist";
        }

        // 불용어 검사 및 처리 // 여기부터는 금칙어 검사를 통과했기 때문에 검색에대한 로직 적용시작


        // 재검색 파라미터 처리
        requestParam = esservice.researchClear(requestParam, reDiscover);

        // 검색 결과 조회
        ArticleEnt art = esservice.sampleQuery(requestParam);

        // 페이징 처리
        if (art.getHits() != null) {
            int pagesize = 20;
            int totalPages = esservice.getTotalPages(art.getHits().getTotal().getValue(), pagesize);
            model.addAttribute("totalPages", totalPages);
        }

        // 결과 및 검색 파라미터 전달
        model.addAttribute("data", art);
        model.addAttribute("searchparameter", requestParam);

        // 인기 검색어 조회
        List<TopKeyword> topKeywordList = esservice.top_search_log();
        model.addAttribute("topKeywordList", topKeywordList);

        // 오타 교정
        String typoSuggest = esservice.typoCorrect(requestParam.getSearch());
        model.addAttribute("typoSuggest", typoSuggest);

        // 수동 추천 리스트
        handleManualRecommendation(requestParam, model);

        // 자동 추천 검색어
        String autoRecomm = esservice.autoRecomm(requestParam.getSearch());
        model.addAttribute("autorecomm", autoRecomm);

        return "searchlist";
    }

    // 금칙어 검사 및 처리 로직
    private boolean handleBannedKeywords(RequestParam requestParam, Model model) throws IOException {
        BanString banned = new BanString();

        if (!requestParam.getSearch().isEmpty()) {
            banned = esservice.isBannedSearch_Forbidden(requestParam.getSearch());
        }

        if (banned.isBanned_forbidden()) {
            model.addAttribute("isBanned", true);
            model.addAttribute("bankeyword_forbidden", banned.getBannedKeyWord_forbidden());
            model.addAttribute("searchparameter", requestParam);
            model.addAttribute("topKeywordList", esservice.top_search_log());
            model.addAttribute("data", new ArticleEnt());
            return true;
        }

        return false;
    }

    // 수동 추천 리스트 처리 로직
    private void handleManualRecommendation(RequestParam requestParam, Model model) throws IOException {
        if (!requestParam.getSearch().isEmpty()) {
            RecommManual recomm = esservice.recommManualGet(requestParam.getSearch());
            model.addAttribute("recommmanual", recomm);
        }
    }

    
    // 자동완성 콘트롤러
    @ResponseBody
    @GetMapping("/getAutoComplete")
    public List<String> getAutoComplete(String searchdata) throws IOException {

        List<String> result = esservice.getAutoComplete(searchdata);
//        System.out.println("result: " + result);

        return result;
    }







}
