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
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;



@Service
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Value("${qiniu.access-key}")
    private String accessKey;
    @Value("${qiniu.secret-key}")
    private String secretKey;
    @Value("${qiniu.bucket}")
    private String bucket;
    @Value("${qiniu.domain}")
    private String domain;

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
        try {
            // Region.autoRegion() 会自动匹配七牛云存储区域，也可以手动指定如 Region.region0() (华东)
            Configuration cfg = new Configuration(Region.autoRegion());
            UploadManager uploadManager = new UploadManager(cfg);

            // 组装随机文件名，避免重名冲突
            String originalFilename = file.getOriginalFilename();
            String ext = "";
            if (originalFilename != null && originalFilename.lastIndexOf(".") != -1) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String key = UUID.randomUUID().toString().replace("-", "") + ext;

            // 获取凭证
            Auth auth = Auth.create(accessKey, secretKey);
            String upToken = auth.uploadToken(bucket);

            // 执行上传
            Response response = uploadManager.put(file.getInputStream(), key, upToken, null, null);
            if (response.isOK()) {
                // 返回完整的访问 URL
                return (domain.endsWith("/") ? domain : domain + "/") + key;
            } else {
                throw new RuntimeException("上传七牛云失败: " + response.bodyString());
            }
        } catch (Exception e) {
            throw new RuntimeException("上传图片异常", e);
        }
    }

    private void deleteFromQiniu(String fileUrl) {
        if (fileUrl == null || !fileUrl.startsWith(domain)) {
            return;
        }
        try {
            // 从 URL 中提取 File Key
            String prefix = domain.endsWith("/") ? domain : domain + "/";
            String key = fileUrl.replaceFirst(prefix, "");

            // 如果 URL 带了类似 ?imageView2 等参数，我们需要截断获取实际的 key
            int queryIndex = key.indexOf("?");
            if (queryIndex != -1) {
                key = key.substring(0, queryIndex);
            }

            // 初始化删除管理器
            Configuration cfg = new Configuration(Region.autoRegion());
            Auth auth = Auth.create(accessKey, secretKey);
            BucketManager bucketManager = new BucketManager(auth, cfg);

            // 发起删除请求
            bucketManager.delete(bucket, key);
        } catch (QiniuException e) {
            // 在批量删除时，建议打印日志而非让主流程中断直接跑出异常
            System.err.println("删除七牛云文件失败: " + e.getMessage());
        }
    }
}