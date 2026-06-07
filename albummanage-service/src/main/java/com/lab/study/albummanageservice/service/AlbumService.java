package com.lab.study.albummanageservice.service;

import com.LAB.study.dto.*;
import com.LAB.study.request.AlbumPhotoRequest;
import com.LAB.study.request.AlbumRequest;
import com.LAB.study.request.CopyPhotoRequest;
import com.LAB.study.request.MovePhotoRequest;
import com.LAB.study.vo.AlbumVO;

import java.util.List;
import java.util.Map;

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
    AlbumVO createAlbum(Integer userId, AlbumRequest request);

    /**
     * 删除影集（逻辑删除）
     *
     * @param userId  当前登录用户ID
     * @param albumId 影集ID
     */
    void deleteAlbum(Integer userId, Long albumId);

    /**
     * 更新影集信息
     *
     * @param userId  当前登录用户ID
     * @param albumId 影集ID
     * @param request 更新参数
     * @return 更新后的影集VO
     */
    AlbumVO updateAlbum(Integer userId, Long albumId, AlbumRequest request);

    /**
     * 获取当前用户的影集列表
     *
     * @param userId 当前登录用户ID
     * @return 影集列表
     */
    List<AlbumVO> listAlbums(Integer userId);

    /**
     * 获取影集详情（含照片ID列表）
     *
     * @param userId  当前登录用户ID（用于权限校验）
     * @param albumId 影集ID
     * @return 影集详情VO
     */
    AlbumVO getAlbumDetail(Integer userId, Long albumId);

    /**
     * 向影集添加照片
     *
     * @param userId  当前登录用户ID
     * @param request 影集ID + 照片ID列表
     */
    void addPhotos(Integer userId, AlbumPhotoRequest request);

    /**
     * 从影集移除照片
     *
     * @param userId  当前登录用户ID
     * @param request 影集ID + 照片ID列表
     */
    void removePhotos(Integer userId, AlbumPhotoRequest request);

    /**
     * 移动照片：将照片从源影集移动到目标影集（源影集删除，目标影集新增）
     *
     * @param userId  当前登录用户ID
     * @param request 源影集ID + 照片ID列表 + 目标影集ID
     */
    void movePhotos(Integer userId, MovePhotoRequest request);

    /**
     * 复制照片：将照片从源影集复制到多个目标影集（源影集保留，目标影集新增）
     *
     * @param userId  当前登录用户ID
     * @param request 源影集ID + 照片ID列表 + 目标影集ID列表
     */
    void copyPhotos(Integer userId, CopyPhotoRequest request);


    Map<String, List<PictureDTO>> getPicturesByCategory(Integer userId, boolean onlyFirst);

    void clearPhotosFromAlbums(List<Long> photoIds);

    void updateAlbumPhotoStatus(List<Long> photoIds, Integer status);
}
