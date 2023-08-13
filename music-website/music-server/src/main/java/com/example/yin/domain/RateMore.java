package com.example.yin.domain;

public class RateMore {

    public Integer mid;

    public Integer count;

    public RateMore(Integer mid, Integer count) {
        this.mid = mid;
        this.count = count;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
