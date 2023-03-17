package com.ictnj.gpuschedule.bean;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName GPU
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/9 16:23
 **/
@Data
public class GPU {
    private int id;
    private int hostId;
    //1-已调度  0-未调度
    private int status;

    private List<Task> assignedTask;

    public GPU(int id, int hostId) {
        this.id = id;
        this.hostId = hostId;
        this.assignedTask = new ArrayList<Task>();
    }
}
