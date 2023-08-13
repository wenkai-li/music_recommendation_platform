package com.example.yin.controller;

import com.example.yin.common.ErrorMessage;
import com.example.yin.common.SuccessMessage;
import com.example.yin.common.WarningMessage;
import com.example.yin.dao.mongodb.RankDao;
import com.example.yin.domain.SongRating;
import org.apache.log4j.Logger;
import com.example.yin.domain.UMRating;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;


import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestController
public class RankController {

    private static Logger logger = Logger.getLogger(RankController.class.getName());

    @Autowired
    private RankDao rankDao;

    @Autowired
    private JedisPool jp;

    @ResponseBody
    // 获取指定歌曲的评分
    @RequestMapping(value = "/rank", method = RequestMethod.GET)
    public Object getRankByMid(HttpServletRequest req){
        String songId = req.getParameter("songId");
        return new SuccessMessage<Number>(null, rankDao.getMusicAverageScore(Integer.parseInt(songId))).getMessage();
    }

    @RequestMapping(value = "/rank/user", method = RequestMethod.GET)
    public Object getRankByUid(HttpServletRequest req){
        String uid = req.getParameter("uid");
        String mid = req.getParameter("mid");
        double result = rankDao.getRankByUidAndMid(Integer.parseInt(uid), Integer.parseInt(mid));
        if (result == 404){
            return new WarningMessage("没评分");
        }else{
            return new SuccessMessage<Number>(null, result).getMessage();
        }
    }

    @RequestMapping(value = "/rank/add", method = RequestMethod.POST)
    public Object setRating(HttpServletRequest req){
        int uid = Integer.parseInt(req.getParameter("uid").trim());
        int mid = Integer.parseInt(req.getParameter("mid").trim());
        int score = Integer.parseInt(req.getParameter("score").trim());
        int timestamp = Math.abs((int) new Date().getTime());
        SongRating sr = new SongRating(uid,mid,score,timestamp);
        boolean res = rankDao.addRating(sr);
        if (res) {
            System.out.print("=========complete=========");
            logger.info("MUSIC_RATING_PREFIX" + ":" + uid +"|"+ mid +"|"+ score +"|"+ timestamp);
            return new SuccessMessage<ObjectUtils.Null>("评价成功").getMessage();
        } else {
            return new ErrorMessage("评价失败").getMessage();
        }
    }

    @RequestMapping(value = "/testredis", method = RequestMethod.GET)
    public Object testRedis(HttpServletRequest req){
        Jedis jd = jp.getResource();
        return jd.get("uid:1");
    }

}
