package com.ictnj.gpuschedule.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @ClassName Gpu
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/15 22:33
 **/
@Data
public class Gpu {

    @TableId(type = IdType.AUTO)
    private int id;
    private int hostId;
    private int type;
    private int gpuIndex;

}
