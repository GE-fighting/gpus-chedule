package com.ictnj.gpuschedule.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ictnj.gpuschedule.bean.GPU;
import com.ictnj.gpuschedule.bean.Host;
import com.ictnj.gpuschedule.bean.ResultData;
import com.ictnj.gpuschedule.bean.Task;
import com.ictnj.gpuschedule.dao.Gpu;
import com.ictnj.gpuschedule.dao.HostEntity;
import com.ictnj.gpuschedule.dao.Record;
import com.ictnj.gpuschedule.dao.TaskEntity;
import com.ictnj.gpuschedule.mapper.GpuMapper;
import com.ictnj.gpuschedule.mapper.HostMapper;
import com.ictnj.gpuschedule.mapper.RecordMapper;
import com.ictnj.gpuschedule.mapper.TaskEntityMapper;
import com.ictnj.gpuschedule.service.*;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.swing.plaf.synth.SynthOptionPaneUI;
import java.sql.Wrapper;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @ClassName ScheduleController
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/13 22:01
 **/
@RestController
@RequestMapping("schedule")
public class ScheduleController {
    @Autowired
    private RecordMapper recordMapper;
    @Autowired
    private HostMapper hostMapper;
    @Autowired
    private GpuMapper gpuMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private TaskEntityMapper taskEntityMapper;

    private static int time = 0;

    private List<Host> hosts = new ArrayList<>();


    @RequestMapping("/FCFS")
    public void scheduleFIFO() {
        //insertTask();
        getHostInfo();

        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();
        //queryWrapper.orderByDesc(TaskEntity::getUrgency)
        List<Task> tasks = taskEntityMapper.selectList(null).stream().map(taskEntity -> {
            Task task = new Task();
            task.setId(taskEntity.getId());
            task.setRunTime(taskEntity.getRunTime());
            task.setGpuNum(taskEntity.getGpuNum());
            task.setDeadLine(taskEntity.getDeadLine());
            task.setArriveTime(taskEntity.getArriveTime());
            return task;
        }).collect(Collectors.toList());
        FIFOService fifoService = new FIFOService(hosts, tasks);
        ResultData schedule = fifoService.getSchedule();
        HashMap<Integer, HashMap<Integer, List<Task>>> result = schedule.getResult();
        //记录任务的Q
        HashMap<Integer, Integer> resultQosRecord = new HashMap<>();
        //记录任务的等待时间
        HashMap<Integer, Double> resultWaitRecord = new HashMap<>();
        //任务延时记录
        HashMap<Integer, Integer> taskDelayRecord = new HashMap<>();
        Double waitTime = 0.0;
        int maxFinishTime = 0;
        for (int hostId : result.keySet()) {
            System.out.println("物理机Index-" + hostId);
            HashMap<Integer, List<Task>> gpuTaskList = result.get(hostId);
            for (int gpuId : gpuTaskList.keySet()) {
                System.out.println("GPU  Index - " + gpuId);
                List<Task> taskList = gpuTaskList.get(gpuId);
                System.out.println("Task NUm is -" + taskList.size());
                for (Task task : taskList) {
                    //System.out.println("任务编号是 - " + task.getId());
                    Record record = new Record();
                    record.setGpuId(gpuId);
                    record.setHostId(hostId);
                    record.setTaskId(task.getId());
                    record.setTaskStartTime(task.getStartTime());
                    record.setTaskArriveTime(task.getArriveTime());
                    record.setTaskDeadLine(task.getDeadLine());
                    record.setTaskFinishTime(task.getFinishTime());
                    record.setTaskGpuNum(task.getGpuNum());
                    record.setTaskRunTime(task.getRunTime());
                    recordMapper.insert(record);
                    if (task.getDeadLine() >= task.getFinishTime()) {
                        //    如果任务在截止时间前完成
                        resultQosRecord.put(task.getId(), 1);
                    } else {
                        taskDelayRecord.put(task.getId(), task.getFinishTime() - task.getDeadLine());

                    }
                    resultWaitRecord.put(task.getId(), task.getStartTime() - task.getArriveTime());
                    if (record.getTaskFinishTime() > maxFinishTime) {
                        maxFinishTime = record.getTaskFinishTime();
                    }
                }
            }
        }
        //   计算截止日期前的任务数量
        System.out.println("任务的总数量：" + tasks.size() + " 截止日期前完成的任务数量：" + resultQosRecord.keySet().size());
        //    任务的平均等待时间
        for (Integer taskId : resultWaitRecord.keySet()) {
            waitTime += resultWaitRecord.get(taskId);
        }
        System.out.println("调度的平均等待时间-" + waitTime / resultWaitRecord.keySet().size());
        System.out.println("任务完成的时间-" + maxFinishTime);
        //    任务平均时延
        int delayTime = 0;
        for (Integer taskId : taskDelayRecord.keySet()) {
            delayTime += taskDelayRecord.get(taskId);
        }
        System.out.println("任务总时延：" + delayTime+" 超时任务数：" + taskDelayRecord.keySet().size());
        System.out.println("任务平均时延-" + delayTime / taskDelayRecord.keySet().size());
    }


    /**
     * @return void
     * @Author zyn
     * @Description //执行DACO调度算法
     * @Date 22:11 2023/3/13
     * @Param []
     **/
    @RequestMapping("/DACO")
    public void scheduleDACO() {


        getHostInfo();

        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();

        List<Task> tasks = taskEntityMapper.selectList(null).stream().map(taskEntity -> {
            Task task = new Task();
            task.setId(taskEntity.getId());
            task.setRunTime(taskEntity.getRunTime());
            task.setGpuNum(taskEntity.getGpuNum());
            task.setDeadLine(taskEntity.getDeadLine());
            task.setArriveTime(taskEntity.getArriveTime());
            return task;
        }).collect(Collectors.toList());
        double[][] pheromone = new double[tasks.size()][hosts.size()];
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = 0; j < hosts.size(); j++) {
                pheromone[i][j] = 1.0;
            }
        }

        ScheduleServiceDACO scheduleService = new ScheduleServiceDACO(10, hosts.size(), tasks.size(), pheromone, tasks, hosts);
        ResultData schedule = scheduleService.getSchedule();
        HashMap<Integer, HashMap<Integer, List<Task>>> result = schedule.getResult();
        HashMap<Integer, Integer> resultQosRecord = new HashMap<>();
        //记录任务的等待时间
        HashMap<Integer, Double> resultWaitRecord = new HashMap<>();
        Double waitTime = 0.0;
        for (int hostId : result.keySet()) {
            System.out.println("物理机Index-" + hostId);
            HashMap<Integer, List<Task>> gpuTaskList = result.get(hostId);
            for (int gpuId : gpuTaskList.keySet()) {
                System.out.println("GPU  Index - " + gpuId);
                List<Task> taskList = gpuTaskList.get(gpuId);
                System.out.println("Task NUm is -" + taskList.size());
                for (Task task : taskList) {
                    //System.out.println("任务编号是 - " + task.getId());
                    Record record = new Record();
                    record.setGpuId(gpuId);
                    record.setHostId(hostId);
                    record.setTaskId(task.getId());
                    record.setTaskStartTime(task.getStartTime());
                    record.setTaskDeadLine(task.getDeadLine());
                    record.setTaskArriveTime(task.getArriveTime());
                    record.setTaskFinishTime(task.getFinishTime());
                    record.setTaskGpuNum(task.getGpuNum());
                    record.setTaskRunTime(task.getRunTime());
                    recordMapper.insert(record);
                    if (task.getDeadLine() > task.getFinishTime()) {
                        //    如果任务在截止时间前完成
                        resultQosRecord.put(task.getId(), 1);
                    }
                    resultWaitRecord.put(task.getId(), task.getStartTime() - task.getArriveTime());
                }
            }
        }
        //   计算截止日期前的任务数量
        System.out.println("任务的总数量：" + tasks.size() + " 截止日期前完成的任务数量：" + resultQosRecord.keySet().size());
        //    任务的平均等待时间
        for (Integer taskId : resultWaitRecord.keySet()) {
            waitTime += resultWaitRecord.get(taskId);
        }
        System.out.println("调度的平均等待时间-" + waitTime / resultWaitRecord.keySet().size());

    }

    /**
     * @return void
     * @Author zyn
     * @Description //执行DACO调度算法
     * @Date 22:11 2023/3/13
     * @Param []
     **/
    @RequestMapping("/DACO2")
    public void scheduleDACO2() {


        getHostInfo();

        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();

        List<Task> tasks = taskEntityMapper.selectList(queryWrapper.orderByDesc(TaskEntity::getUrgency)).stream().map(taskEntity -> {
            Task task = new Task();
            task.setId(taskEntity.getId());
            task.setRunTime(taskEntity.getRunTime());
            task.setGpuNum(taskEntity.getGpuNum());
            task.setDeadLine(taskEntity.getDeadLine());
            task.setArriveTime(taskEntity.getArriveTime());
            return task;
        }).collect(Collectors.toList());
        double[][] pheromone = new double[tasks.size()][hosts.size()];
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = 0; j < hosts.size(); j++) {
                pheromone[i][j] = 1.0;
            }
        }

        ScheduleServiceDACO2 scheduleService = new ScheduleServiceDACO2(50, hosts.size(), tasks.size(), pheromone, tasks, hosts);
        ResultData schedule = scheduleService.getSchedule();
        HashMap<Integer, HashMap<Integer, List<Task>>> result = schedule.getResult();
        HashMap<Integer, Integer> resultQosRecord = new HashMap<>();
        //记录任务的等待时间
        HashMap<Integer, Double> resultWaitRecord = new HashMap<>();
        //任务延时记录
        HashMap<Integer, Integer> taskDelayRecord = new HashMap<>();
        Double waitTime = 0.0;
        int maxFinishTime = 0;
        for (int hostId : result.keySet()) {
            System.out.println("物理机Index-" + hostId);
            HashMap<Integer, List<Task>> gpuTaskList = result.get(hostId);
            for (int gpuId : gpuTaskList.keySet()) {
                System.out.println("GPU  Index - " + gpuId);
                List<Task> taskList = gpuTaskList.get(gpuId);
                System.out.println("Task NUm is -" + taskList.size());
                for (Task task : taskList) {
                    //System.out.println("任务编号是 - " + task.getId());
                    Record record = new Record();
                    record.setGpuId(gpuId);
                    record.setHostId(hostId);
                    record.setTaskId(task.getId());
                    System.out.println("---------" + task.getStartTime());
                    record.setTaskStartTime(task.getStartTime());
                    record.setTaskDeadLine(task.getDeadLine());
                    record.setTaskArriveTime(task.getArriveTime());
                    record.setTaskFinishTime(task.getFinishTime());
                    record.setTaskGpuNum(task.getGpuNum());
                    record.setTaskRunTime(task.getRunTime());
                    recordMapper.insert(record);
                    if (task.getDeadLine() >= task.getFinishTime()) {
                        //    如果任务在截止时间前完成
                        resultQosRecord.put(task.getId(), 1);
                    } else {
                        taskDelayRecord.put(task.getId(), task.getFinishTime() - task.getDeadLine());
                    }
                    resultWaitRecord.put(task.getId(), task.getStartTime() - task.getArriveTime());

                    if (record.getTaskFinishTime() > maxFinishTime) {
                        maxFinishTime = record.getTaskFinishTime();
                    }
                }
            }
        }
        //   计算截止日期前的任务数量
        System.out.println("任务的总数量：" + tasks.size() + " 截止日期前完成的任务数量：" + resultQosRecord.keySet().size());
        //    任务的平均等待时间
        for (Integer taskId : resultWaitRecord.keySet()) {
            waitTime += resultWaitRecord.get(taskId);
        }
        System.out.println("调度的平均等待时间-" + waitTime / resultWaitRecord.keySet().size());
        System.out.println("任务完成的时间-" + maxFinishTime);
        //    任务平均时延
        int delayTime = 0;
        for (Integer taskId : taskDelayRecord.keySet()) {
            delayTime += taskDelayRecord.get(taskId);
        }
        System.out.println("任务总时延：" + delayTime+" 超时任务数：" + taskDelayRecord.keySet().size());
        System.out.println("任务平均时延-" + delayTime / taskDelayRecord.keySet().size());
    }


    @RequestMapping("/ACO")
    public void scheduleACO() {
        //insertTask();

        getHostInfo();

        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();

        List<Task> tasks = taskEntityMapper.selectList(queryWrapper.orderByDesc(TaskEntity::getUrgency)).stream().map(taskEntity -> {
            Task task = new Task();
            task.setId(taskEntity.getId());
            task.setRunTime(taskEntity.getRunTime());
            task.setGpuNum(taskEntity.getGpuNum());
            task.setDeadLine(taskEntity.getDeadLine());
            task.setArriveTime(taskEntity.getArriveTime());
            return task;
        }).collect(Collectors.toList());
        double[][] pheromone = new double[tasks.size()][hosts.size()];
        for (int i = 0; i < tasks.size(); i++) {
            for (int j = 0; j < hosts.size(); j++) {
                pheromone[i][j] = 1.0;
            }
        }

        ScheduleService scheduleService = new ScheduleService(10, hosts.size(), tasks.size(), pheromone, tasks, hosts);
        ResultData schedule = scheduleService.getSchedule();
        HashMap<Integer, HashMap<Integer, List<Task>>> result = schedule.getResult();
        HashMap<Integer, Integer> resultQosRecord = new HashMap<>();
        for (int hostId : result.keySet()) {
            System.out.println("物理机Index-" + hostId);
            HashMap<Integer, List<Task>> gpuTaskList = result.get(hostId);
            for (int gpuId : gpuTaskList.keySet()) {
                System.out.println("GPU  Index - " + gpuId);
                List<Task> taskList = gpuTaskList.get(gpuId);
                System.out.println("Task NUm is -" + taskList.size());
                for (Task task : taskList) {
                    Record record = new Record();
                    record.setGpuId(gpuId);
                    record.setHostId(hostId);
                    record.setTaskId(task.getId());
                    record.setTaskStartTime(task.getStartTime());
                    record.setTaskDeadLine(task.getDeadLine());
                    record.setTaskFinishTime(task.getFinishTime());
                    record.setTaskGpuNum(task.getGpuNum());
                    record.setTaskRunTime(task.getRunTime());
                    recordMapper.insert(record);
                    if (task.getDeadLine() > task.getFinishTime()) {
                        //    如果任务在截止时间前完成
                        resultQosRecord.put(task.getId(), 1);
                    }
                }
            }
        }

        //   计算截止日期前的任务数量
        System.out.println("任务的总数量：" + tasks.size() + " 截止日期前完成的任务数量：" + resultQosRecord.keySet().size());
    }

    /**
     * @return
     * @Author zhangjun
     * @Description //TODO 接收用户的调度任务
     * @Date 20:03 2023/3/15
     * @Param
     **/
    @PostMapping("/postTask")
    public void getScheduleTask(@RequestBody TaskEntity taskEntity) {

        int scheduleNum = 10;
        getHostInfo();
        //任务入库
        taskEntity.setStatus(0);
        taskEntityMapper.insert(taskEntity);
        //定义redis sorted set键名
        String redisKey = "scheduleKey";
        //将任务id存入redis有序集合，以到达的时间为score
        stringRedisTemplate.opsForZSet().add(redisKey, String.valueOf(taskEntity.getId()), time);

        //判断等待调度的队列中任务数
        Long size = stringRedisTemplate.opsForZSet().size(redisKey);
        double[][] pheromone = new double[scheduleNum][hosts.size()];
        for (int i = 0; i < scheduleNum; i++) {
            for (int j = 0; j < hosts.size(); j++) {
                pheromone[i][j] = 1.0;
            }
        }
        if (size == scheduleNum) {
            Set<String> range = stringRedisTemplate.opsForZSet().range(redisKey, 0, -1);
            List<Task> tasks = new ArrayList<>();
            for (String o : range) {
                Task task1 = new Task();
                TaskEntity taskEntity1 = taskEntityMapper.selectById(Integer.valueOf(o));
                task1.setId(taskEntity1.getId());
                task1.setRunTime(taskEntity1.getRunTime());
                task1.setDeadLine(taskEntity1.getDeadLine());
                task1.setGpuNum(taskEntity1.getGpuNum());
                tasks.add(task1);
                //    更新task的调度状态
                taskEntity1.setStatus(1);
                taskEntityMapper.updateById(taskEntity1);
            }
            ScheduleServiceDACO scheduleService = new ScheduleServiceDACO(10, hosts.size(), tasks.size(), pheromone, tasks, hosts);
            ResultData schedule = scheduleService.getSchedule();
            HashMap<Integer, HashMap<Integer, List<Task>>> result = schedule.getResult();
            for (int hostId : result.keySet()) {
                System.out.println("物理机Index-" + hostId);
                HashMap<Integer, List<Task>> gpuTaskList = result.get(hostId);
                for (int gpuId : gpuTaskList.keySet()) {
                    System.out.println("GPU  Index - " + gpuId);
                    List<Task> taskList = gpuTaskList.get(gpuId);
                    System.out.println("Task NUm is -" + taskList.size());
                    for (Task task : taskList) {
                        //System.out.println("任务编号是 - " + task.getId());
                        Record record = new Record();
                        record.setGpuId(gpuId);
                        record.setHostId(hostId);
                        record.setTaskId(task.getId());
                        record.setTaskStartTime(task.getStartTime());
                        record.setTaskDeadLine(task.getDeadLine());
                        record.setTaskFinishTime(task.getFinishTime());
                        record.setTaskGpuNum(task.getGpuNum());
                        record.setTaskRunTime(task.getRunTime());
                        recordMapper.insert(record);
                    }
                }
            }
        }


    }


    //从数据库里读取host信息
    public void getHostInfo() {
        if (hosts.size() == 0) {
            List<HostEntity> list = hostMapper.selectList(null);
            for (HostEntity hostEntity : list) {
                LambdaQueryWrapper<Gpu> queryWrapper = new LambdaQueryWrapper<Gpu>();
                List<Gpu> gpus = gpuMapper.selectList(queryWrapper.eq(Gpu::getHostId, hostEntity.getId()));
                Host host = new Host();
                host.setId(hostEntity.getId());
                List<GPU> assignedGpu = new ArrayList<>();
                for (Gpu gpu : gpus) {
                    GPU gpu1 = new GPU(gpu.getGpuIndex(), gpu.getHostId());
                    assignedGpu.add(gpu1);
                }
                host.setGpus(assignedGpu);
                hosts.add(host);
            }
        }
    }


    //随机插入任务
    @RequestMapping("/insertTask")
    public void insertTask(@RequestParam Integer taskNum) {
        //PoissonArrivalService poissonArrivalService = new PoissonArrivalService(150);
        ////随机产生任务存入数据库
        //double minArriveTime = 10000000.0;
        //double maxArriveTime = 0;
        //for (int i = 0; i < 50; i++) {
        //    double nextArrivalTime = poissonArrivalService.getNextArrivalTime();
        //    if (nextArrivalTime > maxArriveTime) {
        //        maxArriveTime = nextArrivalTime;
        //    }
        //    if (nextArrivalTime < minArriveTime) {
        //        minArriveTime = nextArrivalTime;
        //    }
        //    TaskEntity entity = new TaskEntity();
        //    entity.setArriveTime(nextArrivalTime);
        //    entity.setRunTime(new Random().nextInt(10) * 20 + new Random().nextInt(10) * 10 + new Random().nextInt(10));
        //    entity.setGpuNum(1 + new Random().nextInt(4));
        //    entity.setDeadLine((int) (entity.getRunTime() + entity.getArriveTime() + entity.getGpuNum() * 200));
        //    double urgency = entity.getRunTime() / (entity.getDeadLine() - entity.getArriveTime());
        //    entity.setUrgency(urgency);
        //    taskEntityMapper.insert(entity);
        //}
        GenerateTaskService generateTaskService = new GenerateTaskService(taskEntityMapper);
        for (int i = 1; i <= 7; i++) {
            if (i <= 5 && i >= 1) {
                generateTaskService.generateTask((int) (taskNum * 0.16), i);
            } else {
                generateTaskService.generateTask((int) (taskNum * 0.1), i);
            }

        }


        List<TaskEntity> taskEntities1 = taskEntityMapper.selectList(null);
        int sumRunTime = 0;
        for (TaskEntity taskEntity : taskEntities1) {
            LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();
            int aLong = Integer.valueOf(taskEntityMapper.selectCount(queryWrapper.le(TaskEntity::getArriveTime, taskEntity.getArriveTime())).toString());
            taskEntity.setDeadLine(taskEntity.getDeadLine() + aLong * 20);
            taskEntityMapper.updateById(taskEntity);
            sumRunTime += taskEntity.getRunTime();
        }
        System.out.println(sumRunTime / taskEntities1.size());
        //System.out.println("任务的边界" + minArriveTime + "-" + maxArriveTime);
    }


}
