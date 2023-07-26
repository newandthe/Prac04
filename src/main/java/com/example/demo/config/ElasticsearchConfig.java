package com.example.demo.config;

import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchConfig {

    private final String elasticsearchIndexName;
    private static String elasticsearchUris;
    private final String elasticsearchClusterName;
    private final String elasticsearchUsername;
    private final String elasticsearchPassword;

    public ElasticsearchConfig(
            @Value("${elasticsearch.index.name}") String elasticsearchIndexName,
            @Value("${spring.elasticsearch.uris}") String elasticsearchUris,
            @Value("${spring.elasticsearch.rest.cluster-name}") String elasticsearchClusterName,
            @Value("${spring.elasticsearch.username}") String elasticsearchUsername,
            @Value("${spring.elasticsearch.password}") String elasticsearchPassword) {
        this.elasticsearchIndexName = elasticsearchIndexName;
        this.elasticsearchUris = elasticsearchUris;
        this.elasticsearchClusterName = elasticsearchClusterName;
        this.elasticsearchUsername = elasticsearchUsername;
        this.elasticsearchPassword = elasticsearchPassword;
    }

    @Bean
    public static RestHighLevelClient elasticsearchClient() {
//        System.out.println("elasticsearchClusterName : " + elasticsearchClusterName);
//        System.out.println("elasticsearchUsername : " + elasticsearchUsername);
//        System.out.println("elasticsearchPassword : " + elasticsearchPassword);
//        System.out.println("elasticsearchIndexName : " + elasticsearchIndexName);

        String[] uriArr = elasticsearchUris.split(",");
        HttpHost[] httpHosts = new HttpHost[uriArr.length];
        for (int i = 0; i < uriArr.length; i++) {
            httpHosts[i] = HttpHost.create(uriArr[i]);
        }

        CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
        credentialsProvider.setCredentials(AuthScope.ANY,
                new UsernamePasswordCredentials("admin", "admin"));

        return new RestHighLevelClient(
                RestClient.builder(httpHosts)
                        .setHttpClientConfigCallback(httpClientBuilder ->
                                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                        )
        );
    }
}
