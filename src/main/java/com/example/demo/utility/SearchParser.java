package com.example.demo.utility;

import com.example.demo.model.RequestParam;
import com.example.demo.model.searchParsedEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SearchParser {


    public static searchParsedEntity searchParsing(RequestParam requestParam) {
        String search = requestParam.getSearch();
        search = search.trim();

        // 입력된 검색어를 공백 또는 + 또는 - 또는 " 앞에서 분리
        String[] searchWords = search.split("(?=\\+)|(?=-)|(?=\")");

        List<String> mustList = new ArrayList<>();
        List<String> mustNotList = new ArrayList<>();
        List<String> matchPhraseList = new ArrayList<>();

        boolean isFirstWord = true;

        for (String word : searchWords) {
            // 양 끝에 쌍따옴표가 있는 경우 제거하여 값을 입력
            word = word.replaceAll("^\"|\"$", "").trim();

            if (word.isEmpty()) {
                continue; // 빈 문자열은 skip
            }

            if (isFirstWord) {      // 첫문장은 무조건 must조건에 추가
                mustList.add(word);
                isFirstWord = false;
            } else if (word.startsWith("+")) {
                mustList.add(word.substring(1).trim()); // "+" 기호 제거하고 must 배열에 추가
            } else if (word.startsWith("-")) {
                mustNotList.add(word.substring(1).trim()); // "-" 기호 제거하고 must_not 배열에 추가
            } else {
                matchPhraseList.add(word.trim()); // 그 외에는 match_phrase 배열에 추가
            }
        }

        searchParsedEntity parsedEntity = new searchParsedEntity();
        parsedEntity.setMust(mustList.toArray(new String[0]));
        parsedEntity.setMust_not(mustNotList.toArray(new String[0]));
        parsedEntity.setMatch_pharse(matchPhraseList.toArray(new String[0]));

        System.out.println("searchParsedEntity 는 => " + parsedEntity);
//        System.out.println("따옴표 는 => " + Arrays.deepToString(parsedEntity.getMatch_pharse()));
//        System.out.println(parsedEntity.getMatch_pharse().length);

        return parsedEntity;
    }




}
