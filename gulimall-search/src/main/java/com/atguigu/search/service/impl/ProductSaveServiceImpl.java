package com.atguigu.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.common.to.es.SkuEsModel;
import com.atguigu.search.config.GulimallEsConfig;
import com.atguigu.search.constant.EsConstant;
import com.atguigu.search.service.ProductSaveService;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service("productSaveService")
@Slf4j
public class ProductSaveServiceImpl implements ProductSaveService {

    @Autowired
    RestHighLevelClient restHighLevelClient;

    @Override
    public boolean productStatusUp(List<SkuEsModel> skuEsModelList) throws IOException {

        BulkRequest bulkRequest = new BulkRequest();
        for(SkuEsModel model : skuEsModelList){
            //1.构造请求，指定es索引
            IndexRequest indexRequest = new IndexRequest(EsConstant.PRODUCT_INDEX);
            //指定id
            indexRequest.id(model.getSkuId().toString());
            //将对象esmodel转换为json进行存储
            String s = JSON.toJSONString(model);
            indexRequest.source(s, XContentType.JSON);
            bulkRequest.add(indexRequest);
        }
        BulkResponse bulk = restHighLevelClient.bulk(bulkRequest, GulimallEsConfig.COMMON_OPTIONS);
        // TODO 1、 如果批量错误
        boolean b = bulk.hasFailures();
        List<String> collect = Arrays.stream(bulk.getItems()).map(item -> {
            return item.getId();
        }).collect(Collectors.toList());
        log.error("商品上架完成：{}",collect);
        return b;
    }
}
