package com.atguigu.gulimall.ware.vo;

import lombok.Data;

@Data
public class PurchaseItemDoneVo {

    /**
     * 完成/失败的需求详情
     */
    private Long itemId;
    /**
     * 状态
     */
    private Integer status;
    
    private String reason;
}