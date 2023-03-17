package com.ictnj.gpuschedule.util;

/**
 * @ClassName ReadDataUtil
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/11 19:27
 **/

import com.ictnj.gpuschedule.entity.Instance;
import com.ictnj.gpuschedule.entity.Item;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * @Author：WSKH
 * @ClassName：ReadDataUtil
 * @ClassType：
 * @Description：读取数据工具类
 * @Date：2022/11/6/19:39
 * @Email：1187560563@qq.com
 * @Blog：https://blog.csdn.net/weixin_51545953?type=blog
 */
public class ReadDataUtil {
    public Instance getInstance(String path) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(path));
        String input = null;
        Instance instance = new Instance();
        List<Item> itemList = new ArrayList<>();
        boolean isFirstLine = true;
        while ((input = bufferedReader.readLine()) != null) {
            String[] split = input.split(" ");
            if (isFirstLine) {
                instance.setW(Double.parseDouble(split[0]));
                instance.setH(Double.parseDouble(split[1]));
                instance.setRotateEnable("1".equals(split[2]));
                isFirstLine = false;
            } else {
                itemList.add(new Item(UUID.randomUUID().toString(), Double.parseDouble(split[0]), Double.parseDouble(split[1])));
            }
        }
        instance.setItemList(itemList);
        return instance;
    }
}
