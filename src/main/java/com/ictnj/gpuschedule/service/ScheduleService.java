//package com.ictnj.gpuschedule.service;
//
//import com.ictnj.gpuschedule.bean.FreeTime;
//import com.ictnj.gpuschedule.bean.GPU;
//import com.ictnj.gpuschedule.bean.Host;
//import com.ictnj.gpuschedule.bean.ResultData;
//import com.ictnj.gpuschedule.bean.Task;
//import com.ictnj.gpuschedule.entity.Instance;
//import com.ictnj.gpuschedule.entity.Item;
//import com.ictnj.gpuschedule.entity.PlaceItem;
//import com.ictnj.gpuschedule.entity.Solution;
//import com.ictnj.gpuschedule.model.heu.aco.ACO;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//
///**
// * @ClassName ScheduleService
// * @Description 调度服务
// * @Author zhangjun
// * @Date 2023/3/10 16:02
// **/
//public class ScheduleService {
//    private int numAnts; // 蚂蚁数量
//    private int numHosts; // 物理机数量
//    private int numTasks; // 任务数量
//    //private double[][] gpuStatus; // GPU状态矩阵，每行表示一个GPU的状态，包括当前任务的编号、完成时间、信息素浓度
//    private double[][] pheromone; // 信息素浓度矩阵，每行表示一个任务在所有host上的信息素浓度
//    private List<Host> hosts;
//    private List<Task> tasks; // 任务列表
//    private List<List<Integer>> antPaths; // 蚂蚁路径列表
//
//
//    private double alpha = 1.0; // alpha参数
//    private double beta = 5.0; // beta参数
//    private double rho = 0.1; //
//    private double Q = 0.5; // 信息素增量
//    private double initPheromone = 0.1; // 初始信息素浓度
//    private int maxIterations = 1;
//    private double w = 0.5;// 多目标函数中目标结果比例
//
//    private HashMap<Integer, HashMap<Integer, ResultData>> result = new HashMap<Integer, HashMap<Integer, ResultData>>();
//
//    public ScheduleService(int numAnts, int numHosts, int numTasks, double[][] pheromone, List<Task> tasks, List<Host> hosts) {
//        this.numAnts = numAnts;
//        this.numHosts = numHosts;
//        this.numTasks = numTasks;
//        this.pheromone = pheromone;
//        this.tasks = tasks;
//        this.hosts = hosts;
//    }
//
//    public ResultData getSchedule() {
//        //所有蚂蚁路径
//        HashMap<Integer, ArrayList<int[]>> pathMatrix_allAnt = new HashMap<Integer, ArrayList<int[]>>();
//        //所有蚂蚁的QOS
//        HashMap<Integer, Double> QOS = new HashMap<Integer, Double>();
//        for (int i = 0; i < maxIterations; i++) {
//            double maxFitness = 0;
//            int maxAntIndex = 0;
//            for (int antCount = 0; antCount < numAnts; antCount++) {
//                System.out.println("迭代数-" + i + "蚂蚁数" + antCount + "------------------------------------------");
//                //初始化蚂蚁分配路径
//                ArrayList<int[]> pathMatrix_oneAnt = initMatrix(numTasks, numHosts, 0);
//                //对任务进行遍历
//                for (int taskId = 0; taskId < numTasks; taskId++) {
//                    int j = assignOneTask(taskId);
//                    // 第antCount只蚂蚁的分配策略(pathMatrix[i][j]=1表示第antCount只蚂蚁将i任务分配给j节点处理)
//                    pathMatrix_oneAnt.get(taskId)[j] = 1;
//                }
//                // 将当前蚂蚁的路径加入pathMatrix_allAnt
//                pathMatrix_allAnt.put(antCount, pathMatrix_oneAnt);
//                //   利用结合天际线启发式的蚁群算法将物理机上的GPU卡合适分配给任务，并返回任务的QOS以及最短完成时间
//                double v = assignGpuToTask(i, antCount);
//                if (v > maxFitness) {
//                    maxFitness = v;
//                    maxAntIndex = antCount;
//                }
//                pathMatrix_allAnt.put(antCount, pathMatrix_oneAnt);
//            }
//            //更新信息素
//            updatePheromoneMatrix(pathMatrix_allAnt, maxAntIndex);
//        }
//        //TODO  找到最后一次迭代中多目标函数最大的蚂蚁路径
//        return getMaxFitnessPath(result.get(maxIterations - 1));
//
//    }
//
//    //将物理机上的GPU卡用天际线和蚁群算法分配给对应的任务，并计算这次该次蚂蚁实验的多目标函数值
//    public double assignGpuToTask(int itemCount, int antCount) {
//        //构建结果数据
//        HashMap<Integer, ResultData> antResultData = new HashMap<Integer, ResultData>();
//        ResultData data = new ResultData();
//        HashMap<Integer, List<Task>> hostTask = new HashMap<Integer, List<Task>>();
//        HashMap<Integer, HashMap<Integer, List<Task>>> resultGPU = new HashMap<>();
//        //本次蚂蚁实验中在deadline之前的完成的任务数
//        int deadlineNUm = 0;
//        //本次蚂蚁实验中，完成所有的任务的最小时间
//        int finishedAllMinTime = 0;
//        for (Host host : hosts) {
//            //拿到分配给该物理机的任务数据
//            List<Task> assignedTasks = host.getAssignedTasks();
//            //数据加进结果集中
//            hostTask.put(host.getId(), assignedTasks);
//            //二维填箱处理
//            Instance instance = new Instance();
//            instance.setH(1.0 * 10 * host.getGpus().size());
//            instance.setW(1.0 * 360);
//            List<Item> items = new ArrayList<>();
//            for (Task task : assignedTasks) {
//                Item item = new Item(String.valueOf(task.getId()), task.getRunTime(), task.getGpuNum() * 10);
//                items.add(item);
//            }
//            instance.setItemList(items);
//            instance.setRotateEnable(false);
//            // 实例化蚁群算法对象
//            ACO aco = new ACO(10, 5, 0.99, 5, 0.5, instance, null);
//            // 调用蚁群算法对象进行求解
//            Solution solution = aco.solve();
//            List<PlaceItem> placeItemList = solution.getPlaceItemList();
//            int finishedNumAfterDeadline = 0;
//            int maxFinishTime = 0;
//            // 记录一个物理机上的GPU的分配列表
//            HashMap<Integer, List<Task>> gpuTaskRecord = new HashMap<>();
//            for (GPU gpu : host.getGpus()) {
//                gpuTaskRecord.put(gpu.getId(), new ArrayList<Task>());
//            }
//            //根据图像处理得到GPU分配的任务列表
//            for (PlaceItem placeItem : placeItemList) {
//                double x = placeItem.getX();
//                double y = placeItem.getY();
//                double h = placeItem.getH();
//                double w = placeItem.getW();
//                Task task = new Task();
//                task.setId(Integer.valueOf(placeItem.getName()));
//                task.setStartTime((int) x);
//                task.setFinishTime((int) (x + w));
//                //如果任务的结束时间小于截止时间
//                for (Task task1 : host.getAssignedTasks()) {
//                    if (task1.getId() == Integer.valueOf(placeItem.getName())) {
//                        task.setDeadLine(task1.getDeadLine());
//                        task.setRunTime(task1.getRunTime());
//                        task.setGpuNum(task1.getGpuNum());
//                        if (task1.getDeadLine() > task.getFinishTime()) {
//                            finishedNumAfterDeadline++;
//                        }
//                    }
//                }
//                //找到该物理机完成所有任务的时间
//                if (task.getFinishTime() > maxFinishTime) {
//                    maxFinishTime = task.getFinishTime();
//                }
//                int gpuStartId = (int) (y / 10);
//                int num = (int) (h / 10);
//                for (int start = gpuStartId; start < gpuStartId + num; start++) {
//                    gpuTaskRecord.get(start).add(task);
//                }
//
//            }
//            //处理gpu任务调度的数据集
//            //HashMap<Integer, List<Task>> gpuTask = new HashMap<Integer, List<Task>>();
//            //for (GPU gpu : host.getGpus()) {
//            //    gpuTask.put(gpu.getId(), gpu.getAssignedTask());
//            //}
//            resultGPU.put(host.getId(), gpuTaskRecord);
//            //拿到该次蚂蚁实验中完成任务的更短时间
//            deadlineNUm += finishedNumAfterDeadline;
//            if (maxFinishTime > finishedAllMinTime) {
//                finishedAllMinTime = maxFinishTime;
//            }
//        }
//        //   计算此次蚂蚁的QOS和最短完成时间，得到多目标函数
//        double fitness = w * 360 / finishedAllMinTime + (1 - w) * (deadlineNUm / numTasks);
//        //将实验的目标函数值存入
//        data.setFitness(fitness);
//        data.setHostTask(hostTask);
//        data.setResult(resultGPU);
//        antResultData.put(antCount, data);
//        result.put(itemCount, antResultData);
//        return fitness;
//    }
//
//
//    //初始化一只蚂蚁的路径
//    public static ArrayList<int[]> initMatrix(int taskNum, int vmNum, int defaultNum) {
////		初始化一个蚂蚁路径
//        ArrayList<int[]> matrix = new ArrayList<int[]>();
//        for (int i = 0; i < taskNum; i++) {
//            // 分别计算任务i分配给所有节点的处理时间
//            int[] matrixOne = new int[vmNum];
//            for (int j = 0; j < vmNum; j++) {
//                matrixOne[j] = defaultNum;
//            }
//            matrix.add(matrixOne);
//        }
//        return matrix;
//    }
//
//    //给任务分配具体的物理机
//    public int assignOneTask(int taskId) {
//        //定义任务去这些物理机上的概率
//        double[] probabilities = new double[numHosts];
//        //可能性总和是0
//        double sum = 0.0;
//        //计算概率
//        for (int i = 0; i < numHosts; i++) {
//            probabilities[i] = computeProbability(taskId, i);
//            //所有GPU概率相加
//            sum += probabilities[i];
//        }
//        //轮盘赌返回要分配的GPU序号
//        double r = Math.random() * sum;
//        sum = 0.0;
//        for (int j = 0; j < numHosts; j++) {
//            sum += probabilities[j];
//            if (sum > r) {
//                // 任务分配给j物理机，更新物理机上工作负载
//                hosts.get(j).getAssignedTasks().add(tasks.get(taskId));
//                return j;
//            }
//        }
//        return -1;
//    }
//
//
//    // 计算任务taskId在物理机j上的分配概率
//    public double computeProbability(int taskId, int j) {
//        //1、拿到信息素
//        double p = this.pheromone[taskId][j];
//        //2、拿到启发式信息（a、物理机上的面积布局 b、任务的deadline）
//        double hostWorkLoad = getHostWorkLoad(j);
//        //3、计算概率并返回
//        double prob = Math.pow(p, alpha) * (1.0 / hostWorkLoad);
//        return prob;
//    }
//
//
//    // 更新信息素
//    public void updatePheromoneMatrix(HashMap<Integer, ArrayList<int[]>> pathMatrix_allAnt, int maxAntIndex) {
//        //    所有信息素均锐减p%
//        for (int i = 0; i < numTasks; i++) {
//            for (int j = 0; j < numHosts; j++) {
//                pheromone[i][j] *= 1 - rho;
//            }
//        }
//        //此次迭代目标函数最大的路径添加信息素
//        // 将本次迭代中最优路径的信息素增加q%
//        for (int taskIndex = 0; taskIndex < numTasks; taskIndex++) {
//            for (int hostIndex = 0; hostIndex < numHosts; hostIndex++) {
//                if (pathMatrix_allAnt.get(maxAntIndex).get(taskIndex)[hostIndex] == 1) {
//                    pheromone[taskIndex][hostIndex] *= 1 + Q;
//                }
//            }
//        }
//    }
//
//    //TODO 一次迭代中多目标函数最大的蚂蚁路径
//    public ResultData getMaxFitnessPath(HashMap<Integer, ResultData> result) {
//        int[] schedule = new int[numTasks];
//        double maxFitness = 0;
//        int maxIndex = 0;
//        for (Integer i : result.keySet()) {
//            double fitness = result.get(i).getFitness();
//            if (fitness > maxFitness) {
//                maxFitness = fitness;
//                maxIndex = i;
//            }
//        }
//        return result.get(maxIndex);
//    }
//
//
//    //    根据面积，计算物理机工作负载
//    public double getHostWorkLoad(int hostId) {
//        List<Task> assignedTasks = hosts.get(hostId).getAssignedTasks();
//        int taskWordLoad = 1;
//        if (assignedTasks != null) {
//            for (Task task : assignedTasks) {
//                taskWordLoad += task.getGpuNum() * task.getRunTime();
//
//            }
//        }
//        return 1.0 * taskWordLoad / (hosts.get(hostId).getGpus().size() * 6 * 60);
//    }
//
//}
package com.ictnj.gpuschedule.service;

import com.ictnj.gpuschedule.bean.GPU;
import com.ictnj.gpuschedule.bean.Host;
import com.ictnj.gpuschedule.bean.ResultData;
import com.ictnj.gpuschedule.bean.Task;
import com.ictnj.gpuschedule.entity.Instance;
import com.ictnj.gpuschedule.entity.Item;
import com.ictnj.gpuschedule.entity.PlaceItem;
import com.ictnj.gpuschedule.entity.Solution;
import com.ictnj.gpuschedule.model.heu.aco.ACO;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @ClassName ScheduleServiceDACO
 * @Description TODO
 * @Author zyn
 * @Date 2023/3/13 19:56
 **/
public class ScheduleService {
    private int numAnts; // 蚂蚁数量
    private int numHosts; // 物理机数量
    private int numTasks; // 任务数量
    //private double[][] gpuStatus; // GPU状态矩阵，每行表示一个GPU的状态，包括当前任务的编号、完成时间、信息素浓度
    private double[][] pheromone; // 信息素浓度矩阵，每行表示一个任务在所有host上的信息素浓度
    private List<Host> hosts;
    private List<Task> tasks; // 任务列表
    private List<List<Integer>> antPaths; // 蚂蚁路径列表


    private double alpha = 1.0; // alpha参数
    private double beta = 5.0; // beta参数
    private double rho = 0.1; //
    private double Q = 0.5; // 目标函数最大，全局信息素增量
    private double q = 0.25;// 任务在截止时间内完成，局部信息素增量
    private double initPheromone = 0.1; // 初始信息素浓度
    private int maxIterations = 10;
    private double w = 0.5;// 多目标函数中目标结果比例

    private  static int  timePeriod = 880;

    private HashMap<Integer, HashMap<Integer, ResultData>> result = new HashMap<Integer, HashMap<Integer, ResultData>>();

    public ScheduleService(int numAnts, int numHosts, int numTasks, double[][] pheromone, List<Task> tasks, List<Host> hosts) {
        this.numAnts = numAnts;
        this.numHosts = numHosts;
        this.numTasks = numTasks;
        this.pheromone = pheromone;
        this.tasks = tasks;
        this.hosts = hosts;
    }

    public ResultData getSchedule() {
        //所有蚂蚁路径
        HashMap<Integer, ArrayList<int[]>> pathMatrix_allAnt = new HashMap<Integer, ArrayList<int[]>>();
        //所有蚂蚁的QOS
        HashMap<Integer, Double> QOS = new HashMap<Integer, Double>();
        for (int i = 0; i < maxIterations; i++) {
            double maxFitness = 0;
            int maxAntIndex = 0;
            for (int antCount = 0; antCount < numAnts; antCount++) {
                System.out.println("迭代数-" + i + "蚂蚁数" + antCount + "------------------------------------------");
                //初始化蚂蚁分配路径
                ArrayList<int[]> pathMatrix_oneAnt = initMatrix(numTasks, numHosts, 0);
                //对任务进行遍历
                for (int taskId = 0; taskId < numTasks; taskId++) {
                    int j = assignOneTask(taskId);
                    // 第antCount只蚂蚁的分配策略(pathMatrix[i][j]=1表示第antCount只蚂蚁将i任务分配给j节点处理)

                    pathMatrix_oneAnt.get(taskId)[j] = 1;
                }
                // 将当前蚂蚁的路径加入pathMatrix_allAnt
                pathMatrix_allAnt.put(antCount, pathMatrix_oneAnt);
                //   利用结合天际线启发式的蚁群算法将物理机上的GPU卡合适分配给任务，并返回任务的QOS以及最短完成时间
                double v = assignGpuToTask(i, antCount);
                if (v > maxFitness) {
                    maxFitness = v;
                    maxAntIndex = antCount;
                }
                pathMatrix_allAnt.put(antCount, pathMatrix_oneAnt);
                clearHostAssignedTask();
            }
            //更新信息素
            updatePheromoneMatrix(pathMatrix_allAnt, maxAntIndex);
            //    每次迭代后 物理机上分配的任务清空
            //clearHostAssignedTask();
        }
        //TODO  找到最后一次迭代中多目标函数最大的蚂蚁路径
        return getMaxFitnessPath(result.get(maxIterations - 1));

    }

    //物理机上分配的任务清空
    public void clearHostAssignedTask() {
        for (Host host : hosts) {
            host.setAssignedTasks(new ArrayList<>());
        }
    }

    //将物理机上的GPU卡用天际线和蚁群算法分配给对应的任务，并计算这次该次蚂蚁实验的多目标函数值
    public double assignGpuToTask(int itemCount, int antCount) {
        //构建结果数据，Integer是指蚂蚁id
        HashMap<Integer, ResultData> antResultData = new HashMap<Integer, ResultData>();
        ResultData data = new ResultData();
        HashMap<Integer, List<Task>> hostTask = new HashMap<Integer, List<Task>>();
        HashMap<Integer, HashMap<Integer, List<Task>>> resultGPU = new HashMap<>();
        //本次蚂蚁实验中在deadline之前的完成的任务数
        int deadlineNUm = 0;
        //本次蚂蚁实验中，完成所有的任务的最小时间
        int finishedAllMinTime = 0;
        for (Host host : hosts) {
            //拿到分配给该物理机的任务数据
            List<Task> assignedTasks = host.getAssignedTasks();
            //数据加进结果集中
            hostTask.put(host.getId(), assignedTasks);
            //二维填箱处理
            Instance instance = new Instance();
            instance.setH(1.0 * 10 * host.getGpus().size());
            instance.setW(1.0 * timePeriod);
            List<Item> items = new ArrayList<>();
            for (Task task : assignedTasks) {
                Item item = new Item(String.valueOf(task.getId()), task.getRunTime(), task.getGpuNum() * 10);
                items.add(item);
            }
            instance.setItemList(items);
            instance.setRotateEnable(false);
            // 实例化蚁群算法对象
            ACO aco = new ACO(10, 10, 0.99, 5, 0.5, instance, null);
            // 调用蚁群算法对象进行求解
            Solution solution = aco.solve();
            List<PlaceItem> placeItemList = solution.getPlaceItemList();
            int finishedNumAfterDeadline = 0;
            int maxFinishTime = 0;
            // 记录一个物理机上的GPU的分配列表
            HashMap<Integer, List<Task>> gpuTaskRecord = new HashMap<>();
            for (GPU gpu : host.getGpus()) {
                gpuTaskRecord.put(gpu.getId(), new ArrayList<Task>());
            }
            //根据图像处理得到GPU分配的任务列表
            for (PlaceItem placeItem : placeItemList) {
                double x = placeItem.getX();
                double y = placeItem.getY();
                double h = placeItem.getH();
                double w = placeItem.getW();
                Task task = new Task();
                task.setId(Integer.valueOf(placeItem.getName()));
                task.setStartTime((int) x);
                task.setFinishTime((int) (x + w));
                //如果任务的结束时间小于截止时间
                for (Task task1 : assignedTasks) {
                    if (task1.getId() == task.getId()) {
                        task.setDeadLine(task1.getDeadLine());
                        task.setRunTime(task1.getRunTime());
                        task.setGpuNum(task1.getGpuNum());
                        if (task1.getDeadLine() > task.getFinishTime()) {
                            //局部信息浓度更新
                            pheromone[task1.getId() - 1][host.getId() - 1] *= 1 + q;
                            finishedNumAfterDeadline++;
                        }
                    }
                }
                //找到该物理机完成所有任务的时间
                if (task.getFinishTime() > maxFinishTime) {
                    maxFinishTime = task.getFinishTime();
                }
                int gpuStartId = (int) (y / 10);
                int num = (int) (h / 10);
                for (int start = gpuStartId; start < gpuStartId + num; start++) {
                    gpuTaskRecord.get(start).add(task);
                }

            }


            //处理gpu任务调度的数据集
            //HashMap<Integer, List<Task>> gpuTask = new HashMap<Integer, List<Task>>();
            //for (GPU gpu : host.getGpus()) {
            //    gpuTask.put(gpu.getId(), gpuTaskRecord.get(gpu.getId()));
            //}
            resultGPU.put(host.getId(), gpuTaskRecord);
            //拿到该次蚂蚁实验中完成任务的更短时间
            deadlineNUm += finishedNumAfterDeadline;
            if (maxFinishTime > finishedAllMinTime) {
                finishedAllMinTime = maxFinishTime;
            }
        }
        //   计算此次蚂蚁的QOS和最短完成时间，得到多目标函数
        double fitness = w * timePeriod / finishedAllMinTime + (1 - w) * (deadlineNUm / numTasks);
        System.out.println("实验完成的任务数----------------------------------------------------------------------------"+deadlineNUm);
        //将实验的目标函数值存入
        data.setFitness(fitness);
        data.setHostTask(hostTask);
        data.setResult(resultGPU);
        antResultData.put(antCount, data);
        result.put(itemCount, antResultData);
        return fitness;
    }


    //初始化一只蚂蚁的路径
    public static ArrayList<int[]> initMatrix(int taskNum, int vmNum, int defaultNum) {
//		初始化一个蚂蚁路径
        ArrayList<int[]> matrix = new ArrayList<int[]>();
        for (int i = 0; i < taskNum; i++) {
            // 分别计算任务i分配给所有节点的处理时间
            int[] matrixOne = new int[vmNum];
            for (int j = 0; j < vmNum; j++) {
                matrixOne[j] = defaultNum;
            }
            matrix.add(matrixOne);
        }
        return matrix;
    }

    //给任务分配具体的物理机
    public int assignOneTask(int taskId) {
        List<Integer> fitHostId = new ArrayList<>();
        for(int i = 0; i < numHosts; i++){
            if (hosts.get(i).getGpus().size()>=tasks.get(taskId).getGpuNum()){
                fitHostId.add(i);
            }
        }

        //定义任务去这些物理机上的概率
        double[] probabilities = new double[fitHostId.size()];
        //可能性总和是0
        double sum = 0.0;

        //计算概率
        for (int i = 0; i < fitHostId.size(); i++) {
            probabilities[i] = computeProbability(taskId, fitHostId.get(i));
            //所有GPU概率相加
            sum += probabilities[i];
        }
        //轮盘赌返回要分配的G
        double r = Math.random() * sum;
        sum = 0.0;
        for (int j = 0; j < fitHostId.size(); j++) {
            sum += probabilities[j];
            if (sum > r) {
                // 任务分配给j物理机，更新物理机上工作负载
                hosts.get(fitHostId.get(j)).getAssignedTasks().add(tasks.get(taskId));
                return fitHostId.get(j);
            }
        }
        System.out.println("taskId-" + taskId);
        return -1;
    }


    // 计算任务taskId在物理机j上的分配概率
    public double computeProbability(int taskId, int j) {
        //1、拿到信息素
        double p = this.pheromone[taskId][j];
        //2、拿到启发式信息（a、物理机上的面积布局 b、任务的deadline）
        double hostWorkLoad = getHostWorkLoad(j);
        //3、计算概率并返回
        double prob = Math.pow(p, alpha) * (1.0 / hostWorkLoad);
        return prob;
    }


    // 更新信息素
    public void updatePheromoneMatrix(HashMap<Integer, ArrayList<int[]>> pathMatrix_allAnt, int maxAntIndex) {
        //    所有信息素均锐减p%
        for (int i = 0; i < numTasks; i++) {
            for (int j = 0; j < numHosts; j++) {
                pheromone[i][j] *= 1 - rho;
            }
        }
        //此次迭代目标函数最大的路径添加信息素
        // 将本次迭代中最优路径的信息素增加q%
        for (int taskIndex = 0; taskIndex < numTasks; taskIndex++) {
            for (int hostIndex = 0; hostIndex < numHosts; hostIndex++) {
                if (pathMatrix_allAnt.get(maxAntIndex).get(taskIndex)[hostIndex] == 1) {
                    pheromone[taskIndex][hostIndex] *= 1 + Q;
                }
            }
        }
    }

    //TODO 一次迭代中多目标函数最大的蚂蚁路径
    public ResultData getMaxFitnessPath(HashMap<Integer, ResultData> result) {
        int[] schedule = new int[numTasks];
        double maxFitness = 0;
        int maxIndex = 0;
        for (Integer i : result.keySet()) {
            double fitness = result.get(i).getFitness();
            if (fitness > maxFitness) {
                maxFitness = fitness;
                maxIndex = i;
            }
        }
        return result.get(maxIndex);
    }


    //    根据面积，计算物理机工作负载
    public double getHostWorkLoad(int hostId) {
        List<Task> assignedTasks = hosts.get(hostId).getAssignedTasks();
        int taskWordLoad = 1;
        if (assignedTasks != null) {
            for (Task task : assignedTasks) {
                taskWordLoad += task.getGpuNum() * task.getRunTime();

            }
        }
        return 1.0 * taskWordLoad / (hosts.get(hostId).getGpus().size() * 6 * 60);
    }

}
