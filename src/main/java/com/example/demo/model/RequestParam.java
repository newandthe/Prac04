package com.example.demo.model;

import lombok.*;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestParam {

    @Builder.Default
    @Min(1)
    private int page = 1;   // Default Value = 1페이지

    @Builder.Default
    private String choice = "accuracyorderby"; // Default Value = 정확도 순

    @Builder.Default
    private String search = ""; // Default Value = ""

    @Builder.Default
    private String target = "getall"; // Default Value = 전체검색

    @Builder.Default
    private boolean research = false;  // 결과 내 재검색 ( 기본 False )

    @Builder.Default
    private String category = "전체"; // Default Value = 전체

    @Builder.Default
    private String period_of_view = "0";

    private ReDiscover reDiscoverParam;

    private ArrayList<ReDiscover> reDiscoverArr;

    private String prevSearch; // 이전 검색어
    
    private String query; // 현재 검색어

    // Getter 메서드 추가
    public boolean isResearch() {
        return research;
    }

    public void setPage(int page) {
        if (page < 1) {
            this.page = 1;
        } else {
            this.page = page;
        }
    }

}
