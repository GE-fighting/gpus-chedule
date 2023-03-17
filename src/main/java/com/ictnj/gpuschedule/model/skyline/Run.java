package com.ictnj.gpuschedule.model.skyline;

/**
 * @ClassName Run
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/11 19:31
 **/

import com.ictnj.gpuschedule.entity.Instance;
import com.ictnj.gpuschedule.entity.Item;
import com.ictnj.gpuschedule.entity.PlaceItem;
import com.ictnj.gpuschedule.entity.Solution;
import com.ictnj.gpuschedule.util.ReadDataUtil;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


import java.util.Arrays;
import java.util.Random;

public class Run extends javafx.application.Application {

    private int counter = 0;

    @Override
    public void start(Stage primaryStage) throws Exception {

        // 数据地址
        String path = "src/main/java/com/wskh/data/data.txt";
        // 根据txt文件获取实例对象
        Instance instance = new ReadDataUtil().getInstance(path);
        // 记录算法开始时间
        long startTime = System.currentTimeMillis();
        // 实例化天际线对象
        SkyLinePacking skyLinePacking = new SkyLinePacking(instance.isRotateEnable(), instance.getW(), instance.getH(), instance.getItemList().toArray(new Item[0]));
        // 调用天际线算法进行求解
        Solution solution = skyLinePacking.packing();
        // 输出相关信息
        System.out.println("求解用时:" + (System.currentTimeMillis() - startTime) / 1000.0 + " s");
        System.out.println("共放置了矩形" + solution.getPlaceItemList().size() + "个");
        System.out.println("利用率为:" + solution.getRate());
        // 输出画图数据
        String[] strings1 = new String[solution.getPlaceItemList().size()];
        String[] strings2 = new String[solution.getPlaceItemList().size()];
        for (int i = 0; i < solution.getPlaceItemList().size(); i++) {
            PlaceItem placeItem = solution.getPlaceItemList().get(i);
            strings1[i] = "{x:" + placeItem.getX() + ",y:" + placeItem.getY() + ",l:" + placeItem.getH() + ",w:" + placeItem.getW() + "}";
            strings2[i] = placeItem.isRotate() ? "1" : "0";
        }
        System.out.println("data:" + Arrays.toString(strings1) + ",");
        System.out.println("isRotate:" + Arrays.toString(strings2) + ",");

        // --------------------------------- 后面这些都是画图相关的代码，可以不用管 ---------------------------------------------
        AnchorPane pane = new AnchorPane();
        Canvas canvas = new Canvas(instance.getW(), instance.getH());
        pane.getChildren().add(canvas);
        canvas.relocate(100, 100);
        // 绘制最外层的矩形
        canvas = draw(canvas, 0, 0, instance.getW(), instance.getH(), true);
        // 添加按钮
        Button nextButton = new Button("Next +1");
        Canvas finalCanvas = canvas;
        nextButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                try {
                    PlaceItem placeItem = solution.getPlaceItemList().get(counter);
                    draw(finalCanvas, placeItem.getX(), placeItem.getY(), placeItem.getW(), placeItem.getH(), false);
                    counter++;
                } catch (Exception e) {
                    Alert alert = new Alert(Alert.AlertType.WARNING);
                    alert.setContentText("已经没有可以放置的矩形了！");
                    alert.showAndWait();
                }
            }
        });
        //
        pane.getChildren().add(nextButton);
        primaryStage.setTitle("二维矩形装箱可视化");
        primaryStage.setScene(new Scene(pane, 1000, 1000, Color.AQUA));
        primaryStage.show();
    }

    private Canvas draw(Canvas canvas, double x, double y, double l, double w, boolean isBound) {
        GraphicsContext gc = canvas.getGraphicsContext2D();
        // 边框
        gc.setStroke(Color.BLACK);
        gc.setLineWidth(2);
        gc.strokeRect(x, y, l, w);
        // 填充
        if (!isBound) {
            gc.setFill(new Color(new Random().nextDouble(), new Random().nextDouble(), new Random().nextDouble(), new Random().nextDouble()));
        } else {
            gc.setFill(new Color(1, 1, 1, 1));
        }
        gc.fillRect(x, y, l, w);
        return canvas;
    }

    public static void main(String[] args) {
        launch(args);
    }

}

