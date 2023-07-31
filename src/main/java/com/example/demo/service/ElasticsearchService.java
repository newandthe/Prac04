package com.example.demo.service;

import com.example.demo.config.ElasticsearchConfig;
import com.example.demo.model.ArticleEnt;
import com.example.demo.model.ReDiscover;
import com.example.demo.model.RequestParam;
import com.example.demo.model.searchParsedEntity;
import com.example.demo.utility.EngToKor;
import com.example.demo.utility.IsOnlyEnglish;
import com.example.demo.utility.SearchParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.management.Query;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticsearchService {

    RestHighLevelClient client;

    public void init() {
        client = ElasticsearchConfig.elasticsearchClient();
    }



    private static final String INDEX = "jsk_index";

    private static final String SAERCHLOG = "search-log";




    // 전체 쿼리 처리
    public ArticleEnt sampleQuery(RequestParam requestParam) throws IOException {
        if(requestParam.getSearch().trim().equals("")){
            ArticleEnt art = new ArticleEnt();  // hits = null 인 객체 생성해서 반환
            return art;
        }

        // 검색어 + - "" ( 포함 제외 정확히 일치 파싱해서 배열 형태로 모두 반환 ) (Utility의 SearchParser 클래스)
        searchParsedEntity searchedpEnt;
        searchedpEnt = SearchParser.searchParsing(requestParam);

        init();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(true);   // default: hits 1만건 초과 -> value 1만개 고정.. 따라서 default "해제"

        // 정렬을 위한 설정
        String sorting_field = "";
        String sorting_standard = "";
        if(requestParam.getChoice().equals("accuracyorderby")){
            sorting_field = "_score";
            sorting_standard = "DESC";
        } else if(requestParam.getChoice().equals("recentorderby")){
            sorting_field = "wdate";
            sorting_standard = "DESC";
        } else if(requestParam.getChoice().equals("olderorderby")){
            sorting_field = "wdate";
            sorting_standard = "ASC";
        }




        // RequestParam에 따른 동적 쿼리
        if (requestParam.getTarget().equals("getall")) { // 전체 검색이라면


            BoolQueryBuilder categoryQuery = QueryBuilders.boolQuery();
            if (!requestParam.getCategory().equals("전체")) {
                categoryQuery.should(QueryBuilders.matchQuery("category", requestParam.getCategory()));
            }

            BoolQueryBuilder multiMatchQuery = QueryBuilders.boolQuery();


            if (searchedpEnt.getMust().length > 0) {
                for (int i = 0; i < searchedpEnt.getMust().length; i++) {
                    String mustWord = searchedpEnt.getMust()[i];
                    multiMatchQuery.should(QueryBuilders.matchQuery("title", mustWord))
                            .should(QueryBuilders.matchQuery("content", mustWord))
                            .should(QueryBuilders.matchQuery("_file.nameOrg", mustWord))
                            .should(QueryBuilders.matchQuery("_file.content", mustWord));
                }
            }

            if (searchedpEnt.getMust_not().length > 0) {
                for (int i = 0; i < searchedpEnt.getMust_not().length; i++) {
                    String mustNotWord = searchedpEnt.getMust_not()[i];
                    multiMatchQuery.mustNot(QueryBuilders.matchQuery("title", mustNotWord))
                            .mustNot(QueryBuilders.matchQuery("content", mustNotWord))
                            .mustNot(QueryBuilders.matchQuery("_file.nameOrg", mustNotWord))
                            .mustNot(QueryBuilders.matchQuery("_file.content", mustNotWord));
                }
            }

            if (searchedpEnt.getMatch_pharse().length > 0) {
                for (int i = 0; i < searchedpEnt.getMatch_pharse().length; i++) {
                    String matchPharseWord = searchedpEnt.getMatch_pharse()[i];
                    multiMatchQuery.must(QueryBuilders.matchPhraseQuery("title", matchPharseWord))
                            .must(QueryBuilders.matchPhraseQuery("content", matchPharseWord))
                            .must(QueryBuilders.matchPhraseQuery("_file.nameOrg", matchPharseWord))
                            .must(QueryBuilders.matchPhraseQuery("_file.content", matchPharseWord));
                }
            }



            // 정렬 쿼리 빌더 생성
            FieldSortBuilder updatedAtSort = SortBuilders.fieldSort(sorting_field);
            // 정렬 방향 설정
            if (sorting_standard.equals("DESC")) {
                updatedAtSort.order(SortOrder.DESC);
            } else if (sorting_standard.equals("ASC")) {
                updatedAtSort.order(SortOrder.ASC);
            }

            // 쿼리 결합
            BoolQueryBuilder finalQuery = QueryBuilders.boolQuery()
                    .must(categoryQuery)
                    .must(multiMatchQuery);

            // 정렬 쿼리까지 적용
            searchSourceBuilder.query(finalQuery).sort(updatedAtSort);


        }
        // 전체 검색이아니라면 .. // 깔끔하게하려면 검색로직 함수로 빼내서 호출하도록 ....
        else {
            String etc = "";
//        System.out.println(etc);
            if (requestParam.getTarget().equals("gettitle")) {  // 제목으로 검색인 경우
                etc = "title";
            } else if (requestParam.getTarget().equals("getcontent")) { // 내용으로 검색인 경우
                etc = "content";
            } else if (requestParam.getTarget().equals("getfilename")) { // 첨부 파일 명으로 검색인 경우
                etc = "_file.nameOrg";
            } else if (requestParam.getTarget().equals("getfilecontent")) { // 첨부 파일 명으로 검색인 경우
                etc = "_file.content";
            }

            BoolQueryBuilder categoryQuery = QueryBuilders.boolQuery();
            if (!requestParam.getCategory().equals("전체")) {
                categoryQuery.should(QueryBuilders.matchQuery("category", requestParam.getCategory()));
            }

            BoolQueryBuilder multiMatchQuery = QueryBuilders.boolQuery();

            for (int i = 0; i < searchedpEnt.getMust().length; i++) {
                String mustWord = searchedpEnt.getMust()[i];
                multiMatchQuery.must(QueryBuilders.matchQuery(etc, mustWord));
            }

            if (searchedpEnt.getMust_not().length > 0) {
                BoolQueryBuilder mustNotQuery = QueryBuilders.boolQuery();
                for (int i = 0; i < searchedpEnt.getMust_not().length; i++) {
                    String mustNotWord = searchedpEnt.getMust_not()[i];
                    mustNotQuery.mustNot(QueryBuilders.matchQuery(etc, mustNotWord));
                }
                multiMatchQuery.filter(mustNotQuery);
            }

            if (searchedpEnt.getMatch_pharse().length > 0) {
                for (int i = 0; i < searchedpEnt.getMatch_pharse().length; i++) {
                    String matchPharseWord = searchedpEnt.getMatch_pharse()[i];
                    multiMatchQuery.must(QueryBuilders.matchPhraseQuery(etc, matchPharseWord));
                }
            }

            // 정렬 쿼리 빌더 생성
            FieldSortBuilder updatedAtSort = SortBuilders.fieldSort(sorting_field);
            // 정렬 방향 설정
            if (sorting_standard.equals("DESC")) {
                updatedAtSort.order(SortOrder.DESC);
            } else if (sorting_standard.equals("ASC")) {
                updatedAtSort.order(SortOrder.ASC);
            }

            // 쿼리 결합
            BoolQueryBuilder finalQuery = QueryBuilders.boolQuery()
                    .must(categoryQuery)
                    .must(multiMatchQuery);

            // 정렬 쿼리까지 적용
            searchSourceBuilder.query(finalQuery).sort(updatedAtSort);

        }

        searchSourceBuilder.size(5);    // 페이지당 5개씩 출력
        searchSourceBuilder.from((requestParam.getPage()-1) * 5);   // Pagination을 위한 "from"

        System.out.println("requestParam : " + requestParam);

        // post_filter 필터링 (재검색)
        // 재 검색이 true 라면 && 재 검색의 리스트 배열이 null 이 아닌 경우
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
        }

        SearchRequest searchRequest = new SearchRequest(INDEX)
                .source(searchSourceBuilder);

        System.out.println(searchRequest);  // 요청쿼리 출력

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

//        System.out.println(searchResponse.toString());  // 결과 json 출력

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        JsonNode jsonNode = objectMapper.readTree(String.valueOf(searchResponse));
        ArticleEnt art = objectMapper.treeToValue(jsonNode, ArticleEnt.class);


        crawlLog(requestParam, art.getHits().getTotal().getValue(), art.getTook());     // 검색로그 저장
//        System.out.println(art.getHits().getTotal().getValue());  // value 값
//        System.out.println(art.getTook());  // took 값

        // 냉장고를 (sodwkdrh)로 잘못 입력한 경우 => "냉장고"로 return ( 검색결과가 없는 경우 && 오직 영어로만 검색어가 정해진 경우 )
        if (IsOnlyEnglish.isEnglishString(requestParam.getSearch()) && art.getHits().getHits().length == 0){
//            System.out.println(EngToKor.engToKor(requestParam.getSearch()));
            requestParam.setSearch( EngToKor.engToKor(requestParam.getSearch()) );  // 오타 가능성이 있는 영문을 한글로 바꾸어 다시 검색
            art = sampleQuery(requestParam);
        }

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


    public void crawlLog(RequestParam requestParam, String hit_total, int took){

        // 파라미터 정의 시작
        // asc 정의
        Boolean asc;
        if(requestParam.getChoice().equals("accuracyorderby")  || requestParam.getChoice().equals("recentorderby")){
            asc = false; // desc
        } else {
            asc = true;
        }

        // createdDate 정의
        // 현재 시간 얻기
        LocalDateTime now = LocalDateTime.now();
        // 포맷 정의
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        // 형식에 맞게 문자열로 변환
        String createdDate = now.format(formatter);
        System.out.println(createdDate);

        String domain = "0번사전";

        String ip = "127.0.0.1";
        // IP 주소 취득
        try {
            // 현재 호스트의 InetAddress 객체 얻기
            InetAddress localHost = InetAddress.getLocalHost();

            // IP 주소 얻기
            ip = localHost.getHostAddress();

            // 결과 출력
//            System.out.println("현재 IP 주소: " + ip);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        // 정렬기준
        String sort="_score";
        if(requestParam.getChoice().equals("accuracyorderby")){
            sort = "_score";
        } else if (requestParam.getChoice().equals("recentorderby") || requestParam.getChoice().equals("olderorderby")){
            sort = "wdate";
        }

        // user ID 취득
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        int startIndex = username.indexOf("username=") + "username=".length();
        int endIndex = username.indexOf(",", startIndex);
        String extractedUsername = username.substring(startIndex, endIndex);


        // 파라미터 정의 끝

        try {
            XContentBuilder builder = XContentFactory.jsonBuilder()
                    .startObject()
                    .field("asc", asc)
                    .field("category", requestParam.getCategory())
                    .field("createdDate", createdDate)
                    .field("domain", domain)
                    .field("ip", ip)
                    .field("oquery", requestParam.getReDiscoverParam())
                    .field("param", requestParam.toString())
                    .field("query", requestParam.getQuery())
                    .field("re", requestParam.isResearch())
                    .field("referer", "none")
                    .field("sort", sort)
                    .field("took", took)
                    .field("total", hit_total)
                    .field("user", extractedUsername)
                    .endObject();


            System.out.println("builder!!!!!");
            System.out.println(builder);
            // IndexRequest 객체 생성 및 JSON 문서 추가
            IndexRequest indexRequest = new IndexRequest("search-log") // 인덱스 이름
                    .source(builder);

            // IndexRequest를 Elasticsearch에 전송하여 데이터를 인덱스에 추가
            IndexResponse indexResponse = client.index(indexRequest, RequestOptions.DEFAULT);

            System.out.println(indexResponse);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }

}
