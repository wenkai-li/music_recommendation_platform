package com.example.yin.domain;

public class Rec {

    public Integer mid;
    public double score;
    public Rec(Integer mid, double score) {
        this.mid = mid;
        this.score = score;
    }


    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }
}
