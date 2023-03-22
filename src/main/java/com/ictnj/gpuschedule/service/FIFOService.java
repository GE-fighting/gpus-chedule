package com.ictnj.gpuschedule.service;

import com.ictnj.gpuschedule.bean.GPU;
import com.ictnj.gpuschedule.bean.Host;
import com.ictnj.gpuschedule.bean.ResultData;
import com.ictnj.gpuschedule.bean.Task;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName FIFOService
 * @Description 先进先出算法
 * @Author zyn
 * @Date 2023/3/17 20:59
 **/
@Data
public class FIFOService {

    private int numHosts; // 物理机数量
    private int numTasks; // 任务数量
    private List<Host> hosts;
    private List<Task> tasks; // 任务列表
    //记录GPU卡结束工作的时间,<1,<1,0>>的
    private HashMap<Integer, HashMap<Integer, Integer>> gpuStartTime;

    HashMap<Integer, HashMap<Integer, List<Task>>> result = new HashMap<>();//GPU卡上任务安排
    private HashMap<Integer, Integer> taskIdMapIndex = new HashMap<>();

    public FIFOService(List<Host> hosts, List<Task> tasks) {
        this.numHosts = hosts.size();
        this.numTasks = tasks.size();
        this.hosts = hosts;
        this.tasks = tasks;
        //初始化GPU卡结束工作的时间
        HashMap<Integer, HashMap<Integer, Integer>> gpuStartTime = new HashMap<>();
        for (Host host : hosts) {

            HashMap<Integer, Integer> map = new HashMap<>();
            for (GPU gpu : host.getGpus()) {
                map.put(gpu.getId(), 0);
            }
            gpuStartTime.put(host.getId(), map);
            HashMap<Integer, List<Task>> gpuTask = new HashMap<>();
            for (GPU gpu : host.getGpus()) {
                gpuTask.put(gpu.getId(), new ArrayList<>());
            }
            result.put(host.getId(), gpuTask);


        }
        this.gpuStartTime = gpuStartTime;
        //    初始化任务id到任务的map
        for (int i = 0; i < tasks.size(); i++) {
            taskIdMapIndex.put(tasks.get(i).getId(),i);
        }


    }


    public ResultData getSchedule() {
        ResultData data = new ResultData();
        for (Task task : tasks) {
            int minId = 1;
            int minTime = Integer.MAX_VALUE;

            for (Host host : hosts) {
                if (host.getGpus().size() >= task.getGpuNum()) {
                    int earlyTime = getEarlyTime(task.getId(), host.getId());
                    if (earlyTime < minTime) {
                        minTime = earlyTime;
                        minId = host.getId();
                    }
                }
            }
            assignTaskToHost(task.getId(), minId);
        }
        data.setResult(result);
        return data;
    }

    //拿到任务给某虚拟机后的任务的执行情况
    public int getEarlyTime(int taskId, int hostId) {
        //先找到物理机但当前所以GPU卡的结束任务时间
        HashMap<Integer, Integer> timeMap = gpuStartTime.get(hostId);
        //把GPU卡按结束时间排序，最早完成的在前
        ArrayList<Map.Entry<Integer, Integer>> entryArrayList = new ArrayList<>(timeMap.entrySet());
        Collections.sort(entryArrayList, (o1, o2) -> (o1.getValue().compareTo(o2.getValue())));
        //拿到任务所需的GPU卡数m，选取结束任务时间排序后的前M张卡，并选第M张卡为开始时间，更新任务的开始时间和结束时间
        Task task = tasks.get(taskIdMapIndex.get(taskId));
        Integer startTime = entryArrayList.get(task.getGpuNum() - 1).getValue();
        Integer finishTime = task.getRunTime() + startTime;
        task.setStartTime(startTime);
        task.setFinishTime(finishTime);
        return finishTime;
    }

    public void assignTaskToHost(int taskId, int hostId) {
        //先找到物理机但当前所以GPU卡的结束任务时间
        HashMap<Integer, Integer> timeMap = gpuStartTime.get(hostId);
        //把GPU卡按结束时间排序，最早完成的在前
        ArrayList<Map.Entry<Integer, Integer>> entryArrayList = new ArrayList<>(timeMap.entrySet());
        Collections.sort(entryArrayList, (o1, o2) -> (o1.getValue().compareTo(o2.getValue())));
        //拿到任务所需的GPU卡数m，选取结束任务时间排序后的前M张卡，并选第M张卡为开始时间，更新任务的开始时间和结束时间
        Task task = tasks.get(taskIdMapIndex.get(taskId));
        Integer startTime = entryArrayList.get(task.getGpuNum() - 1).getValue();
        Integer finishTime = task.getRunTime() + startTime;
        task.setStartTime(startTime);
        task.setFinishTime(finishTime);
        //更新选取的卡上的任务列表和结束任务的最新时间
        for (int i = 0; i < task.getGpuNum(); i++) {
            Map.Entry<Integer, Integer> entry = entryArrayList.get(i);
            //    更新gpu卡上的任务
            result.get(hostId).get(entry.getKey()).add(task);
            //    更新gpu上卡结束任务的最新时间
            gpuStartTime.get(hostId).put(entry.getKey(), finishTime);
        }
    }
}
