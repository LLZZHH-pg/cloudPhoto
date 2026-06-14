package com.lab.study.photomanageservice.scheduler;

import com.LAB.study.dto.UserInfoDTO;
import com.LAB.study.result.Result;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lab.study.photomanageservice.entity.Picture;
import com.lab.study.photomanageservice.feign.UserFeign;
import com.lab.study.photomanageservice.mapper.PictureMapper;
import com.lab.study.photomanageservice.service.PictureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrashAutoCleanScheduler {

    private final PictureMapper pictureMapper;
    private final PictureService pictureService;
    private final UserFeign userFeign;

    /**
     * 每周六凌晨 3 点执行一次
     * 扫描所有过期（deleteTime + recycledays < now）的照片并彻底删除
     */
    @Scheduled(cron = "0 0 3 ? * SAT")
    public void autoCleanExpiredTrash() {
        log.info("开始自动清理过期回收站照片...");

        // 1. 查询所有 delete_time 不为空的照片，按用户分组
        LambdaQueryWrapper<Picture> wrapper = new LambdaQueryWrapper<>();
        wrapper.isNotNull(Picture::getDeleteTime);
        List<Picture> trashedPhotos = pictureMapper.selectList(wrapper);

        // 2. 按 userId 分组
        Map<Integer, List<Picture>> groupedByUser = trashedPhotos.stream()
                .collect(Collectors.groupingBy(Picture::getUserid));

        LocalDateTime now = LocalDateTime.now();

        for (Map.Entry<Integer, List<Picture>> entry : groupedByUser.entrySet()) {
            Integer userId = entry.getKey();
            List<Picture> userPhotos = entry.getValue();

            // 3. 获取该用户的 recycledays
            Result<UserInfoDTO> result = userFeign.getUserInfo(userId);
            int recycleDays = 30;
            if (result != null && result.getCode() == 200 && result.getData() != null
                    && result.getData().getRecycledays() != null) {
                recycleDays = result.getData().getRecycledays();
            }

            // 4. 筛选已过期的照片 ID
            final int limit = recycleDays;
            List<Long> expiredIds = userPhotos.stream()
                    .filter(p -> {
                        long daysPassed = ChronoUnit.DAYS.between(p.getDeleteTime(), now);
                        return daysPassed >= limit;
                    })
                    .map(Picture::getPictureid)
                    .collect(Collectors.toList());

            // 5. 调用已有的 cleanTrash 方法（包含删库 + 删云端 + 归还配额）
            if (!expiredIds.isEmpty()) {
                try {
                    pictureService.cleanTrash(expiredIds, userId);
                    log.info("用户 {} 自动清理了 {} 张过期照片", userId, expiredIds.size());
                } catch (Exception e) {
                    log.error("用户 {} 自动清理失败: {}", userId, e.getMessage());
                }
            }
        }

        log.info("自动清理回收站完成");
    }
}