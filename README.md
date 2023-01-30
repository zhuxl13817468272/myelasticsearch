一、项目技术简介:

     首先，本项目是直接copy代码可以运行成功的。
     其次，本项目使用高版本官方推荐的ElasticsearchRestTemplate。Elasticsearch在高版本中，官方建议我们使用：High Level REST Client，也可以兼容低版本的ElasticsearchTemplate。官方参考网址:https://docs.spring.io/spring-data/elasticsearch/docs/3.2.6.RELEASE/reference/html/#elasticsearch.operations.resttemplate。 很好的项目实例：https://github.com/spring-projects/spring-data-elasticsearch
     最后，本项目中连接Elasticsearch数据层除了使用Spring Data Jpa的API之外，也使用了DSL(Domain Special Language)语言。DSL语言参考：https://www.cnblogs.com/ifme/p/12004660.html。 聚合度量的加条件返回：https://elasticsearch.cn/article/13501 。Spring Data ElasticSearch参考文章：https://www.cnblogs.com/ifme/p/12005026.html
     
二、项目前提--安装elasticsearch：

    文章推荐使用Linux系统Centos7 基于Docker搭建ELK分布式日志系统，文章地址：https://www.jianshu.com/p/4997f4a02d23。
    当然也可以只安装elasticsearch部分。
    
三、开发SpringBoot+Elasticsearch集成实战：
  
  1. 配置（pom.xml + config配置）
  
   1.1） pom.xml文件：

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <parent>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-parent</artifactId>
            <version>2.2.6.RELEASE</version>
            <relativePath/> <!-- lookup parent from repository -->
        </parent>
        <groupId>com.example</groupId>
        <artifactId>myelasticsearch</artifactId>
        <version>0.0.1-SNAPSHOT</version>
        <name>myelasticsearch</name>
        <description>Demo project for Spring Boot</description>

        <properties>
            <java.version>1.8</java.version>
        </properties>

        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-data-elasticsearch</artifactId>
            </dependency>

            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-test</artifactId>
                <scope>test</scope>
                <exclusions>
                    <exclusion>
                        <groupId>org.junit.vintage</groupId>
                        <artifactId>junit-vintage-engine</artifactId>
                    </exclusion>
                </exclusions>
            </dependency>

            <!-- 新增 lombok -->
            <dependency>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <optional>true</optional>
            </dependency>

        </dependencies>

        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>
            </plugins>
        </build>
    </project>
    
  1.2） 方式一、配置类/config/ElasticSearchConfig.java

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
    
  方式二、配置文件application.properties
  
    spring.elasticsearch.rest.uris=xxx.xxx.xxx.xxx:9200
    spring.elasticsearch.rest.username=xxxx
    spring.elasticsearch.rest.password=xxxx
    spring.elasticsearch.rest.connection-timeout=5000
    spring.elasticsearch.rest.read-timeout=30000

2. 代码如下(实体类domain + 数据层dao + 测试类test)

  2.1) 实体类/domain/Item.java

    package com.example.myelasticsearch.domain;

    import lombok.Data;
    import lombok.ToString;
    import lombok.experimental.Accessors;
    import org.springframework.data.annotation.Id;
    import org.springframework.data.elasticsearch.annotations.Document;
    import org.springframework.data.elasticsearch.annotations.Field;
    import org.springframework.data.elasticsearch.annotations.FieldType;

    @Data
    @Accessors(chain = true)
    @ToString
    @Document(indexName = "item2",type = "docs", shards = 1, replicas = 0)
    public class Item {

        @Id
        private Long id;

        @Field(type = FieldType.Text, analyzer = "ik_max_word")
        private String title; //标题

        @Field(type = FieldType.Keyword)
        private String category;// 分类

        @Field(type = FieldType.Keyword)
        private String brand; // 品牌

        @Field(type = FieldType.Double)
        private Double price; // 价格

        @Field(index = false, type = FieldType.Keyword)
        private String images; // 图片地址

        public Item() {
        }

        public Item(Long id, String title, String category, String brand, Double price, String images) {
            this.id = id;
            this.title = title;
            this.category = category;
            this.brand = brand;
            this.price = price;
            this.images = images;
        }
    }

  2.2）数据层 dao/ItemRepository.java
  
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
    
  2.3） 测试类 MyelasticsearchApplicationTests
  
    package com.example.myelasticsearch;

    import com.example.myelasticsearch.dao.ItemRepository;
    import com.example.myelasticsearch.domain.Item;
    import org.junit.jupiter.api.Test;
    import org.springframework.beans.factory.annotation.Autowired;
    import org.springframework.boot.test.context.SpringBootTest;
    import org.springframework.data.domain.Sort;
    import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

    import java.util.ArrayList;
    import java.util.List;
    import java.util.Optional;

    @SpringBootTest
    class MyelasticsearchApplicationTests {

        @Autowired
        private ElasticsearchRestTemplate elasticsearchRestTemplate;
        @Autowired
        private ItemRepository itemRepository;

        @Test
        void contextLoads() {
        }

        @Test
        public void testCreate() {
            //创建索引，会根据item类的@Document注解信息来创建
            boolean index = elasticsearchRestTemplate.createIndex(Item.class);
            //配置索引，会根据Item类中的id、Feild等字段来自动完成映射
            boolean b = elasticsearchRestTemplate.putMapping(Item.class);
            System.out.println("创建索引和类型："+index+"。创建field:"+b);

        }

        @Test
        public void testAdd() {
            Item item = new Item(1L, "小米手机7", " 手机",
                    "小米", 3499.00, "http://image.leyou.com/13123.jpg");
            itemRepository.save(item);
        }

        @Test
        public void indexList() {
            List<Item> list = new ArrayList<>();
            list.add(new Item(2L, "坚果手机R1", " 手机", "锤子", 3699.00, "http://image.leyou.com/123.jpg"));
            list.add(new Item(3L, "华为META10", " 手机", "华为", 4499.00, "http://image.leyou.com/3.jpg"));
            // 接收对象集合，实现批量新增
            itemRepository.saveAll(list);
        }

        @Test
        public void testDelete() {
            itemRepository.deleteById(1L);
        }

        @Test
        public void testQuery(){
            Optional<Item> optional = itemRepository.findById(2L);
            System.out.println(optional.get());
        }

        @Test
        public void testFind(){
            // 查询全部，并按照价格降序排序
            Iterable<Item> items = this.itemRepository.findAll(Sort.by(Sort.Direction.DESC, "price"));
            items.forEach(item-> System.out.println(item));
        }

        @Test
        public void findByMatch(){
            List<Item> list = this.itemRepository.findByMatch("坚果");
            list.forEach(item -> System.out.println(item.toString()));
        }

        @Test
        public void findByMultiMatch(){
            List<Item> list = this.itemRepository.findByMultiMatch("手机");
            list.forEach(item -> System.out.println(item));
        }

        @Test
        public void findByRange(){
            List<Item> list = this.itemRepository.findByRange("3000", "5000");
            list.forEach(item -> System.out.println(item));
        }



    }


四、进阶--聚合查询


     4.1）场景一： 单字段聚合
     /**
     * 查询条件：开盘时间起--现在excuteTime  messageType = 21 isSuccess = 1
     * 聚合条件：按messageId分组 求和
     */

     4.1.1）单字段聚合查询DSL语句
          GET /log_service_trade_indicator*/_search
               {
                 "query": {
                   "bool": {
                     "must": [
                       {
                         "match": {
                           "messageId": "21"
                         }
                       },
                       {
                         "match": {
                           "isSuccess": "1"
                         }
                       }
                     ],
                     "must_not": [],
                     "should": [],
                     "filter": [
                       {
                         "range": {
                           "excuteTime": {
                             "gte": 1636092301945
                           }
                         }
                       }
                     ]
                   }
                 },
                 "from": 0,
                 "size": 0,
                 "sort": [],
                 "aggs": {
                   "exchangeId_sum": {
                     "terms": {
                       "field": "exchangeId.keyword"
                     },
                     "aggs": {
                       "count_sum": {
                         "sum": {
                           "field": "count"
                         }
                       }
                     }
                   }
                 }
               }

     4.1.2）单字段聚合查询java
          public void entrustSumByExchangeId(){ ;
             NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
             //不查询任何结果
             queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""},null));
             //查询条件 excuteTime开盘时间起--现在  messageId = 21 isSuccess = 1
             BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
             boolQueryBuilder.must(QueryBuilders.matchQuery("messageId","21"));
             boolQueryBuilder.must(QueryBuilders.matchQuery("isSuccess","1"));
             boolQueryBuilder.filter(QueryBuilders.rangeQuery("excuteTime").gte(1636092301945l));
             queryBuilder.withQuery(boolQueryBuilder);

             /**
              *  聚合：按交易所分组 求和
              *      桶聚合类型为terms，名称为exchangeIds，字段为exchangeId.keyword。
              *      度量聚合类型为sum，名称exchangeIdSum，字段为count
              */
             queryBuilder.addAggregation(
                     AggregationBuilders.terms("exchangeIds").field("exchangeId.keyword")
                             .subAggregation(AggregationBuilders.sum("exchangeIdSum").field("count"))

             );

             //查询需要把结果强转为AggregatedPage类型
             AggregatedPage<TradeIndicatorResult> aggPage = (AggregatedPage<TradeIndicatorResult>) this.tradeIndicatorRepository.search(queryBuilder.build());

             // 结果解析
             ParsedStringTerms agg = (ParsedStringTerms) aggPage.getAggregation("exchangeIds");
             List<? extends Terms.Bucket> buckets = agg.getBuckets();
             buckets.forEach(bucket -> {
                 ExchangeOrderPerMinCountRespVo respVo = new ExchangeOrderPerMinCountRespVo();
                 ParsedSum exchangeIdSum = (ParsedSum)bucket.getAggregations().getAsMap().get("exchangeIdSum");
                 log.info("桶信息：exchangeId的key:{}, 文档数量doc_count:{}。度量信息：sum求和：{}" , bucket.getKeyAsString(), bucket.getDocCount(), exchangeIdSum.getValue());
             });

         }
      
      


    4.2）场景二：双字段分组聚合     
     /**
     * 查询条件：交易日开盘时间起--现在excuteTime
     * 聚合条件：按 messageType、isSuccess分组  求和
     */
     4.2.1）双字段分组聚合查询DSL
          GET /log_service_trade_indicator*/_search
               {
                 "query": {
                   "bool": {
                     "must": [],
                     "must_not": [],
                     "should": [],
                     "filter": [
                       {
                         "range": {
                           "excuteTime": {
                             "gte": 1636092301945
                           }
                         }
                       }
                     ]
                   }
                 },
                 "from": 0,
                 "size": 0,
                 "sort": [],
                 "aggs": {
                   "exchangeId_cate": {
                     "terms": {
                       "field": "messageId.keyword"
                     }, 
                     "aggs": {
                       "isSuccess_cate": {
                         "terms": {
                           "field": "isSuccess.keyword"
                         },
                         "aggs": {
                           "count_sum": {
                             "sum": {
                               "field": "count"
                             }
                           }
                         }
                       }
                     }
                   }
                 }
               }

     4.2.2）双字段分组聚合查询JAVA
         public void allKindBusinessIndicator(){
             NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
             //不查询任何结果
             queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{""},null));
             //查询条件 excuteTime开盘时间起--现在 
             BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
             boolQueryBuilder.filter(QueryBuilders.rangeQuery("excuteTime").gte(1636092301945l));
             queryBuilder.withQuery(boolQueryBuilder);

             /**
              * 聚合：按 messageType、isSuccess分组 求和
              * 桶聚合类型为terms，名称为messageIds，字段为messageId.keyword。
              *      子聚合类型为terms,名称为isSuccesses，字段为isSuccess.keyword。
              *          度量聚合类型为sum，名称countSum，字段为count
              */
             queryBuilder.addAggregation(
                     AggregationBuilders.terms("messageIds").field("messageId.keyword")
                             .subAggregation(AggregationBuilders.terms("isSuccesses").field("isSuccess.keyword")
                                     .subAggregation(AggregationBuilders.sum("countSum").field("count")))

             );

             //查询需要把结果强转为AggregatedPage类型
             AggregatedPage<TradeIndicatorResult> aggPage = (AggregatedPage<TradeIndicatorResult>) this.tradeIndicatorRepository.search(queryBuilder.build());

             // 结果解析
             ParsedStringTerms agg = (ParsedStringTerms) aggPage.getAggregation("messageIds");
             List<? extends Terms.Bucket> buckets = agg.getBuckets();
             buckets.forEach(bucket -> {
                 log.info("桶信息：messageIds的key:{}, 文档数量doc_count:{}。" , bucket.getKeyAsString(), bucket.getDocCount());
                 ParsedStringTerms subAgg = (ParsedStringTerms)bucket.getAggregations().getAsMap().get("isSuccesses");
                 List<? extends Terms.Bucket> subAggBuckets = subAgg.getBuckets();
                 subAggBuckets.forEach(subAggBucket->{
                     ParsedSum countSum = (ParsedSum)subAggBucket.getAggregations().getAsMap().get("countSum");
                     log.info("子桶信息：isSuccesses的key:{}, 文档数量doc_count:{}。度量信息：sum求和：{}" , subAggBucket.getKeyAsString(), subAggBucket.getDocCount(), countSum.getValue());
                 });
             });

         }
  

五、进阶--RestHighLevelClient查询


     5.1）DSL语句查询

     private RestResponse getDslRestResponse(Long projectId, Date startTime, Date endTime, String dslConditions) throws IOException {
        log.info("进入DSL条件查询");
        // 获取索引权限（索引权限、字段权限）
        Project project = this.getProject(projectId);

        /**
         *  1. 查询条件封装   a.排序和分页写在dsl中  b.权限字段写在dsl中  c.不支持scroll
         */
        // 配置查询的索引信息
        StringBuffer indexes = new StringBuffer();
        String projectIndex = project.getProjectIndex();
        Set<String> days = DateUtils.betweenDays(startTime, endTime);
        days.forEach(day -> {
            indexes.append(String.format("%s" + day, projectIndex)).append(",");
        });
        String idxes = indexes.substring(0, indexes.length() - 1);
        log.info("DSL生成索引为：{}",idxes);

        /**
         *  2. 查询
         */
        Request request = new Request("GET","/" +idxes+"/_search");
        request.setJsonEntity(dslConditions);
        Response response = restHighLevelClient.getLowLevelClient().performRequest(request);

        /**
         *  3. 结果解析,并封装返回
         */
        String respStr = EntityUtils.toString(response.getEntity());
        String outerHits = JSONObject.parseObject(respStr).getString("hits");
        String innerHits = JSONObject.parseObject(outerHits).getString("hits");
        List<HitsBeanMo> hitsBeanMos = JSONArray.parseArray(innerHits, HitsBeanMo.class);
        List<HitsBeanMo.SourceBean> list = hitsBeanMos.stream().map(f -> f.get_source()).collect(Collectors.toList());

        PageResponse<HitsBeanMo.SourceBean> pageResponse = new PageResponse<>();
        pageResponse.setList(list);
        return RestResponse.ok(pageResponse);
    }
      
     5.2）普通条件查询

        private RestResponse getConditionRestResponse(int pageSize, Long projectId, Date startTime, Date endTime, String ip, String logLevel, String scrollId, String moreConditions) throws IOException {
        moreConditions = URLDecoder.decode(moreConditions, "utf-8");
        log.info("进入普通条件查询");
        SearchResponse searchResponse;
        PageResponse<Map<String, Object>> pageResponse = new PageResponse<>();

        //第一次查询(不带scrollId)
        if (StringUtils.isBlank(scrollId)) {
            // 调用es --> 查询数据 默认是7天的
            if ((endTime.getTime() - startTime.getTime()) > sevenDay) {
                throw new RuntimeException("日志查询间隔不能超过7天");
            }

            // 获取索引权限（索引权限、字段权限）
            Project project = this.getProject(projectId);

            SearchRequest searchRequest = new SearchRequest();

            /**
             * 1. 查询条件封装
             */
            // 1.1 查询条件封装
            SearchSourceBuilder builder = new SearchSourceBuilder();
            BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
            boolQueryBuilder.filter(QueryBuilders.rangeQuery("eventTimestamp").gte(startTime.getTime()).lte(endTime.getTime()));
            if (StringUtils.isNotEmpty(ip)) {
                boolQueryBuilder.must(QueryBuilders.termQuery("hostName.keyword", ip));
            }
            if (StringUtils.isNotEmpty(logLevel)) {
                if (logLevel.contains("warn")) {
                    logLevel = logLevel.concat(",warning");
                }
                BoolQueryBuilder must_bool = QueryBuilders.boolQuery();
                String[] logLevels = logLevel.split(",");
                for (String level : logLevels) {
                    must_bool.should(QueryBuilders.termQuery("level", level));
                }
                boolQueryBuilder.must(must_bool);
            }
            // 模糊搜索条件封装，前端moreConditions传参值为：{"method":"FuturesServer","msg":"md5 HeartBeat"}，其中key为字段，value为字段值
            if (StringUtils.isNotEmpty(moreConditions)) {
                Map<String, String> map = JSONObject.parseObject(moreConditions, Map.class);
                map.forEach((key, value) -> {
                    boolQueryBuilder.must(QueryBuilders.matchQuery(key, value));
                });
            }
            builder.query(boolQueryBuilder);

            // 1.2. 排序分页
            builder.sort("eventTimestamp", SortOrder.DESC);
            // 分页查询已经取消 builder.from((pageNum - 1) * pageSize);
            builder.size(pageSize);

            // 1.3. 查询包含权限字段
            if (Objects.nonNull(project)) {
                List<String> properties = project.getChildren().stream().map(Project::getProperty).collect(Collectors.toList());
                String[] propertiesArray = properties.toArray(new String[properties.size()]);
                builder.fetchSource(propertiesArray, new String[]{});
            }

            // 1.4. 最终条件封装
            log.info("最终条件：{}", builder);
            searchRequest.source(builder);

            /**
             *  2. 配置查询的索引
             */
            StringBuffer indexes = new StringBuffer();
            String projectIndex = project.getProjectIndex();
            Set<String> days = DateUtils.betweenDays(startTime, endTime);
            days.forEach(day -> {
                indexes.append(String.format("%s" + day, projectIndex)).append(",");
            });
            log.info("添加搜索生成索引为：{}", indexes.substring(0, indexes.length() - 1));
            searchRequest.indices(indexes.substring(0, indexes.length() - 1));


            /**
             *  3. 配置scroll，并查询
             */
            searchRequest.scroll(scroll);
            searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

            // 返回增加隐藏和显示字段列表(后续增加的  暂时不用)
            //this.getHideAndShowProperties(pageResponse, project);

        } else {
            // 带scrollId查询（非第一次查询）
            SearchScrollRequest searchScrollRequest = new SearchScrollRequest(scrollId);
            searchScrollRequest.scroll(scroll);
            searchResponse = restHighLevelClient.scroll(searchScrollRequest, RequestOptions.DEFAULT);
        }

        /**
         *  4. 结果解析,并封装返回
         */
        String returnScrollId = searchResponse.getScrollId();
        SearchHits hits = searchResponse.getHits();
        ArrayList<Map<String, Object>> results = new ArrayList<>();
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            //增加id字段，以便前端可以做数据定位
            sourceAsMap.put("id", hit.getId());
            results.add(hit.getSourceAsMap());
        }


        pageResponse.setScrollId(returnScrollId);
        pageResponse.setTotal(hits.getTotalHits().value);
        pageResponse.setList(results);
        return RestResponse.ok(pageResponse);
    }


    5.3）Groovy脚本中，先DSL查询，在插入示例     


      
          import cn.hutool.extra.spring.SpringUtil
          import com.alibaba.fastjson.JSONArray
          import com.alibaba.fastjson.JSONObject
          import com.em.commonModule.analysisCenter.entity.ScriptResult
          import org.apache.commons.lang3.StringUtils
          import org.elasticsearch.action.index.IndexRequest
          import org.elasticsearch.action.index.IndexResponse
          import org.elasticsearch.client.Request
          import org.elasticsearch.client.RequestOptions
          import org.elasticsearch.client.RestHighLevelClient
          import org.elasticsearch.client.indices.GetIndexRequest
          import org.apache.http.util.EntityUtils
          import org.elasticsearch.client.Response
          import org.slf4j.Logger
          import org.slf4j.LoggerFactory

          import java.time.LocalDateTime
          import java.time.ZoneId
          import java.time.format.DateTimeFormatter

          /**
           *       1. 根据DSL语句查询；
           *       2. 查询结果解析
           *       3. 插入数据
           * @return
           */
          def getDslResult() {

              LocalDateTime now = LocalDateTime.now();
              String today = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
              long startTime = now.minusMinutes(3).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
              long endTime = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
              String idx = "log_service_cloudorder_conn_reuse_" + today;

          //    long startTime = 1655275408000l;
          //    long endTime = 1669927283420l;
          //    String idx = "log_service_backhand_conn_reuse_20220822";
              String dslConditions = "{\n" +
                      "  \"query\": {\n" +
                      "    \"bool\": {\n" +
                      "      \"must\": [\n" +
                      "        {\"match_phrase\": { \"msg\": \"ReqOrderInsertPact requestJson\"}}\n" +
                      "      ],\n" +
                      "      \"filter\": [\n" +
                      "        {\n" +
                      "          \"range\": {\n" +
                      "            \"eventTimestamp\": {\n" +
                      "               \"gte\":" + startTime + ",\n" +
                      "               \"lte\": " + endTime + "\n" +
                      "            }\n" +
                      "          }\n" +
                      "        }\n" +
                      "      ]\n" +
                      "    }\n" +
                      "  },\n" +
                      "  \"from\": 0,\n" +
                      "  \"size\": 10,\n" +
                      "  \"aggs\": {}\n" +
                      "}";

              // 1. 根据DSL语句查询
              RestHighLevelClient restHighLevelClient = SpringUtil.getBean(RestHighLevelClient);
              GetIndexRequest getIndexRequest = new GetIndexRequest(idx)
              boolean exists = restHighLevelClient.indices().exists(getIndexRequest, RequestOptions.DEFAULT);
              if (!exists) {
                  return null;
              }
              Request request = new Request("GET", "/" + idx + "/_search");
              request.setJsonEntity(dslConditions);
              return restHighLevelClient.getLowLevelClient().performRequest(request);
          }

          /**
           * 脚本执行入口
           *     2. 查询结果解析
           *     3. 插入数据
           * */
          def mainCall() {
              Logger logger = LoggerFactory.getLogger("groovy")
              try {

                  // 2. 查询结果解析
                  Response response = getDslResult();
                  if (response == null) {
                      return ScriptResult.warnWithData("索引为空");
                  }

                  List<HashMap<String, Object>> insertMapList = new ArrayList<>();
                  HashMap<String, Object> insertMap = new HashMap<>();
                  String respStr = EntityUtils.toString(response.getEntity());
                  JSONObject outerHits = JSONObject.parseObject(respStr).getJSONObject("hits");
                  String value = outerHits.getJSONObject("total").getString("value");
                  logger.info("查询结果条数：{}", value);
                  if (java.lang.Integer.valueOf(value) > 0) {
                      String innerHits = outerHits.getString("hits");
                      List<JSONObject> jsonObjects = JSONArray.parseArray(innerHits, JSONObject.class);
                      for (JSONObject jsonObj: jsonObjects){
                          JSONObject source = jsonObj.getJSONObject("_source");
                          if(StringUtils.isNotEmpty(source.getString("msg"))){
                              int startIndex = source.getString("msg").indexOf("{");
                              int endIndex = source.getString("msg").indexOf("}");
                              JSONObject orderMsgJson = JSONObject.parseObject(source.getString("msg").substring(startIndex, endIndex+1));
                              insertMap.put("instrumentID", orderMsgJson.getString("InstrumentID"));
                              insertMap.put("direction", orderMsgJson.getString("Direction"));
                              insertMap.put("combOffsetFlag", orderMsgJson.getString("CombOffsetFlag"));
                              insertMap.put("exchangeID", orderMsgJson.getString("ExchangeID"));
                              insertMap.put("investorID", orderMsgJson.getString("InvestorID"));
                              insertMap.put("volumeTotalOriginal", orderMsgJson.getString("VolumeTotalOriginal"));

                              insertMap.put("hostName", source.getString("hostName"));
                              insertMap.put("timestamp", source.getDate("timestamp"));
                              insertMap.put("eventTime", source.getDate("eventTime"));
                              insertMap.put("eventTimestamp", source.getLong("eventTimestamp"));
                              insertMapList.add(insertMap);
                          }
                      }

                      //3. 插入数据  BulkRequest它只支持JSON或SHAY源数据，所以对于HashMap的形式建议单条插入
                      if(insertMapList.size() > 0){
                          RestHighLevelClient restHighLevelClient = SpringUtil.getBean(RestHighLevelClient);
                          for(HashMap<String, Object> insert: insertMapList){
                              if(insert.size() > 0){

                                  IndexRequest req = new IndexRequest("grafana_data_conn_reuse_ccorder_order_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")),"_doc", UUID.randomUUID().toString()).source(insertMap);
                                  IndexResponse indexResponse = restHighLevelClient.index(req, RequestOptions.DEFAULT);
                                  logger.info("插入数据结果：{}", indexResponse.getResult());
                              }
                          }
                      }

                      logger.info("插入数据条数：{}", insertMapList.size());
                      return ScriptResult.okWithData("插入数据成功，条数：" + insertMapList.size());
                  } else {
                      return ScriptResult.okWithData("查询结果为空");
                  }

              }catch (Exception e) {
                  logger.error("插入数据失败："+e);
                  return ScriptResult.warn("插入数据失败，信息：" + (!StringUtils.equals(e.getMessage(), "null") && e.getMessage().length() >= 200 ? e.getMessage().substring(0, 200) : e.getMessage()));
              }
          }


          mainCall()
