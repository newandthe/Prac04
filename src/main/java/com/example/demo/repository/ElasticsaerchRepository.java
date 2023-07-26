//package com.example.demo.repository;
//
//import lombok.RequiredArgsConstructor;
//import org.elasticsearch.action.search.SearchRequest;
//import org.elasticsearch.action.search.SearchResponse;
//import org.elasticsearch.client.RequestOptions;
//import org.elasticsearch.client.RestHighLevelClient;
//import org.elasticsearch.search.builder.SearchSourceBuilder;
//import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
//import org.springframework.stereotype.Repository;
//
//import java.io.IOException;
//
//@RequiredArgsConstructor
//public class ElasticsaerchRepository {
//
//    private final ElasticsearchOperations elasticsaerchTemplate;
//    private final RestHighLevelClient client;
//
//    public SearchResponse search(String indexName, SearchSourceBuilder searchSourceBuilder) throws IOException{
//        SearchRequest searchRequest = getSearchRequest(indexName, searchSourceBuilder);
//        return client.search(searchRequest, RequestOptions.DEFAULT);
//    }
//
//    private SearchRequest getSearchRequest(String indexName, SearchSourceBuilder searchSourceBuilder) {
//        SearchRequest searchRequest = new SearchRequest("posts");
//        searchRequest.indices(indexName).source(searchSourceBuilder);
//        return searchRequest;
//    }
//}
