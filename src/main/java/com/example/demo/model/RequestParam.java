package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RequestParam {

    @Builder.Default
    @Min(1)
    private Integer page = 1;   // Default Value = 1페이지

    @Builder.Default
    private String choice = "recentorderby"; // Default Value = 최신순

    @Builder.Default
    private String search = ""; // Default Value = ""

    @Builder.Default
    private String target = "getall"; // Default Value = 전체검색

    @Builder.Default
    private boolean research = false;  // 결과 내 재검색 ( 기본 False )

    private List<ReDiscoverArr> reDiscoverArr;

}
