package com.lab.study.albummanageservice.service;

import com.lab.study.albummanageservice.dto.AlbumPhotoRequest;
import com.lab.study.albummanageservice.dto.AlbumRequest;
import com.lab.study.albummanageservice.vo.AlbumVO;

import java.util.List;

/**
 * 影集管理 Service 接口
 */
public interface AlbumService {

    /**
     * 创建影集
     *
     * @param userId  当前登录用户ID
     * @param request 创建参数
     * @return 创建后的影集VO
     */
    AlbumVO createAlbum(Long userId, AlbumRequest request);

    /**
     * 删除影集（逻辑删除）
     *
     * @param userId  当前登录用户ID
     * @param albumId 影集ID
     */
    void deleteAlbum(Long userId, Long albumId);

    /**
     * 更新影集信息
     *
     * @param userId  当前登录用户ID
     * @param albumId 影集ID
     * @param request 更新参数
     * @return 更新后的影集VO
     */
    AlbumVO updateAlbum(Long userId, Long albumId, AlbumRequest request);

    /**
     * 获取当前用户的影集列表
     *
     * @param userId 当前登录用户ID
     * @return 影集列表
     */
    List<AlbumVO> listAlbums(Long userId);

    /**
     * 获取影集详情（含照片ID列表）
     *
     * @param userId  当前登录用户ID（用于权限校验）
     * @param albumId 影集ID
     * @return 影集详情VO
     */
    AlbumVO getAlbumDetail(Long userId, Long albumId);

    /**
     * 向影集添加照片
     *
     * @param userId  当前登录用户ID
     * @param albumId 影集ID
     * @param request 照片ID列表
     */
    void addPhotos(Long userId, Long albumId, AlbumPhotoRequest request);

    /**
     * 从影集移除照片
     *
     * @param userId  当前登录用户ID
     * @param albumId 影集ID
     * @param request 照片ID列表
     */
    void removePhotos(Long userId, Long albumId, AlbumPhotoRequest request);

    /**
     * 获取公开影集列表（无需登录）
     *
     * @return 公开影集列表
     */
    List<AlbumVO> listPublicAlbums();
}
