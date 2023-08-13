package com.example.yin.domain;

public class UMRating {

    private int uid;

    private int mid;

    private int score;

    public UMRating(int uid, int mid, int score) {
        this.uid = uid;
        this.mid = mid;
        this.score = score;
    }

    public int getUid() {
        return uid;
    }

    public void setUid(int uid) {
        this.uid = uid;
    }

    public int getMid() {
        return mid;
    }

    public void setMid(int mid) {
        this.mid = mid;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }
}
