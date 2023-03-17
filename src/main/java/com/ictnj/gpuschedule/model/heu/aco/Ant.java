package com.ictnj.gpuschedule.model.heu.aco;

/**
 * @ClassName Ant
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/11 19:37
 **/

import com.ictnj.gpuschedule.entity.Item;
import com.ictnj.gpuschedule.entity.Solution;
import com.ictnj.gpuschedule.model.skyline.SkyLinePacking;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Ant {
    // 矩形集合
    private Item[] items;
    // 已经放置的矩形的索引
    private List<Integer> sequence;
    // 还没放置的矩形索引
    private List<Integer> allowedItems;
    // 信息素变化矩阵
    private double[][] delta;
    // 矩形不同度矩阵
    private double[][] different;
    // 信息素重要程度
    private double alpha;
    // 启发式因子重要程度
    private double beta;
    // 矩形数量
    private int itemNum;
    // 第一个放置的矩形
    private int firstSquare;
    // 当前放置的矩形
    private int currentSquare;
    // 随机数对象
    private Random random;

    // 外矩形的长宽
    double W, H;
    // 是否允许旋转
    private boolean isRotateEnable;

    Solution localSolution;

    //构造函数
    public Ant(boolean isRotateEnable, double W, double H, Item[] items, Long seed) {
        this.itemNum = items.length;
        this.items = items;
        this.H = H;
        this.W = W;
        this.isRotateEnable = isRotateEnable;
        this.random = seed == null ? new Random() : new Random(seed);
    }

    //初始化
    public void initAnt(double[][] different, double a, double b) {
        alpha = a;
        beta = b;
        this.different = different;
        // 初始允许搜索的矩形集合
        allowedItems = new ArrayList<>();
        // 初始禁忌表
        sequence = new ArrayList<>();
        // 初始信息数变化矩阵为0
        delta = new double[itemNum][itemNum];
        // 设置起始矩形(随机选取第一个矩形)
        firstSquare = random.nextInt(itemNum);

        for (int i = 0; i < itemNum; i++) {
            if (i != firstSquare) {
                allowedItems.add(i);
            }
        }
        // 将第一个放置的矩形添加至禁忌表
        sequence.add(firstSquare);
        // 第一个矩形即为当前放置的矩形
        currentSquare = firstSquare;
    }

    //选择下一个矩形
    public void selectNextSquare(double[][] pheromone) {
        double[] p = new double[itemNum];
        double sum = 0d;

        // --------------- 思路1:直接将距离看为一个常数1 --------------------
//        // 计算分母部分
//        for (Integer i : allowedItems) {
//            sum += Math.pow(pheromone[currentSquare][i], alpha)
//                    * Math.pow(1.0, beta);
//        }
//        // 计算概率矩阵
//        for (int i : allowedItems) {
//            p[i] = (Math.pow(pheromone[currentSquare][i], alpha) * Math
//                    .pow(1.0, beta)) / sum;
//        }

        // --------------- 思路2:采用矩形的不同度代替距离 --------------------
        // 计算分母部分
        for (Integer i : allowedItems) {
            sum += Math.pow(pheromone[currentSquare][i], alpha)
                    * Math.pow(1.0 / different[currentSquare][i], beta);
        }
        // 计算概率矩阵
        for (int i : allowedItems) {
            p[i] = (Math.pow(pheromone[currentSquare][i], alpha) * Math
                    .pow(1.0 / different[currentSquare][i], beta)) / sum;
        }



        // 轮盘赌选择下一个矩形
        double sleectP = random.nextDouble();
        int selectSquare = -1;
        double sum1 = 0d;
        for (int i = 0; i < itemNum; i++) {
            sum1 += p[i];
            if (compareDouble(sum1, sleectP) != -1) {
                selectSquare = i;
                break;
            }
        }
        // 从允许选择的矩形中去除select 矩形
        for (Integer i : allowedItems) {
            if (i == selectSquare) {
                allowedItems.remove(i);
                break;
            }
        }
        // 在禁忌表中添加select矩形
        sequence.add(selectSquare);
        currentSquare = selectSquare;
    }

    // 根据顺序进行装箱,并返回装载的矩形总面积
    public void evaluate() {
        // 根据顺序进行装箱
        Item[] items = new Item[this.items.length];
        for (int i = 0; i < sequence.size(); i++) {
            items[i] = this.items[sequence.get(i)];
        }
        localSolution = new SkyLinePacking(isRotateEnable, W, H, items).packing();
    }

    /**
     * @param d1 双精度浮点型变量1
     * @param d2 双精度浮点型变量2
     * @return 返回0代表两个数相等，返回1代表前者大于后者，返回-1代表前者小于后者，
     * @Description 判断两个双精度浮点型变量的大小关系
     */
    private int compareDouble(double d1, double d2) {
        // 定义一个误差范围，如果两个数相差小于这个误差，则认为他们是相等的 1e-06 = 0.000001
        double error = 1e-06;
        if (Math.abs(d1 - d2) < error) {
            return 0;
        } else if (d1 < d2) {
            return -1;
        } else if (d1 > d2) {
            return 1;
        } else {
            throw new RuntimeException("d1 = " + d1 + " , d2 = " + d2);
        }
    }

    public Item[] getItems() {
        return items;
    }

    public void setItems(Item[] items) {
        this.items = items;
    }

    public List<Integer> getSequence() {
        return sequence;
    }

    public void setSequence(List<Integer> sequence) {
        this.sequence = sequence;
    }

    public List<Integer> getAllowedItems() {
        return allowedItems;
    }

    public void setAllowedItems(List<Integer> allowedItems) {
        this.allowedItems = allowedItems;
    }

    public double[][] getDelta() {
        return delta;
    }

    public void setDelta(double[][] delta) {
        this.delta = delta;
    }

    public double[][] getDifferent() {
        return different;
    }

    public void setDifferent(double[][] different) {
        this.different = different;
    }

    public double getAlpha() {
        return alpha;
    }

    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }

    public double getBeta() {
        return beta;
    }

    public void setBeta(double beta) {
        this.beta = beta;
    }

    public int getItemNum() {
        return itemNum;
    }

    public void setItemNum(int itemNum) {
        this.itemNum = itemNum;
    }

    public int getFirstSquare() {
        return firstSquare;
    }

    public void setFirstSquare(int firstSquare) {
        this.firstSquare = firstSquare;
    }

    public int getCurrentSquare() {
        return currentSquare;
    }

    public void setCurrentSquare(int currentSquare) {
        this.currentSquare = currentSquare;
    }

    public Random getRandom() {
        return random;
    }

    public void setRandom(Random random) {
        this.random = random;
    }

    public double getW() {
        return W;
    }

    public void setW(double w) {
        W = w;
    }

    public double getH() {
        return H;
    }

    public void setH(double h) {
        H = h;
    }

    public boolean isRotateEnable() {
        return isRotateEnable;
    }

    public void setRotateEnable(boolean rotateEnable) {
        isRotateEnable = rotateEnable;
    }

    public Solution getLocalSolution() {
        return localSolution;
    }

    public void setLocalSolution(Solution localSolution) {
        this.localSolution = localSolution;
    }
}
