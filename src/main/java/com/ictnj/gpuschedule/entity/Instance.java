package com.ictnj.gpuschedule.entity;

import java.util.List;

/**
 * @ClassName Instance
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/11 19:23
 **/
public class Instance {

    // 边界的宽
    private double W;
    // 边界的高
    private double H;
    // 矩形列表
    private List<Item> itemList;
    // 是否允许矩形旋转
    private boolean isRotateEnable;

    public double getW() {
        return W;
    }

    public void setW(double w) {
        W = w;
    }

    public double getH() {
        return H;
    }

    public void setH(double h) {
        H = h;
    }

    public List<Item> getItemList() {
        return itemList;
    }

    public void setItemList(List<Item> itemList) {
        this.itemList = itemList;
    }

    public boolean isRotateEnable() {
        return isRotateEnable;
    }

    public void setRotateEnable(boolean rotateEnable) {
        isRotateEnable = rotateEnable;
    }
}
