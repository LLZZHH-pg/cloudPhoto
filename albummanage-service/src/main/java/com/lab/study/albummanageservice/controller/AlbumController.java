package com.lab.study.albummanageservice.controller;

import com.LAB.study.context.UserContextHolder;
import com.lab.study.albummanageservice.common.Result;
import com.lab.study.albummanageservice.dto.AlbumPhotoRequest;
import com.lab.study.albummanageservice.dto.AlbumRequest;
import com.lab.study.albummanageservice.service.AlbumService;
import com.lab.study.albummanageservice.vo.AlbumVO;
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
 *   POST   /album/{id}/photos/add     向影集添加照片
 *   POST   /album/{id}/photos/remove  从影集移除照片
 *   GET    /album/public              查询所有公开影集
 */
@RestController
@RequestMapping("/album")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;
    private Integer currentUserId() {
        try {
            return UserContextHolder.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────
    // 创建影集
    // ─────────────────────────────────────────────────────────
    @PostMapping("/create")
    public Result<AlbumVO> createAlbum(
            @RequestBody AlbumRequest request) {
        Integer userId = currentUserId();
        if (userId == null) return Result.unauthorized();
        AlbumVO vo = albumService.createAlbum(userId, request);
        return Result.success(vo);
    }

    // ─────────────────────────────────────────────────────────
    // 删除影集
    // ─────────────────────────────────────────────────────────
    @DeleteMapping("/{id}")
    public Result<Void> deleteAlbum(
            @PathVariable Long id) {
        Integer userId = currentUserId();
        if (userId == null) return Result.unauthorized();
        albumService.deleteAlbum(userId, id);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 更新影集信息
    // ─────────────────────────────────────────────────────────
    @PutMapping("/{id}")
    public Result<AlbumVO> updateAlbum(
            @PathVariable Long id,
            @RequestBody AlbumRequest request) {
        Integer userId = currentUserId();
        if (userId == null) return Result.unauthorized();
        AlbumVO vo = albumService.updateAlbum(userId, id, request);
        return Result.success(vo);
    }

    // ─────────────────────────────────────────────────────────
    // 查询我的影集列表
    // ─────────────────────────────────────────────────────────
    @GetMapping("/list")
    public Result<List<AlbumVO>> listAlbums() {
        Integer userId = currentUserId();
        if (userId == null) return Result.unauthorized();
        List<AlbumVO> list = albumService.listAlbums(userId);
        return Result.success(list);
    }

    // ─────────────────────────────────────────────────────────
    // 查询影集详情（含照片ID列表）
    // ─────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public Result<AlbumVO> getAlbumDetail(
            @PathVariable Long id) {
        Integer userId = currentUserId();
        AlbumVO vo = albumService.getAlbumDetail(userId, id);
        return Result.success(vo);
    }

    // ─────────────────────────────────────────────────────────
    // 向影集添加照片
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/photos/add")
    public Result<Void> addPhotos(
            @PathVariable Long id,
            @RequestBody AlbumPhotoRequest request) {
        Integer userId = currentUserId();
        if (userId == null) return Result.unauthorized();
        albumService.addPhotos(userId, id, request);
        return Result.success();
    }

    // ─────────────────────────────────────────────────────────
    // 从影集移除照片
    // ─────────────────────────────────────────────────────────
    @PostMapping("/{id}/photos/remove")
    public Result<Void> removePhotos(
            @PathVariable Long id,
            @RequestBody AlbumPhotoRequest request) {
        Integer userId = currentUserId();
        if (userId == null) return Result.unauthorized();
        albumService.removePhotos(userId, id, request);
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

    
}
