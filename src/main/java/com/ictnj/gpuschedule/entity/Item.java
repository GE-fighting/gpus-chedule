package com.ictnj.gpuschedule.entity;

/**
 * @ClassName Item
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/11 19:23
 **/

import java.util.ArrayList;
import java.util.List;


public class Item {

    // 名字
    private String name;
    // 宽
    private double w;
    // 高
    private double h;

    // 构造函数
    public Item(String name, double w, double h) {
        this.name = name;
        this.w = w;
        this.h = h;
    }

    // 复制单个Item
    public static Item copy(Item item) {
        return new Item(item.name, item.w, item.h);
    }

    // 复制Item数组
    public static Item[] copy(Item[] items) {
        Item[] newItems = new Item[items.length];
        for (int i = 0; i < items.length; i++) {
            newItems[i] = copy(items[i]);
        }
        return newItems;
    }

    // 复制Item列表
    public static List<Item> copy(List<Item> items) {
        List<Item> newItems = new ArrayList<>();
        for (Item item : items) {
            newItems.add(copy(item));
        }
        return newItems;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getH() {
        return h;
    }

    public void setH(double h) {
        this.h = h;
    }
}
