package com.example.yin.domain;

public class MusicWS {

    private Integer mid;
    private String name;
    private String intro;
    private String tag;
    private String pic;
    public Double score;

    public MusicWS(Integer mid, String name, String intro, String tag, String pic, Double score) {
        this.mid = mid;
        this.name = name;
        this.intro = intro;
        this.tag = tag;
        this.pic = pic;
        this.score = score;
    }

    public Integer getMid() {
        return mid;
    }

    public void setMid(Integer mid) {
        this.mid = mid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIntro() {
        return intro;
    }

    public void setIntro(String intro) {
        this.intro = intro;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }
}
