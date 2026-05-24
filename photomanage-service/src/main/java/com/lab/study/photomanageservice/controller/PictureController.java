package com.lab.study.photomanageservice.controller;

import com.LAB.study.context.UserContextHolder;
import com.LAB.study.dto.PictureDTO;
import com.LAB.study.dto.TimelineDTO;
import com.lab.study.photomanageservice.service.PictureService;
import com.lab.study.photomanageservice.vo.ResultVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/media")
public class PictureController {

    @Autowired
    private PictureService pictureService;

    private Integer currentUserId() {
//        return 1;
        return UserContextHolder.getCurrentUserId();

    }
    
    @GetMapping("/timeline")
    public ResultVo<List<TimelineDTO>> getTimeline(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        List<TimelineDTO> result = pictureService.getTimeline(currentUserId(), current, size);
        return ResultVo.success(result);
    }
    
    @GetMapping("/detail/{id}")
    public ResultVo<PictureDTO> getDetail(@PathVariable Long id) {
        PictureDTO detail = pictureService.getDetail(id, currentUserId());
        return ResultVo.success(detail);
    }
    
    @PostMapping("/upload")
    public ResultVo<Void> uploadPictures(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResultVo.fail(400, "上传文件不能为空");
        }
        pictureService.uploadPictures(files, currentUserId());
        return ResultVo.success();
    }
    
    @PostMapping("/delete")
    public ResultVo<Void> deletePictures(@RequestBody List<Long> ids) {
        pictureService.deletePictures(ids, currentUserId());
        return ResultVo.success();
    }
    
    @GetMapping("/trash/list")
    public ResultVo<List<PictureDTO>> getTrashList() {
        List<PictureDTO> list = pictureService.getTrashList(currentUserId());
        return ResultVo.success(list);
    }
    
    @PostMapping("/trash/restore")
    public ResultVo<Void> restorePictures(@RequestBody List<Long> ids) {
        pictureService.restorePictures(ids, currentUserId());
        return ResultVo.success();
    }
    
    @DeleteMapping("/trash/clean")
    public ResultVo<Void> cleanTrash(@RequestBody List<Long> ids) {
        pictureService.cleanTrash(ids, currentUserId());
        return ResultVo.success();
    }


}