package com.example.demo.service;

import org.elasticsearch.search.aggregations.bucket.terms.*;
import com.example.demo.config.ElasticsearchConfig;
import com.example.demo.model.*;
import com.example.demo.utility.EngToKor;
import com.example.demo.utility.IsOnlyEnglish;
import com.example.demo.utility.SearchParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.search.suggest.Suggest;
import org.elasticsearch.search.suggest.SuggestBuilder;
import org.elasticsearch.search.suggest.SuggestBuilders;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms.ParsedBucket;


import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



@Service
public class ElasticsearchService {

    RestHighLevelClient client;

    public void init() {
        client = ElasticsearchConfig.elasticsearchClient();
    }



    private static final String INDEX = "jsk_index";

    private static final String SAERCHLOG = "search-log";




    // 전체 쿼리 처리
    public ArticleEnt mainQuery(RequestParam requestParam) throws IOException {
        if (requestParam.getSearch().trim().equals("")) {         // 처음 입장 및 빈칸 검색 시 인덱스 GET
            ArticleEnt art;  // 전체 검색 반환으로.. GET ALL
            if(requestParam.getCategory().equals("전체")){         // 전체 카테고리검색에 search가 없을 경우 Aggs결과 반환
                art = getAllCategorySearch();
            } else {                                              // 전체가아닌 상세 카테고리를 빈칸으로 검색한 경우
                art = getEmptySearch(requestParam);
            }
            return art;
        }

        // 검색어 + - "" ( 포함 제외 정확히 일치 파싱해서 배열 형태로 모두 반환 ) (Utility의 SearchParser 클래스)
        searchParsedEntity searchedpEnt = SearchParser.searchParsing(requestParam);


        init();
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.trackTotalHits(true);   // default: hits 1만건 초과 -> value 1만개 고정.. 따라서 default "해제"

        // 정렬을 위한 설정
        String sorting_field = "";
        String sorting_standard = "";
        if (requestParam.getChoice().equals("accuracyorderby")) {
            sorting_field = "_score";
            sorting_standard = "DESC";
        } else if (requestParam.getChoice().equals("recentorderby")) {
            sorting_field = "wdate";
            sorting_standard = "DESC";
        } else if (requestParam.getChoice().equals("olderorderby")) {
            sorting_field = "wdate";
            sorting_standard = "ASC";
        }

        // 불용어 must_not 하기 위해 파악
        BanString ban = isBannedSearch_StopWord(requestParam.getSearch());





        BoolQueryBuilder multiMatchQuery = QueryBuilders.boolQuery();
        String need_search = null;
        StringBuilder stringBuilder = new StringBuilder();

        // RequestParam에 따른 동적 쿼리
        if (requestParam.getTarget().equals("getall")) { // 전체 검색이라면

            if (searchedpEnt.getMust().length > 0) {

                for (int i = 0; i < searchedpEnt.getMust().length; i++) {

                    String mustWord = searchedpEnt.getMust()[i];
                    if (i > 0) {
                        stringBuilder.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.
                    }
                    stringBuilder.append(mustWord);
                }
                need_search = stringBuilder.toString();

                multiMatchQuery.must(QueryBuilders.multiMatchQuery(need_search, "title", "content", "_file.nameOrg", "_file.content")
                        .operator(Operator.AND)
                        .minimumShouldMatch("1"));
            }

            if (searchedpEnt.getMust_not().length > 0 || ban.isBanned_stopword()) {
                stringBuilder.setLength(0); // 스트링 빌더 초기화

                for (int i = 0; i < searchedpEnt.getMust_not().length; i++) {
                    String mustNotWord = searchedpEnt.getMust_not()[i];

                    if (i > 0) {
                        stringBuilder.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.
                    }

                    stringBuilder.append(mustNotWord);
                }
                stringBuilder.append(ban.getBannedKeyWord_stopwrod()); // 불용어 또한 must_not 처리


                need_search = stringBuilder.toString();
                multiMatchQuery.mustNot(QueryBuilders.multiMatchQuery(need_search, "title", "content", "_file.nameOrg", "_file.content")
                        .operator(Operator.OR));
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

        }   // 전체 검색의 끝 ..


        // 전체 검색이아니라면 .. /
        else {
            String etc = getEtcField(requestParam.getTarget());

            stringBuilder.setLength(0); // 스트링 빌더 초기화
            for (int i = 0; i < searchedpEnt.getMust().length; i++) {
                String mustWord = searchedpEnt.getMust()[i];

                if (i > 0) {
                    stringBuilder.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.
                }
                stringBuilder.append(mustWord);
            }
            need_search = stringBuilder.toString();

            multiMatchQuery.must(QueryBuilders.boolQuery()
                    .should(QueryBuilders.matchQuery(etc, need_search).operator(Operator.AND))
                    .minimumShouldMatch(1));

            if (searchedpEnt.getMust_not().length > 0 || ban.isBanned_stopword()) {
                stringBuilder.setLength(0); // 스트링 빌더 초기화
//                BoolQueryBuilder mustNotQuery = QueryBuilders.boolQuery();
                for (int i = 0; i < searchedpEnt.getMust_not().length; i++) {
                    String mustNotWord = searchedpEnt.getMust_not()[i];

                    if (i > 0) {
                        stringBuilder.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.
                    }

                    stringBuilder.append(mustNotWord);
                }
                stringBuilder.append(ban.getBannedKeyWord_stopwrod()); // 불용어 또한 must_not 처리
                need_search = stringBuilder.toString();
                System.out.println("need_search: " + need_search);
                System.out.println("etc: " + etc);
                multiMatchQuery.mustNot(QueryBuilders.matchQuery(etc, need_search).operator(Operator.OR));
//                mustNotQuery.mustNot(QueryBuilders.matchQuery(etc, need_search).operator(Operator.AND));
            }


            if (searchedpEnt.getMatch_pharse().length > 0) {
                for (int i = 0; i < searchedpEnt.getMatch_pharse().length; i++) {
                    String matchPharseWord = searchedpEnt.getMatch_pharse()[i];
                    multiMatchQuery.must(QueryBuilders.matchPhraseQuery(etc, matchPharseWord));
                }
            }


        }   // 전체검색이 아닌경우의 끝 ..

        // post_filter 필터링 (재검색 존재 하는 경우)
        // 재 검색이 true 라면 && 재 검색의 리스트 배열이 null 이 아닌 경우
        BoolQueryBuilder outerBoolQuery = null;

        if (requestParam.isResearch() && requestParam.getReDiscoverArr() != null) {
            stringBuilder.setLength(0); // 스트링 빌더 초기화
            BoolQueryBuilder postFilter = QueryBuilders.boolQuery();

            for (int i = 0; i < requestParam.getReDiscoverArr().size(); i++) {
                String searchWord = requestParam.getReDiscoverArr().get(i).getSearchword();

                if (i > 0) {
                    stringBuilder.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.
                }
                stringBuilder.append(searchWord);
            }
            need_search = stringBuilder.toString();
            MultiMatchQueryBuilder multiMatchQuery_postFilter = QueryBuilders.multiMatchQuery(need_search, "title", "content", "_file.nameOrg", "_file.content")
                    .operator(Operator.AND);

            postFilter.should(multiMatchQuery_postFilter);

            outerBoolQuery = QueryBuilders.boolQuery().must(postFilter);
        }

        // 정렬 쿼리 빌더 생성
        FieldSortBuilder updatedAtSort = SortBuilders.fieldSort(sorting_field);
        // 정렬 방향 설정
        if (sorting_standard.equals("DESC")) {
            updatedAtSort.order(SortOrder.DESC);
        } else if (sorting_standard.equals("ASC")) {
            updatedAtSort.order(SortOrder.ASC);
        }


        BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();

        if (!requestParam.getCategory().equals("전체")) {
            filterQuery.must(QueryBuilders.matchQuery("category", requestParam.getCategory()).operator(Operator.AND));
        }


        // 조회기간 쿼리 빌더 생성
        if (!requestParam.getPeriod_of_view().equals("0")) { // 전체 기간 조회가아니면 조건을 달아주어야 하므로

            // 사용자로부터 넘겨받은 조회기간 선택.
            int wantdate = Integer.parseInt(requestParam.getPeriod_of_view());

            Date currentDate = new Date();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(currentDate);
            calendar.add(Calendar.DAY_OF_MONTH, -wantdate);
            Date calcDate = calendar.getTime(); // 하위 범위 날짜

            // 날짜 포맷 설정
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            filterQuery.must(QueryBuilders.rangeQuery("wdate")
                    .format("yyyy-MM-dd || yyyy-MM-dd HH:mm")
                    .gte(sdf.format(calcDate))
                    .lte(sdf.format(new Date())));
        }

        // 쿼리 결합
        BoolQueryBuilder finalQuery = QueryBuilders.boolQuery()
                .must(multiMatchQuery);
        if(requestParam.isResearch() && requestParam.getReDiscoverArr() != null){ finalQuery.must(outerBoolQuery);}
        if(!requestParam.getPeriod_of_view().equals("0") || !requestParam.getCategory().equals("전체")){  // 카테고리나 기간 필터 둘중 하나라도있으면 쿼리 생성
            finalQuery.filter(filterQuery);
        }

        // 정렬 쿼리 적용
        searchSourceBuilder.query(finalQuery).sort(updatedAtSort);

        searchSourceBuilder.size(20);    // 페이지당 20개씩 출력
        searchSourceBuilder.from((requestParam.getPage() - 1) * 5);   // Pagination을 위한 "from"

        System.out.println("requestParam : " + requestParam);

        searchSourceBuilder.highlighter(createHighlightBuilder());

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

        // 냉장고를 (sodwkdrh)로 잘못 입력한 경우 => "냉장고"로 return ( 검색결과가 없는 경우 && 오직 영어로만 검색어가 정해진 경우 )
        if (IsOnlyEnglish.isEnglishString(requestParam.getSearch()) && art.getHits().getHits().length == 0) {
//            System.out.println(EngToKor.engToKor(requestParam.getSearch()));
            requestParam.setSearch(EngToKor.engToKor(requestParam.getSearch()));  // 오타 가능성이 있는 영문을 한글로 바꾸어 다시 검색
            requestParam.setQuery(requestParam.getSearch());    // 영문검색을 다시 한글로 변환
            art = mainQuery(requestParam);
        }



        return art;
    }

    // 전체 카테고리 Aggs 반환
    private ArticleEnt getAllCategorySearch() throws IOException {
        init();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        sourceBuilder.size(0);

        TermsAggregationBuilder termsAggregation = AggregationBuilders.terms("categories")
                .field("category").size(15)
                .subAggregation(AggregationBuilders.topHits("top_documents").size(5));

        sourceBuilder.aggregation(termsAggregation);

        SearchRequest searchRequest = new SearchRequest("jsk_index");
//        System.out.println("전체 카테고리 요청!");
//        System.out.println(searchRequest);
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        System.out.println("전체카테고리 응답!");
//        System.out.println(searchResponse);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);   // 없는필드는 매핑 X
        JsonNode jsonNode = objectMapper.readTree(String.valueOf(searchResponse));
        ArticleEnt art = objectMapper.treeToValue(jsonNode, ArticleEnt.class);

//        System.out.println("전체카테고리!");
//        System.out.println(art);

        return art;
    }

    private String getEtcField(String target) {
        String etc = "";
        switch (target) {
            case "gettitle":
                etc = "title";
                break;
            case "getcontent":
                etc = "content";
                break;
            case "getfilename":
                etc = "_file.nameOrg";
                break;
            case "getfilecontent":
                etc = "_file.content";
                break;
            default:
                System.out.println("switch ERROR");
                break;
        }
        return etc;
    }

    private ArticleEnt getEmptySearch(RequestParam requestParam) throws IOException {

        init();
        QueryBuilder query;
        if ("전체".equals(requestParam.getCategory())) {
            query = QueryBuilders.matchAllQuery();
        } else {
            query = QueryBuilders.matchQuery("category", requestParam.getCategory());
        }

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.trackTotalHits(true);   // default: hits 1만건 초과 -> value 1만개 고정.. 따라서 default "해제"
        sourceBuilder.query(query);
        sourceBuilder.size(20); // 페이지당 20개씩 출력
        sourceBuilder.from((requestParam.getPage() - 1) * 20); // Pagination을 위한 "from"
        sourceBuilder.highlighter(createHighlightBuilder());

        SearchRequest searchRequest = new SearchRequest(INDEX).source(sourceBuilder);

        System.out.println("Search Request: " + searchRequest); // 요청쿼리 출력

            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            JsonNode jsonNode = objectMapper.readTree(searchResponse.toString());
            ArticleEnt art = objectMapper.treeToValue(jsonNode, ArticleEnt.class);




//        System.out.println(art);
        return art;
    }


    // 결과내 재검색 시 ArrayList에 add 로직
    public RequestParam reSearchClear(RequestParam requestParam, ReDiscover reDiscover) {
//        System.out.println("다음은 reDiscoverArr add 로직");
//        System.out.println("requestParam" + requestParam);
//        System.out.println("reDiscover" + reDiscover);
//        requestParam = setPrevSearch(requestParam); // 이전검색어 지정

        if (requestParam.isResearch()) {    // 결과내 재 검색이 True인경우
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
        } else {    // 결과내 재검색 아닌 경우 재검색 List 초기화
            requestParam.setReDiscoverArr(null);
        }
//        System.out.println("다음은 return 값" + requestParam);

        return requestParam;
    }

    private HighlightBuilder createHighlightBuilder() {
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.preTags("<span style='color:red;'><b><u>");
        highlightBuilder.postTags("</u></b></span>");
        highlightBuilder.field("title").highlighterType("plain");
        highlightBuilder.field("content").highlighterType("plain");
        highlightBuilder.field("_file.content").highlighterType("plain");
        highlightBuilder.field("_file.nameOrg").highlighterType("plain");
        highlightBuilder.fragmentSize(300);
//        highlightBuilder.requireFieldMatch(false); //
        return highlightBuilder;
    }


    public void crawlLog(RequestParam requestParam, String hit_total, int took){
        System.out.println("크롤 param" +  requestParam);

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
                    .field("search", requestParam.getSearch())
                    .field("query", requestParam.getQuery())
                    .field("re", requestParam.isResearch())
                    .field("referer", "none")
                    .field("sort", sort)
                    .field("took", took)
                    .field("total", hit_total)
                    .field("user", extractedUsername)
                    .field("prevSearch",requestParam.getPrevSearch());

                    if(requestParam.getReDiscoverArr() == null){
                        builder.field("rediscoverarr", (Boolean) null);
                    } else {
                        String needparse = requestParam.getReDiscoverArr().toString();
                        System.out.println("needparse : " + needparse);
                        String regex = "searchword=([^\\]]+)";

                        Pattern pattern = Pattern.compile(regex);
                        Matcher matcher = pattern.matcher(needparse);

                        StringBuilder getparseBuilder = new StringBuilder();

                        while (matcher.find()) {
                            String extractedValue = matcher.group(1);
                            if (getparseBuilder.length() > 0) {
                                getparseBuilder.append(", ");  // 이미 내용이 있는 경우 쉼표와 공백 추가
                            }
                            getparseBuilder.append(extractedValue);
                        }

                        if (getparseBuilder.length() > 0) {
                            getparseBuilder.setLength(getparseBuilder.length() - 1); // 마지막 문자 제거
                        }

                        String getparse = getparseBuilder.toString();
                        System.out.println("getparse : " + getparse);

                        builder.field("rediscoverarr", getparse);

                    }
                    builder.endObject();


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

    public List<TopKeyword> top_search_log() throws IOException {

        init();
        // 쿼리 빌더 생성
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.rangeQuery("createdDate").gte("now-7d/d"));
        sourceBuilder.size(0);

        // 집계(aggregation) 설정
        TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms("popular_search_terms")
                .field("query")
                .size(10);

        sourceBuilder.aggregation(aggregationBuilder);

        // SearchRequest 생성
        SearchRequest searchRequest = new SearchRequest(SAERCHLOG);
        searchRequest.source(sourceBuilder);

        // 쿼리 실행
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        // 집계 결과 처리
        Terms termsAggregation = searchResponse.getAggregations().get("popular_search_terms");
        List<? extends Terms.Bucket> buckets = termsAggregation.getBuckets();

        // TopKeyword 리스트 초기화
        List<TopKeyword> topKeywords = new ArrayList<>();

        // 결과를 리스트에 저장
        for (Terms.Bucket bucket : buckets) {
            String key = bucket.getKeyAsString();
            long doc_Count = bucket.getDocCount();

            // TopKeyword 객체 생성 및 값 설정
            TopKeyword topKeyword = new TopKeyword();
            topKeyword.setKey(key);
            topKeyword.setDoc_count(doc_Count);

            // 리스트에 추가
            topKeywords.add(topKeyword);
        }
        return topKeywords;
    }


    public List<String> getAutoComplete(String searchdata) throws IOException {

        init();
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
//        System.out.println("searchdata: " + searchdata);
        sourceBuilder.query(QueryBuilders.matchQuery("keyword", searchdata));
        sourceBuilder.size(5);

        SearchRequest searchRequest = new SearchRequest("autocomplete-dict");
        searchRequest.source(sourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        List<String> result = new ArrayList<>();
        Set<String> uniqueKeywords = new HashSet<>();

        for (int i = 0; i < searchResponse.getHits().getHits().length; i++) {
            String keyword = searchResponse.getHits().getHits()[i].getSourceAsMap().get("keyword").toString();
            uniqueKeywords.add(keyword);
        }
        result.addAll(uniqueKeywords);
        Collections.sort(result, Comparator.comparing(String::length));   // 길이순 정렬
//        System.out.println("정렬:" + result);
        return result;
    }


    // Pagination
    public int getTotalPages(String value, int pagesize) {

        return (int) Math.ceil(Double.parseDouble(value) / pagesize);
    }

    public String typoCorrect(String search) throws IOException {
        if(search.isEmpty() == true || search == null){
            return null;
        }
        System.out.println("검색어: " + search);
        init();

        SuggestBuilder suggestBuilder = new SuggestBuilder().addSuggestion("mySuggestion", SuggestBuilders.termSuggestion("keyword.suggest").text(search));
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder().suggest(suggestBuilder);
        SearchRequest searchRequest = new SearchRequest("typocorrect-dict");
        searchRequest.source(sourceBuilder);
//        System.out.println(searchRequest);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
        Suggest suggest = searchResponse.getSuggest();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(suggest.toString());

        JsonNode mySuggestions = jsonNode.get("suggest").get("mySuggestion");
        JsonNode lastMySuggestion = mySuggestions.get(mySuggestions.size() - 1);
        JsonNode options;
        try {
            options = lastMySuggestion.get("options");
        } catch (NullPointerException e){
            return null;
        }
//        System.out.println(options.size());
        if(options.size() == 0){    // 오타제안이 없다면 (검색결과가 존재한다면) null Return
            return null;
        }
        System.out.println("JsonNode : " + options);
        String lastOptionText = options.get(0).get("text").asText();
        System.out.println("오타교정 : " + lastOptionText);
        String normalizedText = Normalizer.normalize(lastOptionText, Normalizer.Form.NFC);


        return normalizedText;      // 오타 교정 제안이 있다면 해당 String Return (option 마지막 인덱스의 Score가 가장 높은 String)
    }

    public RecommManual recommManualGet(String search) throws IOException { // 수동 추천 검색어

        init();

        MatchQueryBuilder matchQuery = QueryBuilders.matchQuery("recomm", search);

        // 검색 요청을 생성
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(matchQuery);

        SearchRequest searchRequest = new SearchRequest("recomm-manual-dict");
        searchRequest.searchType(SearchType.DFS_QUERY_THEN_FETCH);
        searchRequest.source(sourceBuilder);

        // 쿼리를 실행
        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);


        //        System.out.println(searchResponse.toString());
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // 검색 결과에서 "recomm" 배열을 추출하여 RecommManual 객체에 저장
        if (searchResponse.getHits().getTotalHits().value > 0) {
            RecommManual recommManual = objectMapper.convertValue(searchResponse.getHits().getAt(0).getSourceAsMap(), RecommManual.class);

            // WhiteSpace로 구분하여 수동검색어에서 일치하는것들 제외
            String[] searchWords = search.split(" ");
            for (String word : searchWords) {
                recommManual.removeItemContaining(word);
            }

            System.out.println(recommManual);
            return recommManual;
        }
        else {
            return null;
        }






    }

    public BanString isBannedSearch_Forbidden(String search) throws IOException {

        init();
        SearchRequest searchRequest = new SearchRequest("forbiddenword-dict");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("keyword", search));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
//        System.out.println("금칙어검색 테스트!!!");
//        System.out.println(searchRequest.toString());
//        System.out.println(searchResponse.toString());
//        System.out.println(searchResponse.getHits().getTotalHits().value);
        BanString banned = new BanString();
        if(searchResponse.getHits().getTotalHits().value > 0){  // 금칙어에 대한 포함 결과가 반환된 경우
            banned.setBanned_forbidden(true);

            StringBuffer str = new StringBuffer();

            // hits 배열을 순회하며 _source 필드를 StringBuffer에 추가
            for (int i = 0; i < searchResponse.getHits().getHits().length; i++) {
                String keyword = (String) searchResponse.getHits().getHits()[i].getSourceAsMap().get("keyword");    // json 형태를 문자열로
                if (i > 0) {
                    str.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.   // 어떤 것이 문제인지 알려주기 위해서.
                }
                str.append(keyword); // StringBuffer에 추가
            }

            String result = str.toString(); // 최종 결과 문자열
            banned.setBannedKeyWord_forbidden(result);
        }

        return banned;

    }

    public RequestParam checkMinus(RequestParam requestParam) {



        if(requestParam.getPage() < 1){
            requestParam.setPage(1);
        }

        if(Integer.parseInt(requestParam.getPeriod_of_view()) < 0){
            requestParam.setPeriod_of_view("0");
        }

        return requestParam;

    }

    public List<String> autoRecomm(String search, String prevSearch, boolean isResearch) throws IOException {   // 자동 추천 검색어 (String 형태로)

        init();
        SearchRequest searchRequest = new SearchRequest("search-log");  // 로그 기반
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        if(isResearch){
            // Query
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.must(QueryBuilders.matchQuery("search", search));
            boolQuery.must(QueryBuilders.matchQuery("re", true));
            searchSourceBuilder.query(boolQuery);

            // Aggregation
            TermsAggregationBuilder termsAggregation = AggregationBuilders.terms("re_search_terms")
                    .field("rediscoverarr.keyword")
                    .size(1);
            searchSourceBuilder.aggregation(termsAggregation);

            // Size
            searchSourceBuilder.size(0);

            searchRequest.source(searchSourceBuilder);

            System.out.println(searchRequest);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            ParsedStringTerms reSearchTerms = searchResponse.getAggregations().get("re_search_terms");
            List<ParsedTerms.ParsedBucket> buckets = (List<ParsedTerms.ParsedBucket>) reSearchTerms.getBuckets();

            if (!buckets.isEmpty()) {   // 로그 기반 자동추천 검색어 존재할 경우 ..
                ParsedStringTerms.ParsedBucket firstBucket = (ParsedStringTerms.ParsedBucket) buckets.get(0);
                String firstBucketKey = firstBucket.getKeyAsString();
                List<String> res = new ArrayList<>(); // 리스트 초기화
                res.add(firstBucketKey);
                System.out.println("First bucket key: " + firstBucketKey);
                return res;
            } else {
                Collections.emptyList();    // 존재하지 않으면 프론트에서도 출력하지 않기 위해 null로 일단 전달 ..
            }
        } else if (!search.equals("") && !prevSearch.equals("") || !(prevSearch == null)){
            BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
            boolQuery.should(QueryBuilders.matchQuery("search", search)).should(QueryBuilders.matchQuery("prevSearch", prevSearch));
            searchSourceBuilder.query(boolQuery);

            // Aggregation
            TermsAggregationBuilder termsAggregation = AggregationBuilders.terms("prevSearch")
                    .field("search.keyword")
                    .size(5); // Get top 5 previous search terms
            searchSourceBuilder.aggregation(termsAggregation);

            // Size
            searchSourceBuilder.size(0);

            searchRequest.source(searchSourceBuilder);


            System.out.println(searchRequest);
            SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);
            ParsedStringTerms prevSearchTerms = searchResponse.getAggregations().get("prevSearch");
            List<ParsedBucket> buckets = (List<ParsedBucket>) prevSearchTerms.getBuckets();

            List<String> recommendedSearchTerms = new ArrayList<>();

            for (ParsedBucket bucket : buckets) {
                String bucketKey = bucket.getKeyAsString();
                recommendedSearchTerms.add(bucketKey);
            }

            return recommendedSearchTerms;
        }
        return null;
    }



    public BanString isBannedSearch_StopWord(String search) throws IOException {

        init();
        SearchRequest searchRequest = new SearchRequest("stopword-dict");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.query(QueryBuilders.matchQuery("keyword", search));
        searchRequest.source(searchSourceBuilder);

        SearchResponse searchResponse = client.search(searchRequest, RequestOptions.DEFAULT);

        BanString banned = new BanString();
        if(searchResponse.getHits().getTotalHits().value > 0){  // 금칙어에 대한 포함 결과가 반환된 경우
            banned.setBanned_stopword(true);

            StringBuffer str = new StringBuffer();

            // hits 배열을 순회하며 _source 필드를 StringBuffer에 추가
            for (int i = 0; i < searchResponse.getHits().getHits().length; i++) {
                String keyword = (String) searchResponse.getHits().getHits()[i].getSourceAsMap().get("keyword");    // json 형태를 문자열로
                if (i > 0) {
                    str.append(" "); // 첫 단어 이후 단어마다 한칸 씩 띄어서 저장.   // 어떤 것이 문제인지 알려주기 위해서.
                }
                str.append(keyword); // StringBuffer에 추가
            }

            String result = str.toString(); // 최종 결과 문자열
            banned.setBannedKeyWord_stopwrod(result);
        }

        return banned;



    }

}
