package com.example.demo.service;

import com.example.demo.config.ElasticsearchConfig;
import com.example.demo.model.ArticleEnt;
import com.example.demo.model.ReDiscover;
import com.example.demo.model.RequestParam;
import com.example.demo.model.searchParsedEntity;
import com.example.demo.utility.SearchParser;
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
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.stereotype.Service;

import javax.management.Query;
import java.io.IOException;
import java.util.ArrayList;
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

        // 검색어 + - "" ( 포함 제외 정확히 일치 파싱해서 배열 형태로 모두 반환 ) (Utility의 SearchParser 클래스)
        searchParsedEntity searchedpEnt = new searchParsedEntity();
        searchedpEnt = SearchParser.searchParsing(requestParam);

        init();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(true);   // hits가 1만건 초과되면 value 1만개 고정.. default "해제"


        // RequestParam에 따른 동적 쿼리
        if(requestParam.getTarget().equals("getall")){
            BoolQueryBuilder multiMatchQuery = QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery("title", requestParam.getSearch()))
                    .should(QueryBuilders.matchQuery("content", requestParam.getSearch()))
                    .should(QueryBuilders.matchQuery("_file.nameOrg", requestParam.getSearch()))
                    .should(QueryBuilders.matchQuery("_file.filecontent", requestParam.getSearch()));
            searchSourceBuilder.query(multiMatchQuery);
        }
        else if (requestParam.getTarget().equals("gettitle")) {  // 제목으로 검색인 경우
            BoolQueryBuilder multiMatchQuery = QueryBuilders.boolQuery();

            for (int i = 0; i < searchedpEnt.getMust().length; i++) {
                String mustWord = searchedpEnt.getMust()[i];
                multiMatchQuery.should(QueryBuilders.matchQuery("title", mustWord));
            }

            for (int i = 0; i < searchedpEnt.getMust_not().length; i++) {
                String mustNotWord = searchedpEnt.getMust_not()[i];
                multiMatchQuery.mustNot(QueryBuilders.termQuery("title", mustNotWord));
            }

            searchSourceBuilder.query(multiMatchQuery);
        }

        else if (requestParam.getTarget().equals("getcontent")) { // 내용으로 검색인 경우
//            System.out.println("내용 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("content", requestParam.getSearch()));
        } else if (requestParam.getTarget().equals("getfilename")) { // 첨부 파일 명으로 검색인 경우
//            System.out.println("첨부파일 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("_file.nameOrg", requestParam.getSearch()));
        } else if (requestParam.getTarget().equals("getfilecontent")) { // 첨부 파일 명으로 검색인 경우
//            System.out.println("첨부파일 검색");
            searchSourceBuilder.query(QueryBuilders.matchQuery("_file.content", requestParam.getSearch()));
        }

        searchSourceBuilder.size(5);    // 페이지당 5개씩 출력
        searchSourceBuilder.from((requestParam.getPage()-1) * 5);   // Pagination을 위한 "from"

        System.out.println("requestParam : " + requestParam);

        // 재검색이 true에 값이 존재한다면
        if (requestParam.isResearch() && requestParam.getReDiscoverArr() != null) {
            BoolQueryBuilder postFilter = QueryBuilders.boolQuery();
            for (int i = 0; i < requestParam.getReDiscoverArr().size(); i++) {
                String searchword = requestParam.getReDiscoverArr().get(i).getSearchword();
                postFilter.must(QueryBuilders.boolQuery()
                        .should(QueryBuilders.matchQuery("title", searchword).operator(Operator.AND))
                        .should(QueryBuilders.matchQuery("content", searchword).operator(Operator.AND))
                        .should(QueryBuilders.matchQuery("_file.nameOrg", searchword).operator(Operator.AND))
                        .should(QueryBuilders.matchQuery("_file.content", searchword).operator(Operator.AND)));
            }
            searchSourceBuilder.postFilter(postFilter);
            // post_filter 필터링 (재검색)
        }

        SearchRequest searchRequest = new SearchRequest(INDEX)
                .source(searchSourceBuilder);

        System.out.println(searchRequest);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

//        System.out.println(searchResponse.toString());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode jsonNode = objectMapper.readTree(String.valueOf(searchResponse));
        ArticleEnt art = objectMapper.treeToValue(jsonNode, ArticleEnt.class);

        return art;
    }


    // 결과내 재검색 시 ArrayList에 add 로직
    public RequestParam researchClear(RequestParam requestParam, ReDiscover reDiscover) {
//        System.out.println("다음은 reDiscoverArr add 로직");
//        System.out.println("requestParam" + requestParam);
//        System.out.println("reDiscover" + reDiscover);

        if (requestParam.isResearch()) {
//            System.out.println("flag1");
            ArrayList<ReDiscover> reDiscovers;
            if (requestParam.getReDiscoverArr() == null) {
//                System.out.println("flag2");
                reDiscovers = new ArrayList<>();
            } else {
//                System.out.println("flag3");
                reDiscovers = new ArrayList<>(requestParam.getReDiscoverArr());
            }
//            System.out.println("flag4: " + reDiscovers);
            reDiscovers.add(reDiscover);
            requestParam.setReDiscoverArr(reDiscovers);
        } else {
            requestParam.setReDiscoverArr(null);
        }
//        System.out.println("다음은 return 값" + requestParam);

        return requestParam;
    }

}
