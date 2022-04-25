package com.example.nguide6;

import org.opencv.core.Mat;

public class Location {
    private double x;
    private double y;
    private Mat descriptor;
    public Location(double x, double y){
        this.x = 0;
        this.y = 0;
    }
    public double getX(){
        return this.x;
    }
    public double getY(){
        return this.y;
    }

}