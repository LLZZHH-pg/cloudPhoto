-- =====================================================
-- 影集管理服务 数据库建表 SQL
-- 数据库：photo_album
-- =====================================================

CREATE DATABASE IF NOT EXISTS photo_album DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE photo_album;

-- -----------------------------------------------------
-- 影集表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `album` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '影集ID',
    `name`        VARCHAR(100) NOT NULL                COMMENT '影集名称',
    `description` VARCHAR(255)                         COMMENT '影集描述',
    `cover_url`   VARCHAR(500)                         COMMENT '封面图片URL',
    `user_id`     BIGINT       NOT NULL                COMMENT '所属用户ID',
    `is_public`   TINYINT      NOT NULL DEFAULT 0      COMMENT '是否公开：0-私有，1-公开',
    `photo_count` INT          NOT NULL DEFAULT 0      COMMENT '照片数量',
    `deleted`     TINYINT      NOT NULL DEFAULT 0      COMMENT '逻辑删除：0-正常，1-已删除',
    `created_at`  DATETIME                             COMMENT '创建时间',
    `updated_at`  DATETIME                             COMMENT '更新时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_is_public` (`is_public`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影集表';

-- -----------------------------------------------------
-- 影集-照片关联表
-- -----------------------------------------------------
CREATE TABLE IF NOT EXISTS `album_photo` (
    `id`         BIGINT   NOT NULL AUTO_INCREMENT COMMENT '关联ID',
    `album_id`   BIGINT   NOT NULL                COMMENT '影集ID',
    `photo_id`   BIGINT   NOT NULL                COMMENT '照片ID',
    `created_at` DATETIME                         COMMENT '添加时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_album_photo` (`album_id`, `photo_id`),  -- 防止重复添加
    INDEX `idx_album_id` (`album_id`),
    INDEX `idx_photo_id` (`photo_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='影集-照片关联表';
