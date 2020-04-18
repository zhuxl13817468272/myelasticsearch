package com.example.myelasticsearch.config;

import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.RestClients;
import org.springframework.data.elasticsearch.config.AbstractElasticsearchConfiguration;

@Configuration
public class ElasticSearchConfig extends AbstractElasticsearchConfiguration {
    @Override
    public RestHighLevelClient elasticsearchClient() {
        ClientConfiguration configuration = ClientConfiguration.builder()
                .connectedTo("xxx.xxx.xxx.xxx:9200")
                .withBasicAuth("xxxx", "xxxx") //如果没有用户名和密码也可以不用写
                .build();

        RestHighLevelClient client = RestClients.create(configuration).rest();
        return client;
    }

}
