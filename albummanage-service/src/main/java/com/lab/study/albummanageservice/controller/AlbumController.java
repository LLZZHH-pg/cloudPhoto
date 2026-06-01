package com.lab.study.albummanageservice.controller;

import com.LAB.study.context.UserContextHolder;
import com.LAB.study.result.Result;
import com.LAB.study.dto.AlbumPhotoRequest;
import com.LAB.study.dto.AlbumRequest;
import com.LAB.study.dto.CopyPhotoRequest;
import com.LAB.study.dto.MovePhotoRequest;
import com.lab.study.albummanageservice.service.AlbumService;
import com.LAB.study.vo.AlbumVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 影集管理 Controller
 *
 * 接口列表：
 *   POST   /album/create              创建影集
 *   DELETE /album/{id}                删除影集
 *   PUT    /album/{id}                更新影集信息
 *   GET    /album/list                查询我的影集列表
 *   GET    /album/{id}                查询影集详情（含照片ID）
 *   POST   /album/photos/add          向影集添加照片
 *   POST   /album/photos/remove       从影集移除照片
 *   POST   /album/photos/move         移动照片到其他影集
 *   POST   /album/photos/copy         复制照片到其他影集
 *   GET    /album/public              查询所有公开影集
 */
@RestController
@RequestMapping("/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private Integer currentUserId() {
        return UserContextHolder.getCurrentUserId();
    }

    // ─────────────────────────────────────────────────────────
    // 创建影集
    // ─────────────────────────────────────────────────────────
    @PostMapping("/create")
    public Result<AlbumVO> createAlbum(
            @RequestBody AlbumRequest request) {
        AlbumVO vo = albumService.createAlbum(currentUserId(), request);
        return Result.success(vo);
    }

    // ─────────────────────────────────────────────────────────
    // 删除影集
    // ─────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Result<Void> deleteAlbum(
            @PathVariable Long id) {
        albumService.deleteAlbum(currentUserId(), id);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 更新影集信息
    // ─────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public Result<AlbumVO> updateAlbum(
            @PathVariable Long id,
            @RequestBody AlbumRequest request) {
        AlbumVO vo = albumService.updateAlbum(currentUserId(), id, request);
        return Result.success(vo);
    }

    // ─────────────────────────────────────────────────────────
    // 查询我的影集列表
    // ─────────────────────────────────────────────────────────
    @GetMapping("/list")
    public Result<List<AlbumVO>> listAlbums() {
        List<AlbumVO> list = albumService.listAlbums(currentUserId());
        return Result.success(list);
    }

    // ─────────────────────────────────────────────────────────
    // 查询影集详情（含照片ID列表）
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public Result<AlbumVO> getAlbumDetail(
            @PathVariable Long id) {
        AlbumVO vo = albumService.getAlbumDetail(currentUserId(), id);
        return Result.success(vo);
    }

    // ─────────────────────────────────────────────────────────
    // 向影集添加照片
    // ─────────────────────────────────────────────────────────
    @PostMapping("/photos/add")
    public Result<Void> addPhotos(
            @RequestBody AlbumPhotoRequest request) {
        albumService.addPhotos(currentUserId(), request);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 从影集移除照片
    // ─────────────────────────────────────────────────────────
    @PostMapping("/photos/remove")
    public Result<Void> removePhotos(
            @RequestBody AlbumPhotoRequest request) {
        albumService.removePhotos(currentUserId(), request);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 移动照片到其他影集
    // ─────────────────────────────────────────────────────────
    @PostMapping("/photos/move")
    public Result<Void> movePhotos(
            @RequestBody MovePhotoRequest request) {
        albumService.movePhotos(currentUserId(), request);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 复制照片到其他影集
    // ─────────────────────────────────────────────────────────
    @PostMapping("/photos/copy")
    public Result<Void> copyPhotos(
            @RequestBody CopyPhotoRequest request) {
        albumService.copyPhotos(currentUserId(), request);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 查询所有公开影集（不需要登录）
    // ─────────────────────────────────────────────────────────
    @GetMapping("/public")
    public Result<List<AlbumVO>> listPublicAlbums() {
        List<AlbumVO> list = albumService.listPublicAlbums();
        return Result.success(list);
    }

    // 内部接口
    @DeleteMapping("/internal/photos/clear")
    public Result<Void> clearPhotosFromAlbums(@RequestParam("photoIds") List<Long> photoIds) {
        if (photoIds != null && !photoIds.isEmpty()) {
            albumService.clearPhotosFromAlbums(photoIds);
        }
        return Result.success();
    }

    
}
