package com.lab.study.photomanageservice.service.impl;

import com.LAB.study.dto.PictureDTO;
import com.LAB.study.dto.UserInfoDTO;
import com.LAB.study.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.LAB.study.vo.PictureVO;
import com.LAB.study.vo.TimelineVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lab.study.photomanageservice.entity.Picture;
import com.lab.study.photomanageservice.feign.AlbumFeign;
import com.lab.study.photomanageservice.feign.UserFeign;
import com.lab.study.photomanageservice.mapper.PictureMapper;
import com.lab.study.photomanageservice.service.PictureService;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.storage.BucketManager;
import com.qiniu.util.Auth;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.tiia.v20190529.TiiaClient;
import com.tencentcloudapi.tiia.v20190529.models.DetectLabelRequest;
import com.tencentcloudapi.tiia.v20190529.models.DetectLabelResponse;
import com.tencentcloudapi.tiia.v20190529.models.DetectLabelItem;
import lombok.RequiredArgsConstructor;
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {

    @Value("${qiniu.access-key}")
    private String accessKey;
    @Value("${qiniu.secret-key}")
    private String secretKey;
    @Value("${qiniu.bucket}")
    private String bucket;
    @Value("${qiniu.domain}")
    private String domain;

    @Value("${tencent.cloud.secret-id}")
    private String tencentSecretId;
    @Value("${tencent.cloud.secret-key}")
    private String tencentSecretKey;
    @Value("${tencent.cloud.region}")
    private String tencentRegion;

    private final UserFeign userFeign;
    private final AlbumFeign albumFeign;

    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
            "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp",
            "image/bmp", "image/svg+xml", "image/tiff", "image/avif"
    );

    @Override
    public List<TimelineVO> getTimeline(Integer userId, long current, long size) {
        Page<Picture> page = new Page<>(current, size);
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getUserid, userId)
                .isNull(Picture::getDeleteTime)
                .orderByDesc(Picture::getShotTime);

        List<Picture> records = this.page(page, queryWrapper).getRecords();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        Map<String, List<PictureVO>> groupedMap = records.stream().map(this::convertToVOWithThumbnail)
                .collect(Collectors.groupingBy(dto -> dto.getShotTime().format(formatter)));

        return groupedMap.entrySet().stream()
                .map(entry -> {
                    TimelineVO vo = new TimelineVO();
                    vo.setDate(entry.getKey());
                    vo.setPictures(entry.getValue());
                    return vo;
                })
                .sorted((t1, t2) -> t2.getDate().compareTo(t1.getDate()))
                .collect(Collectors.toList());
    }

    @Override
    public PictureVO getDetail(Long id, Integer userId) {
        LambdaQueryWrapper<Picture> query = new LambdaQueryWrapper<>();
        query.eq(Picture::getPictureid, id)
                .eq(Picture::getUserid, userId);
        Picture picture = this.getOne(query);
        if (picture == null || picture.getDeleteTime() != null) {
            throw new IllegalArgumentException("图片不存在或已被移至回收站");
        }
        return convertToVOWithHDPreview(picture);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadPictures(MultipartFile[] files, Integer userId) {
        if (files == null || files.length == 0) {
            return;
        }

        // 1. 预先计算本次上传需要的所有文件大小总和 (字节)
        long totalSizeNeeded = 0;
        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
                throw new IllegalArgumentException("不支持的图片格式");
            }
            totalSizeNeeded += file.getSize();
        }

        // 2. 调用 UserFeign 获取用户信息，检查剩余容量
        Result<UserInfoDTO> userInfoResult = userFeign.getUserInfo(userId);

        UserInfoDTO userInfo = userInfoResult.getData();

        long totalStorage = userInfo.getTotalstorage() != null ? userInfo.getTotalstorage() : 0L;
        long usedStorage = userInfo.getUsedstorage() != null ? userInfo.getUsedstorage() : 0L;
        long remainStorage = totalStorage - usedStorage;

        if (totalSizeNeeded > remainStorage) {
            throw new RuntimeException("存储空间不足，剩余空间：" + (remainStorage / 1024 / 1024) + "MB，本次需要：" + (totalSizeNeeded / 1024 / 1024) + "MB");
        }

        // 3. 空间充足，执行上传和数据库保存
        long actualUploadedSize = 0;
        for (MultipartFile file : files) {
            String hash = calculateHash(file);

            // 检查全局是否已存在该哈希，用于秒传数据复用
            LambdaQueryWrapper<Picture> globalQuery = new LambdaQueryWrapper<>();
            globalQuery.eq(Picture::getFileHash, hash).last("LIMIT 1");
            Picture existingPicture = this.getOne(globalQuery);

            Picture picture = new Picture();
            picture.setUserid(userId);
            picture.setFileHash(hash);

            String qiniuUrl;

            if (existingPicture != null) {
                // =============== 触发秒传 (数据复用) ===============
                // 按照要求复制除了 uid、照片id、分类以外的所有数据
                qiniuUrl = existingPicture.getFileUrl();
                picture.setFileUrl(qiniuUrl);
                picture.setFileName(existingPicture.getFileName());
                picture.setFileSize(existingPicture.getFileSize());
                picture.setShotTime(existingPicture.getShotTime());
                picture.setFileExif(existingPicture.getFileExif());
            } else {
                // ============ 真实执行上传 ============
                picture.setFileName(file.getOriginalFilename());
                picture.setFileSize((int) file.getSize());
                picture.setShotTime(extractShotTime(file));
                picture.setFileExif(extractExif(file));

                qiniuUrl = uploadToQiniu(file);
                picture.setFileUrl(qiniuUrl);
            }

            // 不论是否触发秒传，都执行腾讯云图像标签识别
            String categoryResult = fetchImageCategory(qiniuUrl);
            picture.setCategory(categoryResult);

            this.save(picture);

            // 累计计入用户的存储配额
            actualUploadedSize += (existingPicture != null ? existingPicture.getFileSize() : file.getSize());
        }

        // 4. 同步更新用户已使用容量
        if (actualUploadedSize > 0) {
            Result<Void> updateResult = userFeign.updateStorage(userId, actualUploadedSize);
            if (updateResult.getCode() != 200) {
                throw new RuntimeException("更新存储空间失败");
            }
        }
    }

    @Override
    public List<String> getDownloadUrls(List<Long> ids, Integer userId) {
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }

        LambdaQueryWrapper<Picture> query = new LambdaQueryWrapper<>();
        query.in(Picture::getPictureid, ids)
                .eq(Picture::getUserid, userId)
                .isNull(Picture::getDeleteTime);
        List<Picture> pictures = this.list(query);

        return pictures.stream().map(picture -> {
            String rawUrl = picture.getFileUrl();
            try {
                String encodedFileName = URLEncoder.encode(picture.getFileName(), StandardCharsets.UTF_8);
                // 拼接样式名 -normal 并配合 attname
                String downloadUrl = rawUrl + "-normal?attname=" + encodedFileName;
                return signUrl(downloadUrl);
            } catch (Exception e) {
                return signUrl(rawUrl + "-normal");
            }
        }).collect(Collectors.toList());
    }

    @Override
    public void deletePictures(List<Long> ids, Integer userId) {
        if (ids == null || ids.isEmpty()) return;

        LocalDateTime deleteLimit = LocalDateTime.now();
        LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Picture::getPictureid, ids)
                .eq(Picture::getUserid, userId)
                .set(Picture::getDeleteTime, deleteLimit);
        this.update(updateWrapper);
    }

    @Override
    public List<PictureVO> getTrashList(Integer userId) {
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Picture::getUserid, userId)
                .isNotNull(Picture::getDeleteTime)
                .orderByAsc(Picture::getDeleteTime);

        return this.list(queryWrapper).stream()
                .map(this::convertToVOWithThumbnail)
                .collect(Collectors.toList());
    }

    @Override
    public void restorePictures(List<Long> ids, Integer userId) {
        if (ids == null || ids.isEmpty()) return;

        LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.in(Picture::getPictureid, ids)
                .eq(Picture::getUserid, userId)
                .set(Picture::getDeleteTime, null);
        this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cleanTrash(List<Long> ids, Integer userId) {
        if (ids == null || ids.isEmpty()) return;

        List<Picture> pictures = this.listByIds(ids);
        if (pictures.isEmpty()) return;

        // 1. 先将记录从当前用户的数据库彻底移除
        this.removeByIds(ids);
        albumFeign.clearPhotosFromAlbums(ids);

        long totalFreedSize = 0;

        // 2. 循环判断是否需要真正物理删除
        for (Picture p : pictures) {
            if (p.getFileSize() != null) {
                totalFreedSize += p.getFileSize();
            }
            String hash = p.getFileHash();
            if (hash != null && !hash.isEmpty()) {
                // 统计表中是否还有其他行在使用此哈希
                LambdaQueryWrapper<Picture> countQuery = new LambdaQueryWrapper<>();
                countQuery.eq(Picture::getFileHash, hash);
                long leftCount = this.count(countQuery);

                if (leftCount == 0) {
                    // 没有其他记录引用这个文件了，做七牛云物理清除
                    deleteFromQiniu(p.getFileUrl());
                }
            }
        }

        // 3. 将所选图片占用的配额空间退还给用户
        if (totalFreedSize > 0) {
            // 传负数表示释放配额
            Result<Void> updateResult = userFeign.updateStorage(userId, -totalFreedSize);
            if (updateResult.getCode() != 200) {
                throw new RuntimeException("清理回收站空间回收异常：" + updateResult.getMessage());
            }
        }
    }

    @Override
    public List<PictureDTO> getPicturesByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Picture> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Picture::getPictureid, ids)
                .isNull(Picture::getDeleteTime)
                .orderByDesc(Picture::getShotTime);

        return this.list(queryWrapper).stream()
                .map(this::convertToDTOWithThumbnail)
                .collect(Collectors.toList());
    }

    private PictureVO convertToVOWithThumbnail(Picture picture) {
        PictureVO vo = new PictureVO();
        vo.setPictureid(picture.getPictureid());
        vo.setShotTime(picture.getShotTime());
        vo.setDeleteTime(picture.getDeleteTime());
        String rawUrl = picture.getFileUrl() + "-thumb";
        vo.setPreviewUrl(signUrl(rawUrl));
        return vo;
    }

    private PictureDTO convertToDTOWithThumbnail(Picture picture) {
        PictureDTO dto = new PictureDTO();
        dto.setPictureid(picture.getPictureid());
        dto.setShotTime(picture.getShotTime());
        String rawUrl = picture.getFileUrl() + "-thumb";
        dto.setPreviewUrl(signUrl(rawUrl));
        return dto;
    }

    private PictureVO convertToVOWithHDPreview(Picture picture) {
        PictureVO vo = new PictureVO();
        BeanUtils.copyProperties(picture, vo);
        String rawUrl = picture.getFileUrl() + "-normal";
        vo.setPreviewUrl(signUrl(rawUrl));
        return vo;
    }

    private String signUrl(String url) {
        Auth auth = Auth.create(accessKey, secretKey);
        return auth.privateDownloadUrl(url, 3600);
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
            String key = fileUrl.substring(prefix.length());

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
            System.err.println("删除七牛云文件失败: " + e.getMessage());
        }
    }

    private String fetchImageCategory(String rawQiniuUrl) {
        try {
            // 构造认证信息
            Credential cred = new Credential(tencentSecretId, tencentSecretKey);
            // 实例化请求对象
            TiiaClient client = new TiiaClient(cred, tencentRegion);

            // 让腾讯云通过签名 URL 下载图片进行识别
            String thumbUrl = rawQiniuUrl + "-category";
            String signedUrl = signUrl(thumbUrl);

            DetectLabelRequest req = new DetectLabelRequest();
            req.setImageUrl(signedUrl);

            // 调用 DetectLabel 接口
            DetectLabelResponse resp = client.DetectLabel(req);

            // 获取识别出的标签数组，取置信度最高的名称作为分类结果
            if (resp.getLabels() != null && resp.getLabels().length > 0) {
                // 取第一条标签的 SecondCategory 作为分类
                DetectLabelItem item = resp.getLabels()[0];
                String secondCategory = item.getSecondCategory();
                if (secondCategory != null && !secondCategory.isBlank()) {
                    return secondCategory;
                }
            }
        } catch (Exception e) {
            System.err.println("调用腾讯云通用图像分类API失败: " + e.getMessage());
        }
        return "未分类"; // 请求异常或未识别出标签时的默认值
    }
}