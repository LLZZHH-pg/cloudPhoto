package com.lab.study.photomanageservice.controller;

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

    // 假设通过拦截器获取当前登录用户ID，此处默认写死为 1 以作演示
    private final Integer MOCK_USER_ID = 1;

    /**
     * 分页获取图片信息列表（时间轴）
     */
    @GetMapping("/timeline")
    public ResultVo<List<TimelineDTO>> getTimeline(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        List<TimelineDTO> result = pictureService.getTimeline(MOCK_USER_ID, current, size);
        return ResultVo.success(result);
    }

    /**
     * 获取单张照片完整信息
     */
    @GetMapping("/detail/{id}")
    public ResultVo<PictureDTO> getDetail(@PathVariable Integer id) {
        PictureDTO detail = pictureService.getDetail(id);
        return ResultVo.success(detail);
    }

    /**
     * 批量上传图片文件
     */
    @PostMapping("/upload")
    public ResultVo<Void> uploadPictures(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return ResultVo.fail(400, "上传文件不能为空");
        }
        pictureService.uploadPictures(files, MOCK_USER_ID);
        return ResultVo.success();
    }

    /**
     * 批量移入回收站 (软删除)
     */
    @PostMapping("/delete")
    public ResultVo<Void> deletePictures(@RequestBody List<Integer> ids) {
        pictureService.deletePictures(ids);
        return ResultVo.success();
    }

    /**
     * 获取回收站列表
     */
    @GetMapping("/trash/list")
    public ResultVo<List<PictureDTO>> getTrashList() {
        List<PictureDTO> list = pictureService.getTrashList(MOCK_USER_ID);
        return ResultVo.success(list);
    }

    /**
     * 从回收站批量恢复
     */
    @PostMapping("/trash/restore")
    public ResultVo<Void> restorePictures(@RequestBody List<Integer> ids) {
        pictureService.restorePictures(ids);
        return ResultVo.success();
    }

    /**
     * 永久删除
     */
    @DeleteMapping("/trash/clean")
    public ResultVo<Void> cleanTrash(@RequestBody List<Integer> ids) {
        pictureService.cleanTrash(ids);
        return ResultVo.success();
    }


}