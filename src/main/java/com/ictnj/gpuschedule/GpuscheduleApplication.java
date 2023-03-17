package com.ictnj.gpuschedule;

import com.ictnj.gpuschedule.bean.GPU;
import com.ictnj.gpuschedule.bean.Host;
import com.ictnj.gpuschedule.bean.ResultData;
import com.ictnj.gpuschedule.bean.Task;
import com.ictnj.gpuschedule.mapper.RecordMapper;
import com.ictnj.gpuschedule.service.PoissonArrivalService;
import com.ictnj.gpuschedule.service.ScheduleService;
import com.ictnj.gpuschedule.service.ScheduleServiceDACO;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootApplication
@MapperScan("com.ictnj.gpuschedule.mapper")
public class GpuscheduleApplication {


    public static void main(String[] args) {
        SpringApplication.run(GpuscheduleApplication.class, args);


    }


}
