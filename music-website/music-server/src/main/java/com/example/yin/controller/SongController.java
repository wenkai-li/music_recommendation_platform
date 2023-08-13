package com.example.yin.controller;

import com.example.yin.common.FatalMessage;
import com.example.yin.common.ErrorMessage;
import com.example.yin.common.SuccessMessage;
import com.example.yin.constant.Constants;
import com.example.yin.dao.mongodb.MusicFRDao;
import com.example.yin.domain.*;
import com.example.yin.service.impl.SongServiceImpl;

import org.apache.commons.lang3.ObjectUtils.Null;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;
import org.springframework.util.unit.DataUnit;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.servlet.MultipartConfigElement;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

@RestController
public class SongController {

    @Autowired
    private SongServiceImpl songService;

    @Autowired
    private MusicFRDao musicFRDao;

    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        // 文件最大10M,DataUnit提供5中类型B,KB,MB,GB,TB
        factory.setMaxFileSize(DataSize.of(20, DataUnit.MEGABYTES));
        // 设置总上传数据总大小10M
        factory.setMaxRequestSize(DataSize.of(20, DataUnit.MEGABYTES));
        return factory.createMultipartConfig();
    }

    @Configuration
    public static class MyPicConfig implements WebMvcConfigurer {
        @Override
        public void addResourceHandlers(ResourceHandlerRegistry registry) {
            registry.addResourceHandler("/img/songPic/**")
                    .addResourceLocations(Constants.SONG_PIC_PATH);
            registry.addResourceHandler("/song/**")
                    .addResourceLocations(Constants.SONG_PATH);
        }
    }

    // 添加歌曲
    @ResponseBody
    @RequestMapping(value = "/song/add", method = RequestMethod.POST)
    public Object addSong(HttpServletRequest req, @RequestParam("file") MultipartFile mpfile) {
        String singer_id = req.getParameter("singerId").trim();
        String name = req.getParameter("name").trim();
        String introduction = req.getParameter("introduction").trim();
        String pic = "/img/songPic/tubiao.jpg";
        String lyric = req.getParameter("lyric").trim();

        String fileName = mpfile.getOriginalFilename();
        String filePath = System.getProperty("user.dir") + System.getProperty("file.separator") + "song";
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }

        File dest = new File(filePath + System.getProperty("file.separator") + fileName);
        String storeUrlPath = "/song/" + fileName;
        try {
            mpfile.transferTo(dest);
            Song song = new Song();
            song.setSingerId(Integer.parseInt(singer_id));
            song.setName(name);
            song.setIntroduction(introduction);
            song.setCreateTime(new Date());
            song.setUpdateTime(new Date());
            song.setPic(pic);
            song.setLyric(lyric);
            song.setUrl(storeUrlPath);
            boolean res = songService.addSong(song);
            if (res) {
                return new SuccessMessage<String>("上传成功", storeUrlPath).getMessage();
            } else {
                return new ErrorMessage("上传失败").getMessage();
            }
        } catch (IOException e) {
            return new FatalMessage("上传失败" + e.getMessage()).getMessage();
        }
    }

    // 删除歌曲
    @RequestMapping(value = "/song/delete", method = RequestMethod.GET)
    public Object deleteSong(HttpServletRequest req) {
        String id = req.getParameter("id");

        boolean res = songService.deleteSong(Integer.parseInt(id));
        if (res) {
            return new SuccessMessage<Null>("删除成功").getMessage();
        } else {
            return new ErrorMessage("删除失败").getMessage();
        }
    }

    // 返回所有歌曲
    @RequestMapping(value = "/song", method = RequestMethod.GET)
    public Object allSong() {
        return new SuccessMessage<List<Song>>(null, songService.allSong()).getMessage();
    }
    //  返回推荐相关的歌曲信息
    @RequestMapping(value = "/song/recommendation", method = RequestMethod.GET)
    public Object allSongFromRecommendation() {
        return new SuccessMessage<List<MusicFR>>(null, musicFRDao.getAllMusic()).getMessage();
    }

    //  返回不同推荐方法的结果
    @RequestMapping(value = "/song/recommendation/detail", method = RequestMethod.GET)
    public Object songListByMethod(HttpServletRequest req) {
        String method = req.getParameter("method");
        if ("近期热门歌曲".equals(method) || "历史热门歌曲".equals(method)){
            if(musicFRDao.getByMethod(method) != null){
                return new SuccessMessage<List<MusicRM>>(null, musicFRDao.getByMethod(method)).getMessage();
            }
            else {
                return new ErrorMessage("Unverified method!!!");
            }
        }else {
            if(musicFRDao.getByStyle(method) != null){
                return new SuccessMessage<List<MusicWS>>(null, musicFRDao.getByStyle(method)).getMessage();
            }
            else {
                return new ErrorMessage("Unverified style!!!");
            }
        }

    }


    //  根据用户id完成私人推荐
    @RequestMapping(value = "/song/recommendation/self", method = RequestMethod.GET)
    public Object getByUid(HttpServletRequest req) {
        String uid = req.getParameter("uid");
        String method = req.getParameter("method");
        if(musicFRDao.getByUidWithMethod(Integer.parseInt(uid),method) != null){
            return new SuccessMessage<List<MusicWS>>(null, musicFRDao.getByUidWithMethod(Integer.parseInt(uid),method)).getMessage();
        }else if(method.equals("私人推荐")){
            return new ErrorMessage("私人推荐基于长期评分结果进行推荐，请继续评分耐心等待。").getMessage();
        }else {
            return new ErrorMessage("流式推荐基于音乐内容本身，请对一首音乐进行评价。").getMessage();
        }
    }


    // 返回指定歌曲ID的歌曲
    @RequestMapping(value = "/song/detail", method = RequestMethod.GET)
    public Object songOfId(HttpServletRequest req) {
        String id = req.getParameter("id");

        return new SuccessMessage<List<Song>>(null, songService.songOfId(Integer.parseInt(id))).getMessage();
    }

    // 返回指定歌手ID的歌曲
    @RequestMapping(value = "/song/singer/detail", method = RequestMethod.GET)
    public Object songOfSingerId(HttpServletRequest req) {
        String singerId = req.getParameter("singerId");

        return new SuccessMessage<List<Song>>(null, songService.songOfSingerId(Integer.parseInt(singerId))).getMessage();
    }

    // 返回指定歌手名的歌曲
    @RequestMapping(value = "/song/singerName/detail", method = RequestMethod.GET)
    public Object songOfSingerName(HttpServletRequest req) {
        String name = req.getParameter("name");

        return new SuccessMessage<List<Song>>(null, songService.songOfSingerName('%' + name + '%')).getMessage();
    }

    // 更新歌曲信息
    @ResponseBody
    @RequestMapping(value = "/song/update", method = RequestMethod.POST)
    public Object updateSongMsg(HttpServletRequest req) {
        String id = req.getParameter("id").trim();
        String singer_id = req.getParameter("singerId").trim();
        String name = req.getParameter("name").trim();
        String introduction = req.getParameter("introduction").trim();
        String lyric = req.getParameter("lyric").trim();

        Song song = new Song();
        song.setId(Integer.parseInt(id));
        song.setSingerId(Integer.parseInt(singer_id));
        song.setName(name);
        song.setIntroduction(introduction);
        song.setUpdateTime(new Date());
        song.setLyric(lyric);

        boolean res = songService.updateSongMsg(song);
        if (res) {
            return new SuccessMessage<Null>("修改成功").getMessage();
        } else {
            return new ErrorMessage("修改失败").getMessage();
        }
    }

    // 更新歌曲图片
    @ResponseBody
    @RequestMapping(value = "/song/img/update", method = RequestMethod.POST)
    public Object updateSongPic(@RequestParam("file") MultipartFile urlFile, @RequestParam("id") int id) {
        String fileName = System.currentTimeMillis() + urlFile.getOriginalFilename();
        String filePath = System.getProperty("user.dir") + System.getProperty("file.separator") + "img" + System.getProperty("file.separator") + "songPic";
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }

        File dest = new File(filePath + System.getProperty("file.separator") + fileName);
        String storeUrlPath = "/img/songPic/" + fileName;
        try {
            urlFile.transferTo(dest);
            Song song = new Song();
            song.setId(id);
            song.setPic(storeUrlPath);
            boolean res = songService.updateSongPic(song);
            if (res) {
                return new SuccessMessage<String>("上传成功", storeUrlPath).getMessage();
            } else {
                return new ErrorMessage("上传失败").getMessage();
            }
        } catch (IOException e) {
            return new FatalMessage("上传失败" + e.getMessage()).getMessage();
        }
    }

    // 更新歌曲
    @ResponseBody
    @RequestMapping(value = "/song/url/update", method = RequestMethod.POST)
    public Object updateSongUrl(@RequestParam("file") MultipartFile urlFile, @RequestParam("id") int id) {
        String fileName = urlFile.getOriginalFilename();
        String filePath = System.getProperty("user.dir") + System.getProperty("file.separator") + "song";
        File file1 = new File(filePath);
        if (!file1.exists()) {
            file1.mkdir();
        }

        File dest = new File(filePath + System.getProperty("file.separator") + fileName);
        String storeUrlPath = "/song/" + fileName;
        try {
            urlFile.transferTo(dest);
            Song song = new Song();
            song.setId(id);
            song.setUrl(storeUrlPath);
            boolean res = songService.updateSongUrl(song);
            if (res) {
                return new SuccessMessage<String>("更新成功", storeUrlPath).getMessage();
            } else {
                return new ErrorMessage("更新失败").getMessage();
            }
        } catch (IOException e) {
            return new FatalMessage("更新失败" + e.getMessage()).getMessage();
        }
    }
}
