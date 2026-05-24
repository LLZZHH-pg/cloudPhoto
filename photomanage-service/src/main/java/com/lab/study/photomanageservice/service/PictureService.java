package com.lab.study.photomanageservice.service;

import com.LAB.study.dto.PictureDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.LAB.study.vo.PictureVO;
import com.LAB.study.vo.TimelineVO;
import com.lab.study.photomanageservice.entity.Picture;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PictureService extends IService<Picture> {

    List<TimelineVO> getTimeline(Integer userId, long current, long size);

    PictureVO getDetail(Long id, Integer userId);

    void uploadPictures(MultipartFile[] files, Integer userId);

    void deletePictures(List<Long> ids, Integer userId);

    List<PictureVO> getTrashList(Integer userId);

    void restorePictures(List<Long> ids, Integer userId);

    void cleanTrash(List<Long> ids, Integer userId);

    List<PictureDTO> getPicturesByIds(List<Long> ids);
}
