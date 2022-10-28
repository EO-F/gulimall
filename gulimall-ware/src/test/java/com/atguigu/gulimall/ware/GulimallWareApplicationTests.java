package com.atguigu.gulimall.ware;

import com.atguigu.gulimall.ware.service.WareSkuService;
import org.checkerframework.checker.units.qual.A;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GulimallWareApplicationTests {

    @Autowired
    WareSkuService wareSkuService;

    @Test
    void contextLoads() {
        System.out.println(wareSkuService.count());
    }

}
