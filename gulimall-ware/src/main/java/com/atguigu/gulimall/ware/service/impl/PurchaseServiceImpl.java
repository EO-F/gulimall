package com.atguigu.gulimall.ware.service.impl;

import com.atguigu.common.constant.WareConstant;
import com.atguigu.common.utils.PageUtils;
import com.atguigu.common.utils.Query;
import com.atguigu.gulimall.ware.dao.PurchaseDao;
import com.atguigu.gulimall.ware.entity.PurchaseDetailEntity;
import com.atguigu.gulimall.ware.entity.PurchaseEntity;
import com.atguigu.gulimall.ware.service.PurchaseDetailService;
import com.atguigu.gulimall.ware.service.PurchaseService;
import com.atguigu.gulimall.ware.service.WareSkuService;
import com.atguigu.gulimall.ware.vo.MergeVo;
import com.atguigu.gulimall.ware.vo.PurchaseDoneVo;
import com.atguigu.gulimall.ware.vo.PurchaseItemDoneVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service("purchaseService")
public class PurchaseServiceImpl extends ServiceImpl<PurchaseDao, PurchaseEntity> implements PurchaseService {

    @Autowired
    PurchaseDetailService purchaseDetailService;

    @Autowired
    WareSkuService wareSkuService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public PageUtils queryPageUnreceive(Map<String, Object> params) {
        IPage<PurchaseEntity> page = this.page(
                new Query<PurchaseEntity>().getPage(params),
                new QueryWrapper<PurchaseEntity>().eq("status",0).or().eq("status",1)
        );

        return new PageUtils(page);
    }

    @Override
    public void mergePurchase(MergeVo mergeVo) {
        // 拿到采购单id
        Long purchaseId = mergeVo.getPurchaseId();
        // 采购单 id为空 新建
        if (purchaseId == null ) {
            PurchaseEntity purchaseEntity = new PurchaseEntity();
            // 状态设置为新建
            purchaseEntity.setStatus(WareConstant.PurchaseStatusEnum.CREATED.getCode());
            purchaseEntity.setCreateTime(new Date());
            purchaseEntity.setUpdateTime(new Date());
            this.save(purchaseEntity);
            // 拿到最新的采购单id
            purchaseId = purchaseEntity.getId();
        }
        //TODO 确认采购是 0 或 1 才可以合并

        // 拿到合并项 **采购需求的id**
        List<Long> items = mergeVo.getItems();
        Long finalPurchaseId = purchaseId;
        List<PurchaseDetailEntity> collect = items.stream().map(i -> {
            // 采购需求
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();

            // 通过采购单id 查询到 采购信息对象
            PurchaseEntity byId = this.getById(finalPurchaseId);
            // 状态如果是正在采购
            if (! (byId.getStatus() == WareConstant.PurchaseDetailStatusEnum.BUYING.getCode())) {
                // 设置为已分配
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode());
            }

            detailEntity.setId(i);
            // 设置采购单id
            detailEntity.setPurchaseId(finalPurchaseId);

            return detailEntity;
        }).collect(Collectors.toList());

        // id批量更新
        purchaseDetailService.updateBatchById(collect);

        // 再次合并的话 更新修改时间
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(purchaseId);
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);
    }

    @Override
    public void received(List<Long> ids) {
        // 1、确认当前采购单是 新建或者 已分配状态 才能进行采购
        List<PurchaseEntity> collect = ids.stream().map(id -> {
            // 根据采购id查询出采购信息
            PurchaseEntity byId = this.getById(id);
            return byId;
        }).filter(item -> {
            // 新建或者已分配留下
            if (item.getStatus() == WareConstant.PurchaseStatusEnum.CREATED.getCode() ||
                    item.getStatus() == WareConstant.PurchaseStatusEnum.ASSIGNED.getCode()) {
                return true;
            }
            return false;
        }).map(item -> {
            // 设置为已领取
            item.setStatus(WareConstant.PurchaseStatusEnum.RECEIVE.getCode());
            item.setUpdateTime(new Date());
            return item;
        }).collect(Collectors.toList());

        // 2、改变采购单状态
        this.updateBatchById(collect);


        // 3、改变采购项的状态
        collect.forEach((item) -> {
            // 根据 purchase_id 查询出采购需求
            List<PurchaseDetailEntity> entities = purchaseDetailService.listDetailByPurchaseId(item.getId());
            //
            List<PurchaseDetailEntity> detailEntites = entities.stream().map(entity -> {
                PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();

                detailEntity.setId(entity.getId());
                // 设置状态正在采购
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.BUYING.getCode());
                return detailEntity;
            }).collect(Collectors.toList());
            // id批量更新
            purchaseDetailService.updateBatchById(detailEntites);
        });
    }

    @Override
    public void done(PurchaseDoneVo doneVo) {
        // 采购单id
        Long id = doneVo.getId();

        // 2、改变采购项目的状态
        Boolean flag = true;
        List<PurchaseItemDoneVo> items = doneVo.getItems();
        List<PurchaseDetailEntity> updates = new ArrayList<>();
        for (PurchaseItemDoneVo item : items) {
            PurchaseDetailEntity detailEntity = new PurchaseDetailEntity();
            // 如果采购失败
            if (item.getStatus() == WareConstant.PurchaseDetailStatusEnum.HASERROR.getCode()) {
                flag = false;
                detailEntity.setStatus(item.getStatus());
            } else {
                // 3、将成功采购的进行入库
                PurchaseDetailEntity entity = purchaseDetailService.getById(item.getItemId());
                wareSkuService.addStock(entity.getSkuId(),entity.getWareId(),entity.getSkuNum());
                detailEntity.setStatus(WareConstant.PurchaseDetailStatusEnum.FINISH.getCode());
            }
            detailEntity.setId(item.getItemId());
            updates.add(detailEntity);
        }
        // 批量更新
        purchaseDetailService.updateBatchById(updates);

        // 1、改变采购单状态
        PurchaseEntity purchaseEntity = new PurchaseEntity();
        purchaseEntity.setId(id);
        // 设置状态根据变量判断
        purchaseEntity.setStatus(flag?WareConstant.PurchaseStatusEnum.FINISH.getCode():WareConstant.PurchaseStatusEnum.HASERROR.getCode());
        purchaseEntity.setUpdateTime(new Date());
        this.updateById(purchaseEntity);

    }


}