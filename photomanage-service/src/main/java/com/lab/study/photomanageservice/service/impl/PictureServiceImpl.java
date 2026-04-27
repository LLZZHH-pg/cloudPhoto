package com.lab.study.photomanageservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.LAB.study.dto.PictureDTO;
import com.LAB.study.dto.TimelineDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.study.photomanageservice.entity.Picture;
import com.lab.study.photomanageservice.mapper.PictureMapper;
import com.lab.study.photomanageservice.service.PictureService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Override
    public List<TimelineDTO> getTimeline(Integer userId, long current, long size) {
        Page<Picture> page = new Page<>(current, size);
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getUserid, userId)
                .isNull(Picture::getDeleteTime)
                .orderByDesc(Picture::getShotTime);

        List<Picture> records = this.page(page, queryWrapper).getRecords();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<PictureDTO>> groupedMap = records.stream().map(this::convertToDTOWithThumbnail)
                .collect(Collectors.groupingBy(dto -> dto.getShotTime().format(formatter)));

        return groupedMap.entrySet().stream()
                .map(entry -> {
                    TimelineDTO timelineDTO = new TimelineDTO();
                    timelineDTO.setDate(entry.getKey());
                    timelineDTO.setPictures(entry.getValue());
                    return timelineDTO;
                })
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public PictureDTO getDetail(Long id) {
        Picture picture = this.getById(id);
        if (picture == null || picture.getDeleteTime() != null) {
            throw new IllegalArgumentException("图片不存在或已被移至回收站");
        }
        return convertToDTOWithHDPreview(picture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadPictures(MultipartFile[] files, Integer userId) {
        for (MultipartFile file : files) {
            String hash = calculateHash(file);
            LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Picture::getFileHash, hash).eq(Picture::getUserid, userId);
            if (this.count(queryWrapper) > 0) {
                continue;
            }

            String exifJson = extractExif(file);
            LocalDateTime shotTime = extractShotTime(file);

            String qiniuUrl = uploadToQiniu(file);

            Picture picture = new Picture();
            picture.setUserid(userId);
            picture.setFileHash(hash);
            picture.setFileUrl(qiniuUrl);
            picture.setFileName(file.getOriginalFilename());
            picture.setFileSize((int) file.getSize());
            picture.setShotTime(shotTime);
            picture.setFileExif(exifJson);

            this.save(picture);

            // 5. 删除本地临时文件（Spring 的 MultipartFile 通常在请求结束后自动清理，若有自建落地文件需手动 IO 删除）
        }
    }

    @Override
    public void deletePictures(List<Long> ids) {
        LocalDateTime deleteLimit = LocalDateTime.now();
        LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Picture::getPictureid, ids)
                .set(Picture::getDeleteTime, deleteLimit);
        this.update(updateWrapper);
    }

    @Override
    public List<PictureDTO> getTrashList(Integer userId) {
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getUserid, userId)
                .isNotNull(Picture::getDeleteTime)
                .orderByAsc(Picture::getDeleteTime);

        return this.list(queryWrapper).stream()
                .map(this::convertToDTOWithThumbnail)
                .collect(Collectors.toList());
    }

    @Override
    public void restorePictures(List<Long> ids) {
        LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Picture::getPictureid, ids)
                .set(Picture::getDeleteTime, null);
        this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanTrash(List<Long> ids) {
        List<Picture> pictures = this.listByIds(ids);
        if (pictures.isEmpty()) return;
        for (Picture p : pictures) {
            deleteFromQiniu(p.getFileUrl());
        }
        this.removeByIds(ids);
    }



    private PictureDTO convertToDTOWithThumbnail(Picture picture) {
        PictureDTO dto = new PictureDTO();
        BeanUtils.copyProperties(picture, dto);
        dto.setPreviewUrl(picture.getFileUrl() + "?imageView2/1/w/200/h/200");
        return dto;
    }

    private PictureDTO convertToDTOWithHDPreview(Picture picture) {
        PictureDTO dto = new PictureDTO();
        BeanUtils.copyProperties(picture, dto);
        dto.setPreviewUrl(picture.getFileUrl() + "?imageView2/2/w/1920/q/90");
        return dto;
    }

    private String calculateHash(MultipartFile file) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = file.getBytes();
            byte[] hashBytes = digest.digest(bytes);

            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("计算文件哈希失败", e);
        }
    }

    private String extractExif(MultipartFile file) {
        try {
            Map<String, Object> exifMap = new HashMap<>();

            try (InputStream inputStream = file.getInputStream()) {
                Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

                for (Directory directory : metadata.getDirectories()) {
                    for (Tag tag : directory.getTags()) {
                        exifMap.put(tag.getTagName(), tag.getDescription());
                    }
                }
            }

            return new ObjectMapper().writeValueAsString(exifMap);
        } catch (Exception e) {
            throw new RuntimeException("解析图片 EXIF 失败", e);
        }
    }

    private LocalDateTime extractShotTime(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);

            ExifSubIFDDirectory exifDirectory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDirectory != null) {
                Date date = exifDirectory.getDateOriginal();
                if (date != null) {
                    return date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                }

                date = exifDirectory.getDateDigitized();
                if (date != null) {
                    return date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }

            ExifIFD0Directory ifd0Directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (ifd0Directory != null) {
                Date date = ifd0Directory.getDate(0);
                if (date != null) {
                    return date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDateTime();
                }
            }

            return LocalDateTime.now();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private String uploadToQiniu(MultipartFile file) {
        // TODO: 调用 qiniu-java-sdk 将字节流上传至指定 Bucket
        return "http://cdn.qiniu.com/mock/" + file.getOriginalFilename();
    }

    private void deleteFromQiniu(String fileUrl) {
        // TODO: 解析 URL 中的 Key，调用 Qiniu API 删除空间文件
    }
}