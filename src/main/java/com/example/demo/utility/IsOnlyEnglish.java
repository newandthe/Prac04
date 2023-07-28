package com.example.demo.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class IsOnlyEnglish {
    public static boolean isEnglishString(String str) {
        // 영어 알파벳 대문자(A-Z) 또는 소문자(a-z)로만 구성되는지 확인하는 정규표현식
        String regex = "^[a-zA-Z]+$";

        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);

        // 정규표현식에 맞는지 여부 반환
        return matcher.matches();       // a~Z로 구성되어있으면 true 반환 // 아니면 false 반환
    }

}
