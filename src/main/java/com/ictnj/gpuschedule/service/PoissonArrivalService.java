package com.ictnj.gpuschedule.service;

import lombok.Data;

import java.util.Random;

/**
 * @ClassName PoissonArrivalService
 * @Description TODO
 * @Author zhangjun
 * @Date 2023/3/17 15:31
 **/
@Data
public class PoissonArrivalService {
    private Random random;
    private double meanArrivalTime;

    public PoissonArrivalService(double meanArrivalTime) {
        this.meanArrivalTime = meanArrivalTime;
        this.random = new Random();
    }

    public double getNextArrivalTime() {
        return -meanArrivalTime * Math.log(1 - random.nextDouble());
    }
}
