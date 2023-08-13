package com.example.yin.domain;

//包含评分数量和评分年月统计的
public class MusicRM {
    private Integer mid;
    private String name;
    private String intro;
    private String tag;
    private String pic;

    public Integer count;
    public Integer yearmonth;

    public MusicRM(Integer mid, String name, String intro, String tag, String pic, Integer count, Integer yearmonth) {
        this.mid = mid;
        this.name = name;
        this.intro = intro;
        this.tag = tag;
        this.pic = pic;
        this.count = count;
        this.yearmonth = yearmonth;
    }

    public MusicRM(Integer mid, String name, String intro, String tag, String pic, Integer count) {
        this.mid = mid;
        this.name = name;
        this.intro = intro;
        this.tag = tag;
        this.pic = pic;
        this.count = count;
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

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Integer getYearmonth() {
        return yearmonth;
    }

    public void setYearmonth(Integer yearmonth) {
        this.yearmonth = yearmonth;
    }
}
