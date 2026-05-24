package com.lab.study.photomanageservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.LAB.study.dto.PictureDTO;
import com.LAB.study.dto.TimelineDTO;
import com.lab.study.photomanageservice.entity.Picture;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PictureService extends IService<Picture> {

    List<TimelineDTO> getTimeline(Integer userId, long current, long size);

    PictureDTO getDetail(Long id, Integer userId);

    void uploadPictures(MultipartFile[] files, Integer userId);

    void deletePictures(List<Long> ids, Integer userId);

    List<PictureDTO> getTrashList(Integer userId);

    void restorePictures(List<Long> ids, Integer userId);

    void cleanTrash(List<Long> ids, Integer userId);
}
