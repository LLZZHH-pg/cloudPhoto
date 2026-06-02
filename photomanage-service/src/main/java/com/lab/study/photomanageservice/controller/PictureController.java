package com.lab.study.photomanageservice.controller;

import com.LAB.study.context.UserContextHolder;
import com.LAB.study.dto.PictureDTO;
import com.LAB.study.request.UpdateCategoryRequest;
import com.LAB.study.vo.PictureVO;
import com.LAB.study.vo.TimelineVO;
import com.LAB.study.result.Result;
import com.lab.study.photomanageservice.service.PictureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/media")
public class PictureController {

    @Autowired
    private PictureService pictureService;

    private Integer currentUserId() {
        return UserContextHolder.getCurrentUserId();
    }
    
    @GetMapping("/timeline")
    public Result<List<TimelineVO>> getTimeline(
            @RequestParam(defaultValue = "1") long current,
            @RequestParam(defaultValue = "20") long size) {
        List<TimelineVO> result = pictureService.getTimeline(currentUserId(), current, size);
        return Result.success(result);
    }
    
    @GetMapping("/detail/{id}")
    public Result<PictureVO> getDetail(@PathVariable Long id) {
        PictureVO detail = pictureService.getDetail(id, currentUserId());
        return Result.success(detail);
    }
    
    @PostMapping("/upload")
    public Result<Void> uploadPictures(@RequestParam("files") MultipartFile[] files) {
        if (files == null || files.length == 0) {
            return Result.error(400, "上传文件不能为空");
        }
        pictureService.uploadPictures(files, currentUserId());
        return Result.success();
    }

    @PostMapping("/download/batch")
    public Result<List<String>> getBatchDownloadUrls(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error(400, "请选择要下载的图片");
        }
        List<String> downloadUrls = pictureService.getDownloadUrls(ids, currentUserId());
        return Result.success(downloadUrls);
    }
    
    @PostMapping("/delete")
    public Result<Void> deletePictures(@RequestBody List<Long> ids) {
        pictureService.deletePictures(ids, currentUserId());
        return Result.success();
    }
    
    @GetMapping("/trash/list")
    public Result<List<PictureVO>> getTrashList() {
        List<PictureVO> list = pictureService.getTrashList(currentUserId());
        return Result.success(list);
    }
    
    @PostMapping("/trash/restore")
    public Result<Void> restorePictures(@RequestBody List<Long> ids) {
        pictureService.restorePictures(ids, currentUserId());
        return Result.success();
    }
    
    @DeleteMapping("/trash/clean")
    public Result<Void> cleanTrash(@RequestBody List<Long> ids) {
        pictureService.cleanTrash(ids, currentUserId());
        return Result.success();
    }

    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        List<String> categories = pictureService.getAllCategories(currentUserId());
        return Result.success(categories);
    }

    @PostMapping("/category/update")
    public Result<Void> updatePicturesCategory(@RequestBody UpdateCategoryRequest request) {
        if (request.getIds() == null || request.getIds().isEmpty()) {
            return Result.error(400, "请选择要修改的照片");
        }
        pictureService.updatePicturesCategory(request.getIds(), request.getCategory(), currentUserId());
        return Result.success();
    }


    @PostMapping("/internal/getphoto")
    public Result<List<PictureDTO>> getPicturesByIds(@RequestBody List<Long> ids) {
        List<PictureDTO> list = pictureService.getPicturesByIds(ids);
        return Result.success(list);
    }

    @GetMapping("/internal/category/first")
    public Result<Map<String, List<PictureDTO>>> getCategoryFirstGroup(@RequestParam("userId") Integer userId) {
        // 按分类聚合，只拿第一条
        Map<String, List<PictureDTO>> result = pictureService.getPicturesGroupedByCategory(userId, true);
        return Result.success(result);
    }

    @GetMapping("/internal/category/all")
    public Result<Map<String, List<PictureDTO>>> getCategoryAllGroup(@RequestParam("userId") Integer userId) {
        // 按分类聚合，拿该分类下所有图片
        Map<String, List<PictureDTO>> result = pictureService.getPicturesGroupedByCategory(userId, false);
        return Result.success(result);
    }

}