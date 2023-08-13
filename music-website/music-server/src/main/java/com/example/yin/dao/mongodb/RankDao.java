package com.example.yin.dao.mongodb;

import com.example.yin.domain.MusicAvg;
import com.example.yin.domain.SongRating;
import com.example.yin.domain.UMRating;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

@Component
public class RankDao {

    @Autowired
    private MongoTemplate mongoTemplate;


    // 获取歌曲的平均得分
    public double getMusicAverageScore(int mid){
        Criteria criteria = Criteria.where("mid").is(mid);
        // 创建查询对象，然后将条件对象添加到其中
        Query query = new Query(criteria);
        MusicAvg ma = mongoTemplate.findOne(query,MusicAvg.class, "AverageMusic");
        if(ma == null){
            return 0;
        }
        return ma.getAvg();
    }

    // 获取用户对歌曲的评分
    public double getRankByUidAndMid(int uid, int mid){
        Criteria criteria = new Criteria();
        criteria.andOperator(Criteria.where("uid").is(uid),Criteria.where("mid").is(mid));
        // 创建查询对象，然后将条件对象添加到其中
        Query query = new Query(criteria);
        UMRating umr = mongoTemplate.findOne(query, UMRating.class, "Rating");
        if(umr == null){
            return 404;
        }
        return umr.getScore();
    }

    public boolean addRating(SongRating songRating){
        mongoTemplate.insert(songRating, "Rating");
        return true;
    }
}
