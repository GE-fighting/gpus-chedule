package com.ictnj.gpuschedule.service;

import com.ictnj.gpuschedule.dao.TaskEntity;
import com.ictnj.gpuschedule.mapper.TaskEntityMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * @ClassName GenerateTask
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/22 16:55
 **/
@Data
public class GenerateTaskService {


    private TaskEntityMapper taskEntityMapper;


    public int getPoisson(double mean, int lowerBound, int upperBound) {
        Random RANDOM = new Random();
        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        do {
            p = p * RANDOM.nextDouble();
            k++;
        } while (p > L && (lowerBound + k - 1) < upperBound);
        return Math.min(lowerBound + k - 1, upperBound);
    }

    public GenerateTaskService(TaskEntityMapper taskEntityMapper) {
        this.taskEntityMapper = taskEntityMapper;
    }

    public void generateTask(int num, int day) {
        List<TaskEntity> taskEntities = new ArrayList<>();
        for (int i = 0; i < num; i++) {
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setArriveTime((double) getPoisson(720 + (day - 1) * 1440, (day - 1) * 1440, day * 1440));
            taskEntity.setGpuNum(1 + new Random().nextInt(4));
            int i1 = (int) (num * 0.6);
            int i2 = (int) (num * 0.8);
            int i3 = (int) (num * 0.95);
            if (i >= 0 && i < i1) {
                taskEntity.setRunTime(getPoisson(200, 60, 300));
            }
            if (i >= i1 && i < i2) {
                taskEntity.setRunTime(getPoisson(300, 300, 560));
            }
            if (i >= i2 && i < i3) {
                taskEntity.setRunTime(getPoisson(400, 720, 1300));
            }
            if (i >= i3 && i < num) {
                taskEntity.setRunTime(getPoisson(500, 1440, 3000));
            }

            taskEntities.add(taskEntity);

        }
        for (TaskEntity taskEntity : taskEntities) {
            taskEntity.setDeadLine((int) (taskEntity.getRunTime()+ taskEntity.getGpuNum() * 200));
            double urgency = taskEntity.getRunTime() / (taskEntity.getDeadLine() - taskEntity.getArriveTime());
            taskEntity.setUrgency(urgency);
            taskEntityMapper.insert(taskEntity);
        }

    }


}
