package com.deep.plant;

import com.google.gson.annotations.SerializedName;

import java.text.DecimalFormat;

/**
 * Created by Somnath-Laptop on 10-Dec-2016.
 */

public class Result {
    @SerializedName("Time")
    private String Time = "";
    @SerializedName("Disease")
    private String Disease = "";
    @SerializedName("Crop")
    private String Crop = "";


    public String getTime() {
        return Time;
    }

    public void setTime(String time) {
        Time = time.trim();
    }

    public String getDisease() {
        return Disease;
    }

    public void setDisease(String disease) {
        Disease = disease.trim();
    }

    public String getCrop() {
        return Crop;
    }

    public void setCrop(String crop) {
        Crop = crop.trim();
    }

    @Override
    public String toString() {

        DecimalFormat formatter = new DecimalFormat("#0.00");
        String d = new String(Time);
        Double dble = new Double(d.valueOf(d));
        String formatted = formatter.format(dble);
        return String.format("Disease:%s\nCrop:%s\nTime(s):%s", Disease, Crop, formatted);
    }
}
