package com.example.yin.utils;

import com.example.yin.domain.RateRecentMore;

import java.util.*;

public class Sort {

    public List<RateRecentMore> sortByTime(List<RateRecentMore> rrmlist){

            //按照id去排序
            Collections.sort(rrmlist, new Comparator<RateRecentMore>() {
                @Override
                public int compare(RateRecentMore rrm1, RateRecentMore rrm2) {
                    // TODO Auto-generated method stub
                    //定义比较大小
                    if (rrm1.getYearMonth() > rrm2.getYearMonth()) {
                        return 1;
                    } else if (Objects.equals(rrm1.getYearMonth(), rrm2.getYearMonth())) {
                        return 0;
                    } else {
                        return -1;
                    }
                }
            });
            return rrmlist;
        }

}
