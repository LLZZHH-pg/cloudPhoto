CREATE DATABASE  IF NOT EXISTS `photo_user` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `photo_user`;
-- MySQL dump 10.13  Distrib 8.0.42, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: photo_user
-- ------------------------------------------------------
-- Server version	9.2.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `plan_info`
--

DROP TABLE IF EXISTS `plan_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `plan_info` (
  `planid` int NOT NULL AUTO_INCREMENT,
  `name` varchar(45) COLLATE utf8mb4_unicode_ci NOT NULL,
  `storage` bigint NOT NULL DEFAULT '1073741824',
  `recycle` int NOT NULL DEFAULT '30',
  `price` decimal(10,0) NOT NULL DEFAULT '99999',
  `statues` enum('enable','disable') COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'disable',
  PRIMARY KEY (`planid`),
  UNIQUE KEY `name_UNIQUE` (`name`),
  UNIQUE KEY `planid_UNIQUE` (`planid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `plan_info`
--

LOCK TABLES `plan_info` WRITE;
/*!40000 ALTER TABLE `plan_info` DISABLE KEYS */;
INSERT INTO `plan_info` VALUES (1,'base',1073741824,30,0,'enable'),(2,'bbb',2147483648,60,50,'enable'),(3,'ccc',2147483648,30,30,'disable'),(4,'bbbbbb',10737418240,90,100,'enable');
/*!40000 ALTER TABLE `plan_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_info`
--

DROP TABLE IF EXISTS `user_info`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_info` (
  `userid` int NOT NULL AUTO_INCREMENT,
  `nam` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `pas` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `tel` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `eml` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `usedstorage` bigint unsigned NOT NULL DEFAULT '0',
  `statues` enum('enable','disable','auth') NOT NULL DEFAULT 'enable',
  PRIMARY KEY (`userid`) USING BTREE,
  UNIQUE KEY `tel_UNIQUE` (`tel`) USING BTREE,
  UNIQUE KEY `userid_UNIQUE` (`userid`),
  UNIQUE KEY `username_UNIQUE` (`nam`) USING BTREE
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci ROW_FORMAT=DYNAMIC;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_info`
--

LOCK TABLES `user_info` WRITE;
/*!40000 ALTER TABLE `user_info` DISABLE KEYS */;
INSERT INTO `user_info` VALUES (4,'ddddd','$2a$10$yudvxfovKg0G0UM2qg/Vmugl6RYYBFOim490xYDInx90WXleOqIua','13800138001','alice@example.com',50463468,'auth'),(5,'','$2a$10$o3gXxw.qbt55QDfSTCD3qOvtml5StUZBm88ueCipRQ0gLK/ibW0WK','12345678901','',0,'enable'),(6,'ddd','$2a$10$hE7h5fhM3Y1F2TCCQvbYrOhJYCZYl0j7.WGJcv3r8H.x9k2zd./Pq','13800138002','',1995910,'enable'),(7,'abc','$2a$10$Aukj.bA5MI0vFouW3TnQz.7N5uOsh1X1yB.PLnw6r39xaIBdnBWeK','13800138003','al@example.com',0,'enable');
/*!40000 ALTER TABLE `user_info` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `user_plan`
--

DROP TABLE IF EXISTS `user_plan`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_plan` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `userid` int NOT NULL,
  `planid` int NOT NULL,
  `created_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `id_UNIQUE` (`id`),
  KEY `userid_idx` (`userid`),
  KEY `planid_idx` (`planid`),
  CONSTRAINT `planid` FOREIGN KEY (`planid`) REFERENCES `plan_info` (`planid`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `userid` FOREIGN KEY (`userid`) REFERENCES `user_info` (`userid`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `user_plan`
--

LOCK TABLES `user_plan` WRITE;
/*!40000 ALTER TABLE `user_plan` DISABLE KEYS */;
INSERT INTO `user_plan` VALUES (1,4,2,'2026-06-07 02:14:16'),(2,5,1,NULL),(3,6,2,NULL),(4,7,1,'2026-06-07 00:44:00');
/*!40000 ALTER TABLE `user_plan` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-07  2:29:44
