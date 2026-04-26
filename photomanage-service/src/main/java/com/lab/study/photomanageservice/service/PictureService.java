package com.lab.study.photomanageservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.LAB.study.dto.PictureDTO;
import com.LAB.study.dto.TimelineDTO;
import com.lab.study.photomanageservice.entity.Picture;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PictureService extends IService<Picture> {

    List<TimelineDTO> getTimeline(Integer userId, long current, long size);

    PictureDTO getDetail(Integer id);

    void uploadPictures(MultipartFile[] files, Integer userId);

    void deletePictures(List<Integer> ids);

    List<PictureDTO> getTrashList(Integer userId);

    void restorePictures(List<Integer> ids);

    void cleanTrash(List<Integer> ids);
}
