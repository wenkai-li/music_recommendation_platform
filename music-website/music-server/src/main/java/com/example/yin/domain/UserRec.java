package com.example.yin.domain;

import java.util.List;

public class UserRec {
    public Integer uid;

    public List<Rec> recs;

    public UserRec(Integer uid, List<Rec> recs) {
        this.uid = uid;
        this.recs = recs;
    }

    public UserRec() {
    }

    public Integer getUid() {
        return uid;
    }

    public void setUid(Integer uid) {
        this.uid = uid;
    }

    public List<Rec> getRecs() {
        return recs;
    }

    public void setRecs(List<Rec> recs) {
        this.recs = recs;
    }

}
