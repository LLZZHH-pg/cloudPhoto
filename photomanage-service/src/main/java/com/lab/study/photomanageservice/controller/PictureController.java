package com.lab.study.photomanageservice.controller;

import com.lab.study.photomanageservice.service.PictureService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 图片管理模块控制器
 * 提供图片时间线、详情、上传、回收站等 RESTful API
 */
@RestController
@RequestMapping("/media")
public class PictureController {

    @Autowired
    private PictureService pictureService;

// ==================== 内部路由 ====================

    /**
     * 获取时间线（分页 + 按拍摄日期分组）
     */
    @GetMapping("/timeline")
    public ResponseEntity<Object> getTimeline(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Object result = pictureService.getTimeline(page, size);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取单张照片详情（含 EXIF 和高清预览图）
     */
    @GetMapping("/detail/{id}")
    public ResponseEntity<Object> getDetail(@PathVariable("id") Integer pictureId) {
        Object detail = pictureService.getDetail(pictureId);
        return ResponseEntity.ok(detail);
    }

    /**
     * 批量上传图片
     * userId 可通过 Token 解析获得，这里简化处理由客户端传入
     */
    @PostMapping("/upload")
    public ResponseEntity<Object> upload(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(required = false) Integer userId) {
        // TODO: 从 SecurityContextHolder 获取真实 userId
        Object uploadResult = pictureService.upload(files, userId);
        return ResponseEntity.ok(uploadResult);
    }

    /**
     * 批量删除（移入回收站，设置 delete_time）
     */
    @PostMapping("/delete")
    public ResponseEntity<Void> deletePictures(@RequestBody List<Integer> pictureIds) {
        pictureService.deletePictures(pictureIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 获取回收站列表（delete_time 不为 null 的图片）
     */
    @GetMapping("/trash/list")
    public ResponseEntity<Object> getTrashList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        Object trashList = pictureService.getTrashList(page, size);
        return ResponseEntity.ok(trashList);
    }

    /**
     * 批量恢复回收站图片（清空 delete_time）
     */
    @PostMapping("/trash/restore")
    public ResponseEntity<Void> restorePictures(@RequestBody List<Integer> pictureIds) {
        pictureService.restorePictures(pictureIds);
        return ResponseEntity.ok().build();
    }

    /**
     * 彻底删除（数据库 + 七牛云文件）
     */
    @DeleteMapping("/trash/clean")
    public ResponseEntity<Void> cleanTrash(@RequestBody List<Integer> pictureIds) {
        pictureService.cleanTrash(pictureIds);
        return ResponseEntity.ok().build();
    }

// ==================== OpenFeign 路由====================

}