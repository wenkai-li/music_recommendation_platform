package com.example.yin.domain;

import java.util.List;

public class StyleRec {

    public String uri;

    public List<Rec> recs;

    public StyleRec(String uri, List<Rec> recs) {
        this.uri = uri;
        this.recs = recs;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public List<Rec> getRec() {
        return recs;
    }

    public void setRec(List<Rec> recs) {
        this.recs = recs;
    }
}
