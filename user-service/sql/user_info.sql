/*
 Navicat Premium Dump SQL

 Source Server         : cloudphoto
 Source Server Type    : MySQL
 Source Server Version : 80040 (8.0.40)
 Source Host           : localhost:3306
 Source Schema         : photo_user

 Target Server Type    : MySQL
 Target Server Version : 80040 (8.0.40)
 File Encoding         : 65001

 Date: 26/04/2026 21:20:31
*/
CREATE DATABASE IF NOT EXISTS photo_user DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE photo_user;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `userid` int NOT NULL AUTO_INCREMENT,
  `nam` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `pas` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tel` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `eml` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL,
  `usedstorage` int UNSIGNED NULL DEFAULT 0,
  `totalstorage` int UNSIGNED NULL DEFAULT 0,
  PRIMARY KEY (`userid`) USING BTREE,
  UNIQUE INDEX `tel_UNIQUE`(`tel` ASC) USING BTREE,
  UNIQUE INDEX `username_UNIQUE`(`nam` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
