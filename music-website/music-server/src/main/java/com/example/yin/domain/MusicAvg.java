package com.example.yin.domain;

public class MusicAvg {

    private Integer mid;

    private Double avg;

    public MusicAvg(Integer mid, Double avg) {
        this.mid = mid;
        this.avg = avg;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public Double getAvg() {
        return avg;
    }

    public void setAvg(Double avg) {
        this.avg = avg;
    }
}
