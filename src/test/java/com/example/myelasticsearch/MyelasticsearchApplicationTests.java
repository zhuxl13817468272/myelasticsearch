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
    public void findByName(){
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
