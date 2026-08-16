package org.computational_immunology.ext.ImmuNet.core.models;

import java.util.Arrays;

/**
 * Represents one prediction of a given machine learning model for a given tile.
 * The field t represents the type of the cell, for example, "B cell".
 */
public class PredictionAnnotationPoint {

    private final String dataset;
    private final String slide;
    private final String tile;
    private final String t;
    private final int x;
    private final int y;
    private final String modelName;
    private final int[] positivity;
    private final double[] prediction;

public PredictionAnnotationPoint(String dataset, String slide, String tile, String modelName,
                                    String t, int[] positivity, int x, int y, double[] prediction) {
        this.dataset = dataset;
        this.slide = slide;
        this.tile = tile;
        this.modelName = modelName;
        this.t = t;
        this.positivity = positivity;
        this.x = x;
        this.y = y;
        this.prediction = prediction;
    }
 
    public String getDataset() {
        return dataset;
    }
 
    public String getSlide() {
        return slide;
    }
 
    public String getTile() {
        return tile;
    }
 
    public String getModelName() {
        return modelName;
    }
 
    public String getT() {
        return t;
    }
 
    public int[] getPositivity() {
        return positivity;
    }
 
    public int getX() {
        return x;
    }
 
    public int getY() {
        return y;
    }
 
    public double[] getPrediction() {
        return prediction;
    }
 
    @Override
    public String toString() {
        return "PredictionAnnotationPoint{" +
                "dataset='" + dataset + '\'' +
                ", slide='" + slide + '\'' +
                ", tile='" + tile + '\'' +
                ", model='" + modelName + '\'' +
                ", t='" + t + '\'' +
                ", positivity=" + Arrays.toString(positivity) +
                ", x=" + x +
                ", y=" + y +
                ", prediction=" + Arrays.toString(prediction) +
                '}';
    }
}

