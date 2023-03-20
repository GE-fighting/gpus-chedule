package com.ictnj.gpuschedule.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @ClassName TaskEntity
 * @Description 用户提交任务实体类
 * @Author zyn
 * @Date 2023/3/15 20:57
 **/
@Data
public class TaskEntity {
    @TableId(type = IdType.AUTO)
    private int id;
    private String taskName;
    private String taskDesc;
    private Double arriveTime;
    private int startTime;
    private int finishTime;
    private int runTime;
    private int deadLine;
    private int gpuNum;
    private int userId;
    private int status;
}
