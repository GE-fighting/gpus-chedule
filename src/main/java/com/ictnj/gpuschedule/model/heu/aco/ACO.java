package com.ictnj.gpuschedule.model.heu.aco;

/**
 * @ClassName ACO
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/11 19:37
 **/

import com.ictnj.gpuschedule.entity.Instance;
import com.ictnj.gpuschedule.entity.Item;
import com.ictnj.gpuschedule.entity.Solution;

import java.util.ArrayList;
import java.util.List;

public class ACO {

    // 蚂蚁数组
    public Ant[] ants;
    // 蚂蚁数量
    public int antNum;
    // 矩形数量
    public int itemNum;
    // 最大迭代数
    public int MAX_GEN;
    // 信息素矩阵
    public double[][] pheromone;
    // 最佳放置序列
    public List<Integer> bestSquence;
    // 最佳迭代数
    public int bestT;
    // 最优解
    public Solution bestSolution;
    // 不同度矩形
    double[][] different;

    // 三个参数
    // 信息素重要程度
    private double alpha;
    // 启发式因子重要程度
    private double beta;
    // 信息素挥发速率
    private double rho;

    // 边界的宽
    private double W;
    // 边界的高
    private double H;
    // 矩形数组
    Item[] items;
    // 是否可以旋转
    private boolean isRotateEnable;
    // 随机数种子
    Long seed;

    /**
     * @param antNum   蚂蚁数量
     * @param MAX_GEN  迭代次数(提高这个值可以稳定地提高解质量，但是会增加求解时间)
     * @param alpha    信息素重要程度
     * @param beta     启发式因子重要程度
     * @param rho      信息素挥发速率
     * @param instance 实例对象
     * @param seed     随机数种子，如果传入null则不设置随机数种子，否则按照传入的种子进行设置，方便复现结果
     * @Description 构造函数
     */
    public ACO(int antNum, int MAX_GEN, double alpha, double beta, double rho, Instance instance, Long seed) {
        this.antNum = antNum;
        this.ants = new Ant[antNum];
        this.alpha = alpha;
        this.beta = beta;
        this.rho = rho;
        this.MAX_GEN = MAX_GEN;
        this.W = instance.getW();
        this.H = instance.getH();
        this.isRotateEnable = instance.isRotateEnable();
        this.items = Item.copy(instance.getItemList().toArray(new Item[0]));
        this.itemNum = this.items.length;
        this.seed = seed;
    }

    /**
     * @return 最佳装载结果对象Solution
     * @Description 蚁群算法主函数
     */
    public Solution solve() {
        // 进行初始化操作
        init();
        // 迭代MAX_GEN次
        for (int g = 0; g < MAX_GEN; g++) {
            // antNum只蚂蚁
            for (int i = 0; i < antNum; i++) {
                // i这只蚂蚁走itemNum步，构成一个完整的矩形放置顺序
                for (int j = 1; j < itemNum; j++) {
                    ants[i].selectNextSquare(pheromone);
                }
                // 查看这只蚂蚁装载利用率是否比当前最优解优秀
                ants[i].evaluate();
                if (bestSolution == null || compareDouble(ants[i].getLocalSolution().getRate(), bestSolution.getRate()) == 1) {
                    // 比当前优秀则拷贝优秀的放置顺序
                    bestSquence = new ArrayList<>(ants[i].getSequence());
                    bestT = g;
                    bestSolution = ants[i].getLocalSolution();
                    System.out.println("蚂蚁 " + (i + 1) + " 找到更优解 , 当前迭代次数为: " + g + " , 利用率为：" + bestSolution.getRate());
                }
                // 更新这只蚂蚁的信息数变化矩阵，对称矩阵
                for (int j = 0; j < itemNum; j++) {
                    ants[i].getDelta()[ants[i].getSequence().get(j)][ants[i]
                            .getSequence().get(j + 1 >= itemNum ? 0 : j + 1)] = (1.0 / ants[i]
                            .getLocalSolution().getRate());
                    ants[i].getDelta()[ants[i].getSequence().get(j + 1 >= itemNum ? 0 : j + 1)][ants[i]
                            .getSequence().get(j)] = (1.0 / ants[i]
                            .getLocalSolution().getRate());
                }
            }
            // 更新信息素
            updatePheromone();
            // 重新初始化蚂蚁
            for (int i = 0; i < antNum; i++) {
                ants[i].initAnt(different, alpha, beta);
            }
        }
        // 返回结果
        return bestSolution;
    }

    /**
     * @Description 初始化操作
     */
    private void init() {
        //初始化不同度矩阵
        different = new double[itemNum][itemNum];
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                if (i == j) {
                    different[i][j] = 0.0;
                } else {
                    different[i][j] = getDifferent(items[i], items[j]);
                }
            }
        }
        //初始化信息素矩阵
        pheromone = new double[itemNum][itemNum];
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                // 初始化为0.1
                pheromone[i][j] = 0.1;
            }
        }
        // 放置蚂蚁
        for (int i = 0; i < antNum; i++) {
            ants[i] = new Ant(isRotateEnable, W, H, items, seed);
            ants[i].initAnt(different, alpha, beta);
        }
    }

    /**
     * @Description 更新信息素
     */
    private void updatePheromone() {
        // 信息素挥发
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                pheromone[i][j] = pheromone[i][j] * (1 - rho);
            }
        }
        // 信息素更新
        for (int i = 0; i < itemNum; i++) {
            for (int j = 0; j < itemNum; j++) {
                for (int k = 0; k < antNum; k++) {
                    pheromone[i][j] += ants[k].getDelta()[i][j];
                }
            }
        }
    }

    /**
     * @param a 矩形a
     * @param b 矩形b
     * @return 矩形a和b的不同度
     * @Description 计算矩形a对b的不同度
     */
    public double getDifferent(Item a, Item b) {
        double avgW = (a.getW() + b.getW()) / 2.0;
        double avgH = (a.getH() + b.getH()) / 2.0;
        double different = Math.abs(a.getH() - b.getH()) / avgH;
        different = Math.min(Math.abs(a.getW() - b.getW()) / avgW, different);
        if (isRotateEnable) {
            different = Math.min(Math.abs(a.getW() - b.getH()) / avgH, different);
            different = Math.min(Math.abs(a.getH() - b.getW()) / avgW, different);
        }
        return Math.max(0.0001, different);
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

}

