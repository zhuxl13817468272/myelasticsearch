package com.example.myelasticsearch.dao;

import com.example.myelasticsearch.domain.Item;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ItemRepository extends ElasticsearchRepository<Item,Long> {

    @Query("{\"match\":{\"title\": \"?0\"}}")
    List<Item> findByMatch(String name);

    @Query("{" +
            "    \"multi_match\": {" +
            "      \"query\": \"?0\"," +
            "      \"fields\": [\"title\",\"category\"]" +
            "    }" +
            "  }")
    List<Item> findByMultiMatch(String value);


    @Query("{" +
            "    \"range\": {" +
            "      \"price\": {" +
            "        \"gte\": \"?0\"," +
            "        \"lte\": \"?1\"" +
            "      }" +
            "    }" +
            "  }")
    public List<Item> findByRange(String minValue, String maxValue);

}
