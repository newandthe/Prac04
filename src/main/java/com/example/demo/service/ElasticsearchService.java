package com.example.demo.service;

import com.example.demo.config.ElasticsearchConfig;
import com.example.demo.model.ArticleEnt;
import com.example.demo.model.RequestParam;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;

@Service
public class ElasticsearchService {

    RestHighLevelClient client;

    public void init() {
        client = ElasticsearchConfig.elasticsearchClient();
    }



    private static final String INDEX = "jsk_index";

    // 전체 쿼리 처리
    public ArticleEnt sampleQuery(RequestParam requestParam) throws IOException {
        if(requestParam.getSearch().trim().equals("")){
            ArticleEnt art = new ArticleEnt();  // hits = null 인 객체 생성해서 반환
            return art;
        }

        init();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(true);   // hits가 1만건 초과되면 value 1만개 고정.. default "해제"

        // RequestParam에 따른 동적 쿼리
        if (requestParam.getTarget().equals("gettitle")) {  // 제목으로 검색인 경우
//            System.out.println("제목 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("title", requestParam.getSearch()));
        } else if (requestParam.getTarget().equals("getcontent")) { // 내용으로 검색인 경우
//            System.out.println("내용 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("content", requestParam.getSearch()));
        } else if (requestParam.getTarget().equals("getfilename")) { // 첨부 파일 명으로 검색인 경우
//            System.out.println("첨부파일 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("origin_name", requestParam.getSearch()));
        } else if (requestParam.getTarget().equals("getfilecontent")) { // 첨부 파일 명으로 검색인 경우
//            System.out.println("첨부파일 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("_file.content", requestParam.getSearch()));
        }

        searchSourceBuilder.size(5);    // 페이지당 5개씩 출력
        searchSourceBuilder.from((requestParam.getPage()-1) * 5);   // Pagination을 위한 "from"
        SearchRequest searchRequest = new SearchRequest(INDEX)
                .source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

//        System.out.println(searchResponse.toString());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode jsonNode = objectMapper.readTree(String.valueOf(searchResponse));
        ArticleEnt art = objectMapper.treeToValue(jsonNode, ArticleEnt.class);
//        System.out.println(art.getHits().getTotal().getValue());    // 검색된 게시물의 총 개수

//        for (int i = 0; i<5; i++){
//            System.out.println(art.getHits().getHits()[i].get_source().getTitle());
//        }




        return art;
    }


}
