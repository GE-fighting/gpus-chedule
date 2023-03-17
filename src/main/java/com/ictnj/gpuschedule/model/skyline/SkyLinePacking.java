package com.ictnj.gpuschedule.model.skyline;

/**
 * @ClassName SkyLinePacking
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/11 19:28
 **/

import com.ictnj.gpuschedule.entity.Item;
import com.ictnj.gpuschedule.entity.PlaceItem;
import com.ictnj.gpuschedule.entity.SkyLine;
import com.ictnj.gpuschedule.entity.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * @Author：WSKH
 * @ClassName：SkyLinePacking
 * @ClassType：
 * @Description：天际线启发式算法求解二维矩形装箱问题
 * @Date：2022/11/6/19:39
 * @Email：1187560563@qq.com
 * @Blog：https://blog.csdn.net/weixin_51545953?type=blog
 */
public class SkyLinePacking {

    // 边界的宽
    private double W;
    // 边界的高
    private double H;
    // 矩形数组
    private Item[] items;
    // 是否可以旋转
    private boolean isRotateEnable;
    // 基于堆优化的天际线优先队列（PriorityBlockingQueue是线程安全的，底层基于最小二叉堆实现，具有高效动态排序能力）
    private PriorityBlockingQueue<SkyLine> skyLineQueue = new PriorityBlockingQueue<>();

    /**
     * @param isRotateEnable 是否允许矩形旋转
     * @param W              边界宽度
     * @param H              边界高度
     * @param items          矩形集合
     * @Description 构造函数
     */
    public SkyLinePacking(boolean isRotateEnable, double W, double H, Item[] items) {
        this.isRotateEnable = isRotateEnable;
        this.W = W;
        this.H = H;
        this.items = items;
    }

    /**
     * @return 放置好的矩形列表
     * @Description 天际线启发式装箱主函数
     */
    public Solution packing() {

        // 用来存储已经放置的矩形
        List<PlaceItem> placeItemList = new ArrayList<>();
        // 用来记录已经放置矩形的总面积
        double totalS = 0d;

        // 获取初始天际线(边界底部)
        skyLineQueue.add(new SkyLine(0, 0, W));

        // 记录已经放置的矩形
        boolean[] used = new boolean[items.length];

        // 开始天际线启发式迭代
        while (!skyLineQueue.isEmpty() && placeItemList.size() < items.length) {
            // 获取当前最下最左的天际线（取出队首元素）
            SkyLine skyLine = skyLineQueue.poll();
            // 初始化hl和hr
            double hl = H - skyLine.getY();
            double hr = H - skyLine.getY();
            // 提前跳出计数器(如果hl和hr都获取到了就可以提前跳出，节省时间)
            int c = 0;
            // 顺序遍历天际线队列，根据skyline和skyline队列获取hl和hr
            for (SkyLine line : skyLineQueue) {
                // 由于skyLine是队首元素，所以它的Y肯定最小，所以line.getY() - skyLine.getY()肯定都大于等于0
                if (compareDouble(line.getX() + line.getLen(), skyLine.getX()) == 0) {
                    // 尾头相连，是hl
                    hl = line.getY() - skyLine.getY();
                    c++;
                } else if (compareDouble(line.getX(), skyLine.getX() + skyLine.getLen()) == 0) {
                    // 头尾相连，是hr
                    hr = line.getY() - skyLine.getY();
                    c++;
                }
                // hl和hr都获取到了，就没必要继续遍历了，可以提前跳出节省时间
                if (c == 2) {
                    break;
                }
            }
            // 记录最大评分矩形的索引
            int maxItemIndex = -1;
            // 记录最大评分的矩形是否旋转
            boolean isRotate = false;
            // 记录最大评分
            int maxScore = -1;
            // 遍历每一个矩形，选取评分最大的矩形进行放置
            for (int i = 0; i < items.length; i++) {
                // 矩形没有放置过，才进行接下来的流程
                if (!used[i]) {
                    // 不旋转的情况
                    int score = score(items[i].getW(), items[i].getH(), skyLine, hl, hr);
                    if (score > maxScore) {
                        // 更新最大评分
                        maxScore = score;
                        maxItemIndex = i;
                        isRotate = false;
                    }
                    // 旋转的情况（矩形宽和高互换）
                    if (isRotateEnable) {
                        int rotateScore = score(items[i].getH(), items[i].getW(), skyLine, hl, hr);
                        if (rotateScore > maxScore) {
                            // 更新最大评分
                            maxScore = rotateScore;
                            maxItemIndex = i;
                            isRotate = true;
                        }
                    }
                }
            }
            // 如果当前最大得分大于等于0，则说明有矩形可以放置，则按照规则对其进行放置
            if (maxScore >= 0) {
                // 左墙高于等于右墙
                if (hl >= hr) {
                    // 评分为2时，矩形靠天际线右边放，否则靠天际线左边放
                    if (maxScore == 2) {
                        placeItemList.add(placeRight(items[maxItemIndex], skyLine, isRotate));
                    } else {
                        placeItemList.add(placeLeft(items[maxItemIndex], skyLine, isRotate));
                    }
                } else {
                    // 左墙低于右墙
                    // 评分为4或0时，矩形靠天际线右边放，否则靠天际线左边放
                    if (maxScore == 4 || maxScore == 0) {
                        placeItemList.add(placeRight(items[maxItemIndex], skyLine, isRotate));
                    } else {
                        placeItemList.add(placeLeft(items[maxItemIndex], skyLine, isRotate));
                    }
                }
                // 根据索引将该矩形设置为已经放置的矩形
                used[maxItemIndex] = true;
                // 将该矩形面积追加到totalS中
                totalS += (items[maxItemIndex].getW() * items[maxItemIndex].getH());
            } else {
                // 如果当前天际线一个矩形都放不下，那就上移天际线，与其他天际线合并
                combineSkylines(skyLine);
            }
        }
        // 返回求解结果
        return new Solution(placeItemList, totalS, totalS / (W * H));
    }


    /**
     * @param item     要放置的矩形对象
     * @param skyLine  矩形放置所在的天际线
     * @param isRotate 矩形是否旋转
     * @return 返回一个PlaceItem对象（放置好的矩形对象）
     * @Description 将矩形靠左放
     */
    private PlaceItem placeLeft(Item item, SkyLine skyLine, boolean isRotate) {
        // 生成PlaceItem对象
        PlaceItem placeItem = null;
        if (!isRotate) {
            placeItem = new PlaceItem(item.getName(), skyLine.getX(), skyLine.getY(), item.getW(), item.getH(), isRotate);
        } else {
            placeItem = new PlaceItem(item.getName(), skyLine.getX(), skyLine.getY(), item.getH(), item.getW(), isRotate);
        }
        // 将新天际线加入队列
        addSkyLineInQueue(skyLine.getX(), skyLine.getY() + placeItem.getH(), placeItem.getW());
        addSkyLineInQueue(skyLine.getX() + placeItem.getW(), skyLine.getY(), skyLine.getLen() - placeItem.getW());
        // 返回PlaceItem对象
        return placeItem;
    }

    /**
     * @param item     要放置的矩形对象
     * @param skyLine  矩形放置所在的天际线
     * @param isRotate 矩形是否旋转
     * @return 返回一个PlaceItem对象（放置好的矩形对象）
     * @Description 将矩形靠右放
     */
    private PlaceItem placeRight(Item item, SkyLine skyLine, boolean isRotate) {
        // 生成PlaceItem对象
        PlaceItem placeItem = null;
        if (!isRotate) {
            placeItem = new PlaceItem(item.getName(), skyLine.getX() + skyLine.getLen() - item.getW(), skyLine.getY(), item.getW(), item.getH(), isRotate);
        } else {
            placeItem = new PlaceItem(item.getName(), skyLine.getX() + skyLine.getLen() - item.getH(), skyLine.getY(), item.getH(), item.getW(), isRotate);
        }
        // 将新天际线加入队列
        addSkyLineInQueue(skyLine.getX(), skyLine.getY(), skyLine.getLen() - placeItem.getW());
        addSkyLineInQueue(placeItem.getX(), skyLine.getY() + placeItem.getH(), placeItem.getW());
        // 返回PlaceItem对象
        return placeItem;
    }

    /**
     * @param x   新天际线x坐标
     * @param y   新天际线y坐标
     * @param len 新天际线长度
     * @return void
     * @Description 将指定属性的天际线加入天际线队列
     */
    private void addSkyLineInQueue(double x, double y, double len) {
        // 新天际线长度大于0时，才加入
        if (compareDouble(len, 0.0) == 1) {
            skyLineQueue.add(new SkyLine(x, y, len));
        }
    }

    /**
     * @param skyLine 一个放置不下任意矩形的天际线
     * @return void
     * @Description 传入一个放置不下任意矩形的天际线，将其上移，与其他天际线进行合并
     */
    private void combineSkylines(SkyLine skyLine) {
        boolean b = false;
        for (SkyLine line : skyLineQueue) {
            if (compareDouble(skyLine.getY(), line.getY()) != 1) {
                // 头尾相连
                if (compareDouble(skyLine.getX(), line.getX() + line.getLen()) == 0) {
                    skyLineQueue.remove(line);
                    b = true;
                    skyLine.setX(line.getX());
                    skyLine.setY(line.getY());
                    skyLine.setLen(line.getLen() + skyLine.getLen());
                    break;
                }
                // 尾头相连
                if (compareDouble(skyLine.getX() + skyLine.getLen(), line.getX()) == 0) {
                    skyLineQueue.remove(line);
                    b = true;
                    skyLine.setY(line.getY());
                    skyLine.setLen(line.getLen() + skyLine.getLen());
                    break;
                }
            }
        }
        // 如果有进行合并，才加入
        if (b) {
            // 将最后合并好的天际线加入天际线队列
            skyLineQueue.add(skyLine);
        }
    }

    /**
     * @param w       当前要放置的矩形的宽
     * @param h       当前要放置的矩形的高
     * @param skyLine 该天际线对象
     * @param hl      该天际线的左墙
     * @param hr      该天际线的右墙
     * @return 矩形块的评分，如果评分为 -1 ，则说明该矩形不能放置在该天际线上
     * @Description 对矩形进行评分
     */
    private int score(double w, double h, SkyLine skyLine, double hl, double hr) {
        // 当前天际线长度小于当前矩形宽度，放不下
        if (compareDouble(skyLine.getLen(), w) == -1) {
            return -1;
        }
        // 如果超出上界，也不能放
        if (compareDouble(skyLine.getY() + h, H) == 1) {
            return -1;
        }
        int score = -1;
        // 左边墙高于等于右边墙
        if (hl >= hr) {
            if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == 0) {
                score = 7;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == 0) {
                score = 6;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == 1) {
                score = 5;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hl) == 0) {
                score = 4;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == -1 && compareDouble(h, hr) == 1) {
                score = 3;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hr) == 0) {
                // 靠右
                score = 2;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == -1) {
                score = 1;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hl) != 0) {
                score = 0;
            } else {
                throw new RuntimeException("w = " + w + " , h = " + h + " , hl = " + hl + " , hr = " + hr + " , skyline = " + skyLine);
            }
        } else {
            if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == 0) {
                score = 7;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == 0) {
                score = 6;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == 1) {
                score = 5;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hr) == 0) {
                // 靠右
                score = 4;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == -1 && compareDouble(h, hl) == 1) {
                score = 3;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hl) == 0) {
                score = 2;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == -1) {
                score = 1;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hr) != 0) {
                // 靠右
                score = 0;
            } else {
                throw new RuntimeException("w = " + w + " , h = " + h + " , hl = " + hl + " , hr = " + hr + " , skyline = " + skyLine);
            }
        }
        return score;
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
