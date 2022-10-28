package com.atguigu.search;

import org.elasticsearch.client.RestHighLevelClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

@SpringBootTest
class GulimallSearchApplicationTests {

    @Autowired
    RestHighLevelClient client;

    @Test
    void contextLoads() {
        System.out.println(client);
    }

    @Test
    void testCreateHotelIndex() throws IOException {
        //创建request，hotel为索引
//        CreateIndexRequest request = new CreateIndexRequest("hotel");
//        request.source(xxx, XContentType.JSON);
//        client.indices().create(request, RequestOptions.DEFAULT);
    }

}
