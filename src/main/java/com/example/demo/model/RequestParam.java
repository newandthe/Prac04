package com.example.demo.model;

import lombok.*;

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
    private Integer page = 1;   // Default Value = 1페이지

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

    private ReDiscover reDiscoverParam;

    private ArrayList<ReDiscover> reDiscoverArr;
    
    private String query; // 현재 검색어

    // Getter 메서드 추가
    public boolean isResearch() {
        return research;
    }

}
