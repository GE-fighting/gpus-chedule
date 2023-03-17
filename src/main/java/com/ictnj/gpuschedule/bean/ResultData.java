package com.ictnj.gpuschedule.bean;

import lombok.Data;

import java.util.HashMap;
import java.util.List;

/**
 * @ClassName ResultData
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/12 17:53
 **/
@Data
public class ResultData {
    private HashMap<Integer, List<Task>> hostTask;
    //第一个Integer是hostId,第二个是GPUId
    private HashMap<Integer, HashMap<Integer, List<Task>>> result;
    private double fitness;
}
