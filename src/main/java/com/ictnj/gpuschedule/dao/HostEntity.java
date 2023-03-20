package com.ictnj.gpuschedule.dao;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * @ClassName Host
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/15 22:35
 **/
@Data
public class HostEntity {
    @TableId(type = IdType.AUTO)
    private int id;
    private int gpuType;
    private int gpuNum;

}
