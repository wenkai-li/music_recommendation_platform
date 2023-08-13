package com.example.yin.dao.mongodb;

import com.example.yin.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class MusicFRDao {

    @Autowired
    private MongoTemplate mongoTemplate;

//    返回所有的Music
    public List<MusicFR> getAllMusic(){
        return mongoTemplate.findAll(MusicFR.class, "Music");
    }

    public List<MusicRM> getByMethod(String method){
        List<MusicRM> mrm = new ArrayList<>();
        switch (method)
        {
            case "近期热门歌曲":
            List<RateRecentMore> rrms = mongoTemplate.findAll(RateRecentMore.class, "RateRecentlyMusic");
                rrms = rrms.stream().sorted(Comparator.comparing(RateRecentMore::getYearMonth, Comparator.nullsFirst(Integer::compareTo)).
                        thenComparing(RateRecentMore::getCount, Comparator.nullsFirst(Integer::compareTo)).reversed()).collect(Collectors.toList());
                for (RateRecentMore rm:rrms) {
                    // 创建条件对象
                    Criteria criteria = Criteria.where("mid").is(rm.getMid());
                    // 创建查询对象，然后将条件对象添加到其中
                    Query query = new Query(criteria);
                    MusicFR musicFR = mongoTemplate.findOne(query, MusicFR.class, "Music");
                    assert musicFR != null;
                    mrm.add(new MusicRM(musicFR.getMid(),musicFR.getName(),musicFR.getIntro(),musicFR.getTag(), musicFR.getPic(),rm.getCount(),rm.getYearMonth()));
                }
            case "历史热门歌曲":
            List<RateMore> rms = mongoTemplate.findAll(RateMore.class, "RateMoreMusic");
                rms = rms.stream().sorted(Comparator.comparing(RateMore::getCount, Comparator.nullsFirst(Integer::compareTo))
                        .reversed()).collect(Collectors.toList());
                for (RateMore rm:rms) {
                    // 创建条件对象
                    Criteria criteria = Criteria.where("mid").is(rm.getMid());
                    // 创建查询对象，然后将条件对象添加到其中
                    Query query = new Query(criteria);
                    MusicFR musicFR = mongoTemplate.findOne(query, MusicFR.class, "Music");
                    assert musicFR != null;
                    mrm.add(new MusicRM(musicFR.getMid(),musicFR.getName(),musicFR.getIntro(),musicFR.getTag(), musicFR.getPic(),rm.getCount()));
                }
        }
        return mrm;
    }

    public List<MusicWS> getByUidWithMethod(int uid, String method){
        List<MusicWS> mws = new ArrayList<>();
        UserRec ur = new UserRec();
        Criteria criteria = Criteria.where("uid").is(uid);
        // 创建查询对象，然后将条件对象添加到其中
        Query query = new Query(criteria);
        if(method.equals("私人推荐")){
            ur = mongoTemplate.findOne(query,UserRec.class, "UserRecs");
        }else if (method.equals("流式推荐")){
            ur = mongoTemplate.findOne(query,UserRec.class, "StreamRecs");
        }
        if (ur != null) {
            List<Rec> recs = ur.getRecs();
            for (Rec r : recs) {
                Criteria newCriteria = Criteria.where("mid").is(r.getMid());
                // 创建查询对象，然后将条件对象添加到其中
                Query newQuery = new Query(newCriteria);
                MusicFR m = mongoTemplate.findOne(newQuery, MusicFR.class, "Music");
                assert m != null;
                mws.add(new MusicWS(m.getMid(), m.getName(), m.getIntro(), m.getTag(), m.getPic(), r.getScore()));
            }
            return mws;
        }else{
            return null;
        }
    }

    public List<MusicWS> getByStyle(String style){
        List<MusicWS> mws = new ArrayList<>();
        Criteria criteria = Criteria.where("uri").is(style);
        // 创建查询对象，然后将条件对象添加到其中
        Query query = new Query(criteria);
        StyleRec sr = mongoTemplate.findOne(query,StyleRec.class, "GenresTopMusic");
        assert sr != null;
        List<Rec> recs = sr.getRec();
        for(Rec r : recs){
            Criteria newCriteria = Criteria.where("mid").is(r.getMid());
            // 创建查询对象，然后将条件对象添加到其中
            Query newQuery = new Query(newCriteria);
            MusicFR m = mongoTemplate.findOne(newQuery, MusicFR.class, "Music");
            assert m != null;
            mws.add(new MusicWS(m.getMid(), m.getName(), m.getIntro(),m.getTag(), m.getPic(),r.getScore()));
        }
        return mws;
    }
}
