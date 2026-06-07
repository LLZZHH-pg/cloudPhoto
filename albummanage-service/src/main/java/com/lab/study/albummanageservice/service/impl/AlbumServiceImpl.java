package com.lab.study.albummanageservice.service.impl;

import com.LAB.study.dto.PictureDTO;
import com.LAB.study.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.LAB.study.request.AlbumPhotoRequest;
import com.LAB.study.request.AlbumRequest;
import com.LAB.study.request.CopyPhotoRequest;
import com.LAB.study.request.MovePhotoRequest;
import com.lab.study.albummanageservice.entity.Album;
import com.lab.study.albummanageservice.entity.AlbumPhoto;
import com.lab.study.albummanageservice.feign.PhotoFeign;
import com.lab.study.albummanageservice.mapper.AlbumMapper;
import com.lab.study.albummanageservice.mapper.AlbumPhotoMapper;
import com.lab.study.albummanageservice.service.AlbumService;
import com.LAB.study.vo.AlbumVO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 影集管理 Service 实现
 */
@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumMapper albumMapper;
    private final AlbumPhotoMapper albumPhotoMapper;
    private final PhotoFeign photoFeign;

    // ─────────────────────────────────────────────────────────
    // 创建影集
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO createAlbum(Integer userId, AlbumRequest request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new RuntimeException("影集名称不能为空");
        }

        Long count = albumMapper.selectCount(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getUserId, userId)
                        .eq(Album::getName, request.getName())
        );
        if (count > 0) {
            throw new RuntimeException("已存在同名影集");
        }

        Album album = new Album();
        album.setUserId(userId);
        album.setName(request.getName());
        album.setDescription(request.getDescription());
        album.setCoverUrl(request.getCoverUrl());

        albumMapper.insert(album);
        return toVO(album, null);
    }

    // ─────────────────────────────────────────────────────────
    // 删除影集（逻辑删除，同时清理关联照片记录）
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAlbum(Integer userId, Long albumId) {
        getAlbumAndCheckOwner(userId, albumId);
        albumMapper.deleteById(albumId);
    }

    // ─────────────────────────────────────────────────────────
    // 更新影集信息
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AlbumVO updateAlbum(Integer userId, Long albumId, AlbumRequest request) {
        Album album = getAlbumAndCheckOwner(userId, albumId);

        if (request.getName() != null && !request.getName().isBlank()
                && !request.getName().equals(album.getName())) {
            Long count = albumMapper.selectCount(
                    new LambdaQueryWrapper<Album>()
                            .eq(Album::getUserId, userId)
                            .eq(Album::getName, request.getName())
            );
            if (count > 0) {
                throw new RuntimeException("已存在同名影集");
            }
            album.setName(request.getName());
        }
        if (request.getDescription() != null) {
            album.setDescription(request.getDescription());
        }
        if (request.getCoverUrl() != null) {
            album.setCoverUrl(request.getCoverUrl());
        }

        albumMapper.updateById(album);
        return toVO(album, null);
    }

    // ─────────────────────────────────────────────────────────
    // 查询当前用户的影集列表
    // ─────────────────────────────────────────────────────────
    @Override
    public List<AlbumVO> listAlbums(Integer userId) {
        List<Album> albums = albumMapper.selectList(
                new LambdaQueryWrapper<Album>()
                        .eq(Album::getUserId, userId)
                        .orderByDesc(Album::getCreatedAt)
        );
        if (albums.isEmpty()) return List.of();

        List<AlbumVO> voList = albums.stream().map(a -> toVO(a, null)).collect(Collectors.toList());
        List<Long> albumIds = albums.stream().map(Album::getId).collect(Collectors.toList());

        // 1. 获取所有关联表中未被软删的记录
        List<AlbumPhoto> allRelations = albumPhotoMapper.selectList(
                new LambdaQueryWrapper<AlbumPhoto>()
                        .in(AlbumPhoto::getAlbumId, albumIds)
                        .eq(AlbumPhoto::getIsDeleted, 0)
        );

        // 2. 统计每个影集的有效照片数量 (AlbumId -> Count)
        Map<Long, Long> countsMap = allRelations.stream()
                .collect(Collectors.groupingBy(AlbumPhoto::getAlbumId, Collectors.counting()));

        // 3. 提取每个影集的第一张有效图 (AlbumId -> PhotoId)
        Map<Long, Long> firstPhotoMap = allRelations.stream()
                .collect(Collectors.groupingBy(
                        AlbumPhoto::getAlbumId,
                        Collectors.collectingAndThen(Collectors.toList(), list -> list.get(0).getPhotoId())
                ));

        // 批量回填计数
        voList.forEach(vo -> vo.setPhotoCount(countsMap.getOrDefault(vo.getId(), 0L).intValue()));

        // 批量请求图片信息并回填封面/预览 (逻辑同之前，但 requestPhotoIds 来自 firstPhotoMap)
        List<Long> requestPhotoIds = firstPhotoMap.values().stream().distinct().collect(Collectors.toList());
        if (!requestPhotoIds.isEmpty()) {
            try {
                Result<List<PictureDTO>> feignResult = photoFeign.getPicturesByIds(requestPhotoIds);
                if (feignResult.getCode() == 200 && feignResult.getData() != null) {
                    Map<Long, PictureDTO> photoMap = feignResult.getData().stream()
                            .collect(Collectors.toMap(PictureDTO::getPictureid, p -> p));

                    for (AlbumVO vo : voList) {
                        Long firstId = firstPhotoMap.get(vo.getId());
                        if (firstId != null && photoMap.containsKey(firstId)) {
                            PictureDTO firstPic = photoMap.get(firstId);
                            vo.setPhotos(List.of(firstPic));
                            if (vo.getCoverUrl() == null || vo.getCoverUrl().isBlank()) {
                                vo.setCoverUrl(firstPic.getPreviewUrl());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return voList;
    }

    // ─────────────────────────────────────────────────────────
    // 获取影集详情（含照片ID列表）
    // ─────────────────────────────────────────────────────────
    @Override
    public AlbumVO getAlbumDetail(Integer userId, Long albumId) {
        Album album = getAlbumAndCheckOwner(userId, albumId);

        List<Long> photoIds = albumPhotoMapper.selectPhotoIdsByAlbumId(albumId);
        AlbumVO vo = toVO(album, photoIds);
        vo.setPhotoCount(photoIds.size());

        if (photoIds != null && !photoIds.isEmpty()) {
            try {
                Result<List<PictureDTO>> picResult = photoFeign.getPicturesByIds(photoIds);
                if (picResult.getCode() == 200 && picResult.getData() != null) {
                    vo.setPhotos(picResult.getData());
                    if ((vo.getCoverUrl() == null || vo.getCoverUrl().isBlank())
                            && !picResult.getData().isEmpty()) {
                        vo.setCoverUrl(picResult.getData().getFirst().getPreviewUrl());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return vo;
    }

    // ─────────────────────────────────────────────────────────
    // 向影集添加照片（去重处理）
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPhotos(Integer userId, AlbumPhotoRequest request) {
        Long albumId = request.getAlbumId();
        if (albumId == null) {
            throw new RuntimeException("影集ID不能为空");
        }
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
                .toList();

        // 批量插入新照片关联
        for (AlbumPhoto ap : toInsert) {
            albumPhotoMapper.insert(ap);
        }

    }

    // ─────────────────────────────────────────────────────────
    // 从影集移除照片
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removePhotos(Integer userId, AlbumPhotoRequest request) {
        Long sourceAlbumId = request.getSourceAlbumId();
        if (sourceAlbumId == null) {
            throw new RuntimeException("影集ID不能为空");
        }
        getAlbumAndCheckOwner(userId, sourceAlbumId);

        if (request.getPhotoIds() == null || request.getPhotoIds().isEmpty()) {
            return;
        }

        albumPhotoMapper.delete(
                new LambdaQueryWrapper<AlbumPhoto>()
                        .eq(AlbumPhoto::getAlbumId, sourceAlbumId)
                        .in(AlbumPhoto::getPhotoId, request.getPhotoIds())
        );

        refreshCoverUrl(sourceAlbumId);
    }

    // ─────────────────────────────────────────────────────────
    // 移动照片：从源影集移除，添加到目标影集
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void movePhotos(Integer userId, MovePhotoRequest request) {
        Long sourceAlbumId = request.getSourceAlbumId();
        if (sourceAlbumId == null) {
            throw new RuntimeException("源影集ID不能为空");
        }
        getAlbumAndCheckOwner(userId, sourceAlbumId);

        List<Long> photoIds = request.getPhotoIds();
        Long targetAlbumId = request.getTargetAlbumId();
        if (photoIds == null || photoIds.isEmpty()) {
            return;
        }
        if (targetAlbumId == null) {
            throw new RuntimeException("目标影集ID不能为空");
        }
        if (targetAlbumId.equals(sourceAlbumId)) {
            throw new RuntimeException("目标影集不能与源影集相同");
        }
        getAlbumAndCheckOwner(userId, targetAlbumId);

        // 从源影集移除照片
        albumPhotoMapper.delete(
                new LambdaQueryWrapper<AlbumPhoto>()
                        .eq(AlbumPhoto::getAlbumId, sourceAlbumId)
                        .in(AlbumPhoto::getPhotoId, photoIds)
        );

        // 向目标影集添加照片（去重）
        List<Long> existingInTarget = albumPhotoMapper.selectPhotoIdsByAlbumId(targetAlbumId);
        List<AlbumPhoto> toInsert = photoIds.stream()
                .filter(photoId -> !existingInTarget.contains(photoId))
                .map(photoId -> {
                    AlbumPhoto ap = new AlbumPhoto();
                    ap.setAlbumId(targetAlbumId);
                    ap.setPhotoId(photoId);
                    return ap;
                })
                .toList();

        for (AlbumPhoto ap : toInsert) {
            albumPhotoMapper.insert(ap);
        }

        refreshCoverUrl(sourceAlbumId);
    }

    // ─────────────────────────────────────────────────────────
    // 复制照片：保留源影集照片，复制到多个目标影集
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void copyPhotos(Integer userId, CopyPhotoRequest request) {
        Long sourceAlbumId = request.getSourceAlbumId();
        if (sourceAlbumId == null) {
            throw new RuntimeException("源影集ID不能为空");
        }
        getAlbumAndCheckOwner(userId, sourceAlbumId);

        List<Long> photoIds = request.getPhotoIds();
        List<Long> targetAlbumIds = request.getTargetAlbumIds();
        if (photoIds == null || photoIds.isEmpty()) {
            return;
        }
        if (targetAlbumIds == null || targetAlbumIds.isEmpty()) {
            throw new RuntimeException("目标影集ID列表不能为空");
        }

        // 校验所有目标影集属于当前用户
        for (Long targetAlbumId : targetAlbumIds) {
            if (targetAlbumId.equals(sourceAlbumId)) {
                throw new RuntimeException("目标影集不能与源影集相同");
            }
            getAlbumAndCheckOwner(userId, targetAlbumId);
        }

        // 向每个目标影集添加照片（去重）
        for (Long targetAlbumId : targetAlbumIds) {
            List<Long> existing = albumPhotoMapper.selectPhotoIdsByAlbumId(targetAlbumId);
            List<AlbumPhoto> toInsert = photoIds.stream()
                    .filter(photoId -> !existing.contains(photoId))
                    .map(photoId -> {
                        AlbumPhoto ap = new AlbumPhoto();
                        ap.setAlbumId(targetAlbumId);
                        ap.setPhotoId(photoId);
                        return ap;
                    })
                    .toList();

            for (AlbumPhoto ap : toInsert) {
                albumPhotoMapper.insert(ap);
            }
        }
    }


    @Override
    public Map<String, List<PictureDTO>> getPicturesByCategory(Integer userId, boolean onlyFirst) {
        if (onlyFirst) {
            return photoFeign.getCategoryFirstGroup(userId).getData();
        } else {
            return photoFeign.getCategoryAllGroup(userId).getData();
        }
    }

    // ─────────────────────────────────────────────────────────
    // 内部接口用
    // ─────────────────────────────────────────────────────────
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearPhotosFromAlbums(List<Long> photoIds) {
        if (photoIds == null || photoIds.isEmpty()) return;

        // 获取到这些照片涉及的 albumId 以便更新影集计数
        List<AlbumPhoto> affectedRelations = albumPhotoMapper.selectList(
                new LambdaQueryWrapper<AlbumPhoto>().in(AlbumPhoto::getPhotoId, photoIds)
        );

        if (affectedRelations.isEmpty()) return;

        // 从关联表删除
        albumPhotoMapper.delete(new LambdaQueryWrapper<AlbumPhoto>().in(AlbumPhoto::getPhotoId, photoIds));

        affectedRelations.stream().map(AlbumPhoto::getAlbumId).distinct().forEach(this::refreshCoverUrl);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAlbumPhotoStatus(List<Long> photoIds, Integer status) {
        if (photoIds == null || photoIds.isEmpty()) return;

        // 1. 更新关联表状态
        albumPhotoMapper.update(null,
                new LambdaUpdateWrapper<AlbumPhoto>()
                        .in(AlbumPhoto::getPhotoId, photoIds)
                        .set(AlbumPhoto::getIsDeleted, status)
        );

        // 2. 查出这批照片散落在哪些影集里，刷新受影响影集的封面
        List<AlbumPhoto> affected = albumPhotoMapper.selectList(
                new LambdaQueryWrapper<AlbumPhoto>().in(AlbumPhoto::getPhotoId, photoIds)
        );
        List<Long> albumIds = affected.stream().map(AlbumPhoto::getAlbumId).distinct().toList();

        for (Long albumId : albumIds) {
            refreshCoverUrl(albumId);
        }
    }


    // ─────────────────────────────────────────────────────────
    // 私有工具方法
    // ─────────────────────────────────────────────────────────
    /**
     * 查询影集，并校验是否属于该用户（权限验证）
     */
    private Album getAlbumAndCheckOwner(Integer userId, Long albumId) {
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
     * 刷新影集封面：取关联表中第一条照片的预览URL作为封面
     */
    private void refreshCoverUrl(Long albumId) {
        List<Long> photoIds = albumPhotoMapper.selectPhotoIdsByAlbumId(albumId);
        String newCover = null;
        if (photoIds != null && !photoIds.isEmpty()) {
            try {
                Result<List<PictureDTO>> result = photoFeign.getPicturesByIds(List.of(photoIds.getFirst()));
                if (result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                    newCover = result.getData().getFirst().getPreviewUrl();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        albumMapper.update(null,
                new LambdaUpdateWrapper<Album>()
                        .eq(Album::getId, albumId)
                        .set(Album::getCoverUrl, newCover)
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
