package com.ictnj.gpuschedule.bean;

import lombok.Data;

/**
 * @ClassName FreeTime
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/10 15:36
 **/
@Data
public class FreeTime {
    private int startTime;
    private int finishTime;
    private int duration;
}
