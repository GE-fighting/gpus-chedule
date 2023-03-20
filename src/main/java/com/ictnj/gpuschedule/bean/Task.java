package com.ictnj.gpuschedule.bean;

import lombok.Data;

import java.util.List;

/**
 * @ClassName Task
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/9 14:16
 **/
@Data
public class Task {
    private int id;
    private Double arriveTime;
    private int startTime;
    private int finishTime;
    private int deadLine;
    private int gpuNum;
    private List<GPU> assignedGpus;
    private int hostId;
    private int runTime;
    private int status;
    //GPU类型
    private int gpuType;

    //Todo 调用机器学习接口，传入参数，获取任务的预测时长
    //public int getTaskRunTime() {
    //    return 0;
    //}


}
