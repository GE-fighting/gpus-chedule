package com.ictnj.gpuschedule.bean;

import jdk.internal.dynalink.linker.LinkerServices;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName Host
 * @Description 物理机信息
 * @Author zyn
 * @Date 2023/3/10 16:24
 **/
@Data
public class Host {
    private int id;
    //物理机含有的GPU列表
    private List<GPU> gpus;

    private List<Task> assignedTasks;

    public Host() {
        this.assignedTasks = new ArrayList<Task>();
    }
}
