package com.example.yin.domain;

import java.util.Comparator;

public class RateRecentMore{

    public Integer mid;

    public Integer count;

    public Integer yearmonth;

    public RateRecentMore(Integer mid, Integer count, Integer yearmonth) {
        this.mid = mid;
        this.count = count;
        this.yearmonth = yearmonth;
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

    public Integer getYearMonth() {
        return yearmonth;
    }

    public void setYearMonth(Integer yearmonth) {
        this.yearmonth = yearmonth;
    }

    @Override
    public String toString() {
        return "RateRecentMore{" +
                "mid=" + mid +
                ", count=" + count +
                ", yearmonth=" + yearmonth +
                '}';
    }
}
