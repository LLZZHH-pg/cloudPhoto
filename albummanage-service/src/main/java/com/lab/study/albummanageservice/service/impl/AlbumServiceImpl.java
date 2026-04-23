package com.lab.study.albummanageservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.lab.study.albummanageservice.dto.AlbumPhotoRequest;
import com.lab.study.albummanageservice.dto.AlbumRequest;
import com.lab.study.albummanageservice.entity.Album;
import com.lab.study.albummanageservice.entity.AlbumPhoto;
import com.lab.study.albummanageservice.mapper.AlbumMapper;
import com.lab.study.albummanageservice.mapper.AlbumPhotoMapper;
import com.lab.study.albummanageservice.service.AlbumService;
import com.lab.study.albummanageservice.vo.AlbumVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 影集管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumMapper albumMapper;
    private final AlbumPhotoMapper albumPhotoMapper;

    // ─────────────────────────────────────────────────────────
    // 创建影集
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO createAlbum(Long userId, AlbumRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("影集名称不能为空");
        }

        Album album = new Album();
        album.setUserId(userId);
        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setCoverUrl(request.getCoverUrl());
        album.setIsPublic(request.getIsPublic() != null ? request.getIsPublic() : 0);
        album.setPhotoCount(0);

        albumMapper.insert(album);
        return toVO(album, null);
    }

    // ─────────────────────────────────────────────────────────
    // 删除影集（逻辑删除，同时清理关联照片记录）
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Long userId, Long albumId) {
        Album album = getAlbumAndCheckOwner(userId, albumId);

        // 逻辑删除影集
        albumMapper.deleteById(albumId);

        // 物理删除关联照片记录（关联表无需逻辑删除）
        albumPhotoMapper.delete(
                new LambdaQueryWrapper<AlbumPhoto>()
                        .eq(AlbumPhoto::getAlbumId, albumId)
        );
    }

    // ─────────────────────────────────────────────────────────
    // 更新影集信息
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO updateAlbum(Long userId, Long albumId, AlbumRequest request) {
        Album album = getAlbumAndCheckOwner(userId, albumId);

        if (request.getName() != null && !request.getName().isBlank()) {
            album.setName(request.getName());
        }
        if (request.getDescription() != null) {
            album.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            album.setCoverUrl(request.getCoverUrl());
        }
        if (request.getIsPublic() != null) {
            album.setIsPublic(request.getIsPublic());
        }

        albumMapper.updateById(album);
        return toVO(album, null);
    }

    // ─────────────────────────────────────────────────────────
    // 查询当前用户的影集列表
    // ─────────────────────────────────────────────────────────
    @Override
    public List<AlbumVO> listAlbums(Long userId) {
        List<Album> albums = albumMapper.selectList(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getUserId, userId)
                        .orderByDesc(Album::getCreatedAt)
        );
        return albums.stream()
                .map(a -> toVO(a, null))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    // 获取影集详情（含照片ID列表，公开影集任何人可看）
    // ─────────────────────────────────────────────────────────
    @Override
    public AlbumVO getAlbumDetail(Long userId, Long albumId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new RuntimeException("影集不存在");
        }
        // 私有影集只有主人可以查看
        if (album.getIsPublic() == 0 && !album.getUserId().equals(userId)) {
            throw new RuntimeException("无权限查看该影集");
        }

        List<Long> photoIds = albumPhotoMapper.selectPhotoIdsByAlbumId(albumId);
        return toVO(album, photoIds);
    }

    // ─────────────────────────────────────────────────────────
    // 向影集添加照片（去重处理）
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPhotos(Long userId, Long albumId, AlbumPhotoRequest request) {
        getAlbumAndCheckOwner(userId, albumId);

        if (request.getPhotoIds() == null || request.getPhotoIds().isEmpty()) {
            return;
        }

        // 查询已存在的关联，避免重复插入
        List<Long> existingPhotoIds = albumPhotoMapper.selectPhotoIdsByAlbumId(albumId);

        List<AlbumPhoto> toInsert = request.getPhotoIds().stream()
                .filter(photoId -> !existingPhotoIds.contains(photoId))
                .map(photoId -> {
                    AlbumPhoto ap = new AlbumPhoto();
                    ap.setAlbumId(albumId);
                    ap.setPhotoId(photoId);
                    return ap;
                })
                .collect(Collectors.toList());

        // 批量插入新照片关联
        for (AlbumPhoto ap : toInsert) {
            albumPhotoMapper.insert(ap);
        }

        // 更新影集照片计数
        updatePhotoCount(albumId);
    }

    // ─────────────────────────────────────────────────────────
    // 从影集移除照片
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePhotos(Long userId, Long albumId, AlbumPhotoRequest request) {
        getAlbumAndCheckOwner(userId, albumId);

        if (request.getPhotoIds() == null || request.getPhotoIds().isEmpty()) {
            return;
        }

        albumPhotoMapper.delete(
                new LambdaQueryWrapper<AlbumPhoto>()
                        .eq(AlbumPhoto::getAlbumId, albumId)
                        .in(AlbumPhoto::getPhotoId, request.getPhotoIds())
        );

        updatePhotoCount(albumId);
    }

    // ─────────────────────────────────────────────────────────
    // 获取所有公开影集
    // ─────────────────────────────────────────────────────────
    @Override
    public List<AlbumVO> listPublicAlbums() {
        List<Album> albums = albumMapper.selectList(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getIsPublic, 1)
                        .orderByDesc(Album::getCreatedAt)
        );
        return albums.stream()
                .map(a -> toVO(a, null))
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────
    // 私有工具方法
    // ─────────────────────────────────────────────────────────

    /**
     * 查询影集，并校验是否属于该用户（权限验证）
     */
    private Album getAlbumAndCheckOwner(Long userId, Long albumId) {
        Album album = albumMapper.selectById(albumId);
        if (album == null) {
            throw new RuntimeException("影集不存在");
        }
        if (!album.getUserId().equals(userId)) {
            throw new RuntimeException("无权限操作该影集");
        }
        return album;
    }

    /**
     * 更新影集照片数量（根据关联表实际计数）
     */
    private void updatePhotoCount(Long albumId) {
        Long count = albumPhotoMapper.selectCount(
                new LambdaQueryWrapper<AlbumPhoto>()
                        .eq(AlbumPhoto::getAlbumId, albumId)
        );
        albumMapper.update(null,
                new LambdaUpdateWrapper<Album>()
                        .eq(Album::getId, albumId)
                        .set(Album::getPhotoCount, count)
        );
    }

    /**
     * Album 实体 → AlbumVO 转换
     */
    private AlbumVO toVO(Album album, List<Long> photoIds) {
        AlbumVO vo = new AlbumVO();
        BeanUtils.copyProperties(album, vo);
        vo.setPhotoIds(photoIds);
        return vo;
    }
}
