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
import com.ictnj.gpuschedule.service.FIFOService;
import com.ictnj.gpuschedule.service.PoissonArrivalService;
import com.ictnj.gpuschedule.service.ScheduleServiceDACO;
import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
 * @Author zhangjun
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


    @RequestMapping("/FIFO")
    public void sheduleFIFO() {
        List<TaskEntity> taskEntities = taskEntityMapper.selectList(null);
        if (taskEntities == null) {
            PoissonArrivalService poissonArrivalService = new PoissonArrivalService(50);
            //随机产生任务存入数据库
            for (int i = 0; i < 50; i++) {
                double nextArrivalTime = poissonArrivalService.getNextArrivalTime();
                TaskEntity entity = new TaskEntity();
                entity.setArriveTime(nextArrivalTime);
                entity.setRunTime(new Random().nextInt(10) * 10 + 5);
                entity.setDeadLine(5 * i + new Random().nextInt(50));
                entity.setGpuNum(1 + new Random().nextInt(4));
                taskEntityMapper.insert(entity);
            }
        }
        getHostInfo();

        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();

        List<Task> tasks = taskEntityMapper.selectList(queryWrapper.orderByAsc(TaskEntity::getArriveTime)).stream().map(taskEntity -> {
            Task task = new Task();
            task.setId(taskEntity.getId());
            task.setRunTime(taskEntity.getRunTime());
            task.setGpuNum(taskEntity.getGpuNum());
            task.setDeadLine(taskEntity.getDeadLine());
            return task;
        }).collect(Collectors.toList());
        FIFOService fifoService = new FIFOService(hosts, tasks);
        ResultData schedule = fifoService.getSchedule();
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


    /**
     * @return void
     * @Author zhangjun
     * @Description //执行DACO调度算法
     * @Date 22:11 2023/3/13
     * @Param []
     **/
    @RequestMapping("/DACO")
    public void sheduleDACO() {
        List<TaskEntity> taskEntities = taskEntityMapper.selectList(null);
        if (taskEntities == null) {
            PoissonArrivalService poissonArrivalService = new PoissonArrivalService(50);
            //随机产生任务存入数据库
            for (int i = 0; i < 50; i++) {
                double nextArrivalTime = poissonArrivalService.getNextArrivalTime();
                TaskEntity entity = new TaskEntity();
                entity.setArriveTime(nextArrivalTime);
                entity.setRunTime(new Random().nextInt(10) * 10 + 5);
                entity.setDeadLine(5 * i + new Random().nextInt(50));
                entity.setGpuNum(1 + new Random().nextInt(4));
                taskEntityMapper.insert(entity);
            }
        }

        getHostInfo();

        LambdaQueryWrapper<TaskEntity> queryWrapper = new LambdaQueryWrapper<TaskEntity>();

        List<Task> tasks = taskEntityMapper.selectList(queryWrapper.orderByAsc(TaskEntity::getArriveTime)).stream().map(taskEntity -> {
            Task task = new Task();
            task.setId(taskEntity.getId());
            task.setRunTime(taskEntity.getRunTime());
            task.setGpuNum(taskEntity.getGpuNum());
            task.setDeadLine(taskEntity.getDeadLine());
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

}
