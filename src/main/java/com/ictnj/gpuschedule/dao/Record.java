package com.ictnj.gpuschedule.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @ClassName Record
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/13 21:46
 **/
@Data
public class Record {
    @TableId(type = IdType.AUTO)
    private Integer id;
    private Integer hostId;
    private Integer gpuId;
    private Integer gpuType;
    private Integer taskId;
    private Integer taskStartTime;
    private double taskArriveTime;
    private Integer taskFinishTime;
    private Integer taskRunTime;
    private Integer taskDeadLine;
    private Integer taskGpuNum;


}
