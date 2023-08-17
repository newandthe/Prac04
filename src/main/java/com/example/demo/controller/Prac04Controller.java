package com.example.demo.controller;

import com.example.demo.auth.PrincipalDetails;
import com.example.demo.model.*;
import com.example.demo.model.RequestParam;
import com.example.demo.service.ElasticsearchService;
import com.example.demo.service.Prac04Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.request.async.DeferredResult;

import javax.validation.Valid;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
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

    private String resourcePath = "172.30.1.105:22:/data/download/"; // 리소스 경로



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
    public String searchlist(@ModelAttribute @Valid RequestParam requestParam, @ModelAttribute ReDiscover reDiscover, Model model) throws IOException {
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
        requestParam = esservice.reSearchClear(requestParam, reDiscover);

        // 검색 결과 조회
        ArticleEnt art = esservice.mainQuery(requestParam);


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
//        List<TopKeyword> topKeywordList = esservice.top_search_log();
//        model.addAttribute("topKeywordList", topKeywordList);

        // 오타 교정
//        String typoSuggest = esservice.typoCorrect(requestParam.getSearch());
//        model.addAttribute("typoSuggest", typoSuggest);

        // 수동 추천 리스트
//        handleManualRecommendation(requestParam, model);

        // 자동 추천 검색어
//        List<String> autoRecomm = esservice.autoRecomm(requestParam.getSearch(), requestParam.getPrevSearch(), requestParam.isResearch());
//        model.addAttribute("autorecomm", autoRecomm);


//        String imageURL = "https://img9.yna.co.kr/etc/inner/KR/2023/06/21/AKR20230621096900052_01_i_P4.jpg";
//        model.addAttribute("imageURL", imageURL);

        return "searchlist";
    }

    // 인기 검색어 조회
    @GetMapping("/getTopKeywordList")
    @ResponseBody
    public List<TopKeyword> getTopKeywordList() throws IOException {

        List<TopKeyword> topKeywordList = esservice.top_search_log();
//        System.out.println(topKeywordList);
        return topKeywordList;
    }

    // 오타 교정 AJAX
    @GetMapping("/getTypoSuggest")
    @ResponseBody
    public String getTypoSuggest(String search) throws IOException {

        return esservice.typoCorrect(search);
    }

    // 자동 추천 AJAX
    @GetMapping("/getAutoRecommendation")
    @ResponseBody
    public List<String> getAutoRecommendation(String search, String prevSearch, Boolean isResearch) throws IOException {
        List<String> autorecomm = esservice.autoRecomm(search, prevSearch, isResearch);

        return autorecomm;
    }


    // 수동 추천 AJAX
    @GetMapping("/getManualRecommendation")
    @ResponseBody
    public RecommManual getManualRecommendation(String search) throws IOException {
        RecommManual recomm = new RecommManual();
        if (!search.isEmpty()) {
             recomm = esservice.recommManualGet(search);
        }

        System.out.println("수동추천!!!");
        return recomm;
    }

    // 금칙어 검사
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
            System.out.println(model.getAttribute("data"));
            return true;
        }

        return false;
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
