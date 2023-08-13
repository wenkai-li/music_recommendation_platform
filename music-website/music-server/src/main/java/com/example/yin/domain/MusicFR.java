package com.example.yin.domain;

public class MusicFR {
    private Integer mid;
    private String name;
    private String intro;

    private String tag;

    private String pic;

    public MusicFR(Integer mid, String name, String intro, String tag, String pic) {
        this.mid = mid;
        this.name = name;
        this.intro = intro;
        this.tag = tag;
        this.pic = pic;
    }

    public String getPic() {
        return pic;
    }

    public void setPic(String pic) {
        this.pic = pic;
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

    @Override
    public String toString() {
        return "MusicFR{" +
                "mid=" + mid +
                ", name='" + name + '\'' +
                ", intro='" + intro + '\'' +
                ", tag='" + tag + '\'' +
                ", pic='" + pic + '\'' +
                '}';
    }
}
