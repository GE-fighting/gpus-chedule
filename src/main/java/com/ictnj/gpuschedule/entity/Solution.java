package com.ictnj.gpuschedule.entity;

/**
 * @ClassName Solution
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/11 19:25
 **/

import java.util.List;

public class Solution {

    // 已放置矩形
    private List<PlaceItem> placeItemList;
    // 放置总面积
    private double totalS;
    // 利用率
    private double rate;

    // 构造函数
    public Solution(List<PlaceItem> placeItemList, double totalS, double rate) {
        this.placeItemList = placeItemList;
        this.totalS = totalS;
        this.rate = rate;
    }

    public List<PlaceItem> getPlaceItemList() {
        return placeItemList;
    }

    public void setPlaceItemList(List<PlaceItem> placeItemList) {
        this.placeItemList = placeItemList;
    }

    public double getTotalS() {
        return totalS;
    }

    public void setTotalS(double totalS) {
        this.totalS = totalS;
    }

    public double getRate() {
        return rate;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }
}

