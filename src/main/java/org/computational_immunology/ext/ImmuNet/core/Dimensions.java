package org.computational_immunology.ext.ImmuNet.core;

public class Dimensions {
    double height;
    double  width;

    public Dimensions(double hgt, double wdt){
        if (hgt == 0 || wdt == 0){
            throw new IllegalArgumentException("Input cannot be zero.");
        }
        if (hgt < 0 || wdt < 0){
            throw new IllegalArgumentException("Input cannot be negative.");
        }
        this.height = hgt;
        this.width = wdt;
    }

    public double getHeight(){
        return this.height;
    }

    public double getWidth(){
        return this.width;
    }
}
