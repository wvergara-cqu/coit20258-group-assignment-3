-- =====================================================================
-- DRS-Enhanced - MySQL create and populate script (COIT20258 Assessment 3)
--
-- Creates database `drs_enhanced`, its 11 tables and loads:
--   * 10 user accounts (passwords PBKDF2-hashed - see README for logins)
--   * 8 departments and 16 responders carried over from DRS-Initial (A2)
--   * 12 disaster reports collected with DRS-Initial (A2)
--
-- NOTE: The server ALSO creates the database and tables programmatically on
-- start-up (DatabaseManager) and loads the same data if the tables are empty,
-- so running this script is optional. Reporter name/phone columns are
-- AES-256-GCM ciphertext encrypted with security.aesKey in server.properties;
-- keep that key unchanged or these rows cannot be decrypted.
--
-- Run:  mysql -u root -p < database/drs_enhanced.sql
-- WARNING: drops and recreates the drs_enhanced tables.
-- =====================================================================


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

CREATE DATABASE /*!32312 IF NOT EXISTS*/ `drs_enhanced` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;

USE `drs_enhanced`;
DROP TABLE IF EXISTS `audit_log`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `audit_log` (
  `id` int NOT NULL AUTO_INCREMENT,
  `user_id` int DEFAULT NULL,
  `username` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `action` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `entity_id` int NOT NULL,
  `payload_hash` char(64) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `signature` text COLLATE utf8mb4_unicode_ci,
  `signed` tinyint(1) NOT NULL DEFAULT '0',
  `verified` tinyint(1) NOT NULL DEFAULT '0',
  `logged_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_audit_time` (`logged_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `audit_log` WRITE;
/*!40000 ALTER TABLE `audit_log` DISABLE KEYS */;
/*!40000 ALTER TABLE `audit_log` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `departments` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `contact_person` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `available` tinyint(1) NOT NULL DEFAULT '1',
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `departments` WRITE;
/*!40000 ALTER TABLE `departments` DISABLE KEYS */;
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (1,'Fire & Emergency','Emergency Services','Chief Alan Burns','0400-111-001',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (2,'Hospital Services','Medical','Dr. Helen Carter','0400-111-002',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (3,'Electricity Authority','Utilities','Engineer Sam Reid','0400-111-003',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (4,'Transportation Dept','Infrastructure','Director Fiona Clarke','0400-111-004',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (5,'Waste Management','Sanitation','Supervisor Gary Walsh','0400-111-005',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (6,'Water Supply Authority','Utilities','Engineer Nina Patel','0400-111-006',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (7,'School Administration','Education','Principal Roy Dawson','0400-111-007',1,'2026-10-12 07:06:29');
INSERT INTO `departments` (`id`, `name`, `type`, `contact_person`, `phone`, `available`, `updated_at`) VALUES (8,'Law Enforcement','Security','Sergeant Kim Torres','0400-111-008',1,'2026-10-12 07:06:29');
/*!40000 ALTER TABLE `departments` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `disaster_reports`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disaster_reports` (
  `id` int NOT NULL AUTO_INCREMENT,
  `type` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `severity` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `location` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `description` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `reporter_name_enc` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `reporter_phone_enc` varchar(512) COLLATE utf8mb4_unicode_ci NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `priority` int NOT NULL,
  `reported_by` int DEFAULT NULL,
  `signature` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_report_status_priority` (`status`,`priority`),
  KEY `fk_report_user` (`reported_by`),
  CONSTRAINT `fk_report_user` FOREIGN KEY (`reported_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `disaster_reports` WRITE;
/*!40000 ALTER TABLE `disaster_reports` DISABLE KEYS */;
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (1,'FIRE','HIGH','12 Main Street, Brisbane QLD','Residential building fire with multiple floors affected.','nSDOGS5KLbXzH3BC6snZwxDfNktjatZebIPK9mdVaZE/ycjR','qBzyXb6cHzmnxjwPb+lQT+NAUcxCjLtP8Luva97pxjbgIohfXa1ulQ==','ASSESSING',38,NULL,NULL,'2026-10-10 03:06:29','2026-10-10 03:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (2,'HURRICANE','CRITICAL','Esplanade, Cairns QLD','Category 4 cyclone crossing the coast; roofs lifted and power lines down.','nooThG96tyCJG1fjvD/rZQ5DOIcanrkQaTa/2UdGxIL+z2YkewwO','JPvA91Rv4K0LLkeMaf0qz4fw1iBZveRSK4D+UJoDtWKY88K53U9PRw==','RESPONDING',50,NULL,NULL,'2026-10-10 06:06:29','2026-10-10 06:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (3,'FLOOD','HIGH','Fitzroy River, Rockhampton QLD','River has broken its banks; low-lying streets in Depot Hill under water.','A1nH0ncNwzDopg2/39VZF+E0EW/BrupA5WkHQ0t26zN5YJ2o9V5LjQ==','ltjhPdp0+dI5zlbucRId90momcEcwIgqvzHTHKWUVRctyXukQBmgDA==','ASSESSING',37,NULL,NULL,'2026-10-11 01:06:29','2026-10-11 01:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (4,'EARTHQUAKE','MEDIUM','Gladstone Marina, Gladstone QLD','Magnitude 4.8 tremor; cracks reported in marina breakwall.','D7Gvd+AxkWikNMJauztHVVtq83c1JhJ2vDut/EI/Ad9nu2JLNfs=','ahT1LGoz5LHxTVx/B9I6xTfaW7nmOSTwJIsw0MoGOUaZYyC5uCYrzQ==','REPORTED',29,NULL,NULL,'2026-10-11 05:06:29','2026-10-11 05:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (5,'FIRE','CRITICAL','Mount Archer National Park, Rockhampton QLD','Bushfire spreading towards Frenchville; heavy smoke over the ridge.','Kz09sLjlVFw2T6X9llgZt8knQayVxC0uyKjjclaWwp3XeodjuTGl','/ZPlr1u+VhY4ATrMUxS8bs4VJNWc9hVZlYlaTrurjUMOBYDLqIO2cg==','REPORTED',48,NULL,NULL,'2026-10-11 11:06:29','2026-10-11 11:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (6,'OTHER','LOW','Bruce Highway, Gracemere QLD','Chemical drum spill on road shoulder; no injuries reported.','LuK9l/L5ykMBTH7ASA0mGmi5FNWx7qv3RQGGYxZUrbSbQW8qpVk=','Clq73V0aW0YkwStfVtvFs6V4sEQE/rBVplsr8v9tMWo3osJruU7y3A==','RESOLVED',15,NULL,NULL,'2026-10-09 07:06:29','2026-10-09 07:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (7,'FLOOD','MEDIUM','Pioneer River, Mackay QLD','Flash flooding over the Forgan Bridge approach.','p5R66mDzx9CKR/WuLP1GOIyFhvki/RSizAPs1Yg6+jk60oW1bA==','3CS8Qr+EKqtENBlieD+ZPQtssxsfLeJMHh/lqsrddtGSXG/c2OZSug==','REPORTED',27,NULL,NULL,'2026-10-11 19:06:29','2026-10-11 19:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (8,'FIRE','LOW','Yeppoon Foreshore, Yeppoon QLD','Small grass fire near the lagoon car park.','SMaDQmjJRoy0o4elbRx/ZFpw7zlM7oPucc8A4gmkErNRZqggtAHC','jDhtZM6NbDAvB5srJX6Q2ptK37erF7YIL9EKns0KjwuxTQ9rRFRkgg==','RESOLVED',18,NULL,NULL,'2026-10-08 07:06:29','2026-10-08 07:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (9,'HURRICANE','HIGH','Airlie Beach, Whitsundays QLD','Severe tropical storm; boats torn from moorings.','DuMAlE5St8yT4pWBQLGn6Ik30OzlPnBpxwji8SyWduz3N4a8rg==','gZMUAfwocXsmGfSnLYT2ZUoubZDK2rK+f3q7ZLcD/ee74u0dDBPHTA==','REPORTED',40,NULL,NULL,'2026-10-11 23:06:29','2026-10-11 23:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (10,'EARTHQUAKE','HIGH','Bundaberg CBD, Bundaberg QLD','Magnitude 5.6 quake; facade damage to heritage buildings.','1zcgA4iY4OX9FmwTyF9nyKkQMBwMd6Z65L267MGPJEZeXBPVguTY','lYKZ/MOY/EEL5zXFxN9VDW7LWcHFGhvMpNa1l7Rsy22NQrwbRUCluA==','REPORTED',55,NULL,NULL,'2026-10-12 02:06:29','2026-10-12 02:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (11,'OTHER','MEDIUM','Emerald Rail Yard, Emerald QLD','Freight derailment; diesel leak contained.','g1r6hzmyX7GsruCdz/tPl4KEm8zJml0p4sQsWYTfdDjz/QKuUbFyeg==','/tlOcIUI61c1cjfre2UDEMWHM8FOdX09Q6BLp/Myq37mObC/WM2AfQ==','ASSESSING',25,NULL,NULL,'2026-10-12 04:06:29','2026-10-12 04:06:29');
INSERT INTO `disaster_reports` (`id`, `type`, `severity`, `location`, `description`, `reporter_name_enc`, `reporter_phone_enc`, `status`, `priority`, `reported_by`, `signature`, `created_at`, `updated_at`) VALUES (12,'FLOOD','CRITICAL','Kawana, Rockhampton QLD','Evacuation centre requested; water entering homes on Moores Creek Rd.','tt3rHlkY8ESc+A8CVhHCBoekNVgmmXrlfdMLfLSWHMLuy7yLldDe','CX7s0neSIo6zhs1v/RXKX0244YKm8W9bXU+YkOpKwIDY9nTUCuecZQ==','REPORTED',47,NULL,NULL,'2026-10-12 06:06:29','2026-10-12 06:06:29');
/*!40000 ALTER TABLE `disaster_reports` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `disaster_responses`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `disaster_responses` (
  `id` int NOT NULL AUTO_INCREMENT,
  `report_id` int NOT NULL,
  `notes` text COLLATE utf8mb4_unicode_ci,
  `created_by` int DEFAULT NULL,
  `signature` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_response_report` (`report_id`),
  KEY `fk_response_user` (`created_by`),
  CONSTRAINT `fk_response_report` FOREIGN KEY (`report_id`) REFERENCES `disaster_reports` (`id`),
  CONSTRAINT `fk_response_user` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `disaster_responses` WRITE;
/*!40000 ALTER TABLE `disaster_responses` DISABLE KEYS */;
/*!40000 ALTER TABLE `disaster_responses` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `field_tasks`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `field_tasks` (
  `id` int NOT NULL AUTO_INCREMENT,
  `response_id` int NOT NULL,
  `report_id` int NOT NULL,
  `responder_id` int NOT NULL,
  `instructions` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `created_at` datetime NOT NULL,
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_task_responder_status` (`responder_id`,`status`),
  KEY `fk_task_response` (`response_id`),
  KEY `fk_task_report` (`report_id`),
  CONSTRAINT `fk_task_report` FOREIGN KEY (`report_id`) REFERENCES `disaster_reports` (`id`),
  CONSTRAINT `fk_task_responder` FOREIGN KEY (`responder_id`) REFERENCES `responders` (`id`),
  CONSTRAINT `fk_task_response` FOREIGN KEY (`response_id`) REFERENCES `disaster_responses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `field_tasks` WRITE;
/*!40000 ALTER TABLE `field_tasks` DISABLE KEYS */;
/*!40000 ALTER TABLE `field_tasks` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `public_warnings`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `public_warnings` (
  `id` int NOT NULL AUTO_INCREMENT,
  `report_id` int NOT NULL,
  `level` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `affected_area` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `radius_km` int NOT NULL,
  `message` text COLLATE utf8mb4_unicode_ci NOT NULL,
  `recommended_action` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL,
  `valid_hours` int NOT NULL,
  `status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `issued_by` int DEFAULT NULL,
  `signature` text COLLATE utf8mb4_unicode_ci,
  `issued_at` datetime NOT NULL,
  `expires_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_warning_status_expiry` (`status`,`expires_at`),
  KEY `fk_warning_report` (`report_id`),
  KEY `fk_warning_user` (`issued_by`),
  CONSTRAINT `fk_warning_report` FOREIGN KEY (`report_id`) REFERENCES `disaster_reports` (`id`),
  CONSTRAINT `fk_warning_user` FOREIGN KEY (`issued_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `public_warnings` WRITE;
/*!40000 ALTER TABLE `public_warnings` DISABLE KEYS */;
/*!40000 ALTER TABLE `public_warnings` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `responders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `responders` (
  `id` int NOT NULL AUTO_INCREMENT,
  `name` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(60) COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `department_id` int NOT NULL,
  `username` varchar(30) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `available` tinyint(1) NOT NULL DEFAULT '1',
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  KEY `fk_responder_dept` (`department_id`),
  CONSTRAINT `fk_responder_dept` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `responders` WRITE;
/*!40000 ALTER TABLE `responders` DISABLE KEYS */;
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (1,'John Smith','Firefighter','0401-001-001',1,'jsmith',1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (2,'Maria Garcia','Fire Captain','0401-001-002',1,'mgarcia',1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (3,'Dr. James Lee','Doctor','0401-002-001',2,'jlee',1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (4,'Anna Brown','Paramedic','0401-002-002',2,'abrown',1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (5,'Mike Johnson','Electrician','0401-003-001',3,'mjohnson',1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (6,'Sarah Williams','Electrical Engineer','0401-003-002',3,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (7,'Tom Davis','Traffic Controller','0401-004-001',4,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (8,'Lisa Martinez','Road Engineer','0401-004-002',4,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (9,'Paul Wilson','Waste Technician','0401-005-001',5,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (10,'Rachel Anderson','Sanitation Supervisor','0401-005-002',5,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (11,'David Taylor','Plumber','0401-006-001',6,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (12,'Emma Thomas','Water Engineer','0401-006-002',6,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (13,'Robert Jackson','Principal','0401-007-001',7,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (14,'Susan Harris','Welfare Officer','0401-007-002',7,NULL,1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (15,'Officer Chris White','Police Officer','0401-008-001',8,'cwhite',1,'2026-10-12 07:06:29');
INSERT INTO `responders` (`id`, `name`, `role`, `phone`, `department_id`, `username`, `available`, `updated_at`) VALUES (16,'Sergeant Pat Lewis','Sergeant','0401-008-002',8,NULL,1,'2026-10-12 07:06:29');
/*!40000 ALTER TABLE `responders` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `response_departments`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `response_departments` (
  `response_id` int NOT NULL,
  `department_id` int NOT NULL,
  PRIMARY KEY (`response_id`,`department_id`),
  KEY `fk_rd_department` (`department_id`),
  CONSTRAINT `fk_rd_department` FOREIGN KEY (`department_id`) REFERENCES `departments` (`id`),
  CONSTRAINT `fk_rd_response` FOREIGN KEY (`response_id`) REFERENCES `disaster_responses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `response_departments` WRITE;
/*!40000 ALTER TABLE `response_departments` DISABLE KEYS */;
/*!40000 ALTER TABLE `response_departments` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `response_responders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `response_responders` (
  `response_id` int NOT NULL,
  `responder_id` int NOT NULL,
  PRIMARY KEY (`response_id`,`responder_id`),
  KEY `fk_rr_responder` (`responder_id`),
  CONSTRAINT `fk_rr_responder` FOREIGN KEY (`responder_id`) REFERENCES `responders` (`id`),
  CONSTRAINT `fk_rr_response` FOREIGN KEY (`response_id`) REFERENCES `disaster_responses` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `response_responders` WRITE;
/*!40000 ALTER TABLE `response_responders` DISABLE KEYS */;
/*!40000 ALTER TABLE `response_responders` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `task_status_updates`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `task_status_updates` (
  `id` int NOT NULL AUTO_INCREMENT,
  `task_id` int NOT NULL,
  `from_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `to_status` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `updated_by` int DEFAULT NULL,
  `signature` text COLLATE utf8mb4_unicode_ci,
  `signature_verified` tinyint(1) NOT NULL DEFAULT '0',
  `updated_at` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_update_task` (`task_id`),
  KEY `fk_update_user` (`updated_by`),
  CONSTRAINT `fk_update_task` FOREIGN KEY (`task_id`) REFERENCES `field_tasks` (`id`),
  CONSTRAINT `fk_update_user` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `task_status_updates` WRITE;
/*!40000 ALTER TABLE `task_status_updates` DISABLE KEYS */;
/*!40000 ALTER TABLE `task_status_updates` ENABLE KEYS */;
UNLOCK TABLES;
DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
  `id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(30) COLLATE utf8mb4_unicode_ci NOT NULL,
  `full_name` varchar(150) COLLATE utf8mb4_unicode_ci NOT NULL,
  `role` varchar(20) COLLATE utf8mb4_unicode_ci NOT NULL,
  `password_hash` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `salt` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `public_key` text COLLATE utf8mb4_unicode_ci,
  `created_at` datetime NOT NULL,
  `last_login` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (1,'admin','System Administrator','ADMIN','wXrCHCkViKvUBcTUnP5Pr/1Cl6KWC61PwAGjJEoDAXw=','xLkZu/YR81nF+Wt9ZMk0Mg==',NULL,'2026-10-12 07:06:27',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (2,'controller','Riley Morgan','CONTROLLER','/pm3OXzS8fh9ej/iPsybBzzFA4XAET8dFGNWjWxCdWk=','6rKc15EM10ZyehUt2T2tRA==',NULL,'2026-10-12 07:06:27',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (3,'controller2','Sam Okafor','CONTROLLER','ysaJMSp178lHwm7bPHVxsuNp+4ofUB89yx1hfTWe67E=','O+3BEtuk8SYleJ7Wdiy5WQ==',NULL,'2026-10-12 07:06:28',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (4,'jsmith','John Smith','RESPONDER','9sDJKzs+yRPz4aDmXzqgD22FmA7gSBcgq+r04KJIsGg=','OirGMR1FRAc4GUlpCEJ6nQ==',NULL,'2026-10-12 07:06:28',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (5,'mgarcia','Maria Garcia','RESPONDER','ifSwwaY9RyxAjpEzznTaQK5ncTyIyzj2a0CWtkrFanw=','lmZ3uB9dkaBrgMAqpgIWHA==',NULL,'2026-10-12 07:06:28',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (6,'jlee','Dr. James Lee','RESPONDER','3cTCkTf57BV9WyRqr2VYJ1oaDPEaMYubcvF5L0YfaHE=','sxc2pQtOgt5cZ4vf21LxsQ==',NULL,'2026-10-12 07:06:28',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (7,'abrown','Anna Brown','RESPONDER','6Ug9skW4JX8jWMU/yN29YjtJf9PDFVKc1EPR/G8hL+0=','ZWe6yIpwx0s8fI0MJfO9AA==',NULL,'2026-10-12 07:06:28',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (8,'mjohnson','Mike Johnson','RESPONDER','AOr4DPsWzSkfUC6K/jwnRdZ4P29HclBejmrYvBiE76s=','2zvnQxie+22SZmkDIi19yw==',NULL,'2026-10-12 07:06:29',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (9,'cwhite','Officer Chris White','RESPONDER','MtJHI6W9mzYrvNk5WgFytXEJpLouYmbVPwWBbDuT0xI=','QtPBT0uGsuiRAbo62Bu7Ug==',NULL,'2026-10-12 07:06:29',NULL);
INSERT INTO `users` (`id`, `username`, `full_name`, `role`, `password_hash`, `salt`, `public_key`, `created_at`, `last_login`) VALUES (10,'citizen','Jane Doe','CITIZEN','WNRfSnzr8nSn8gYGOsWu3wEs7DKbpiUG8y4yJR0KbUA=','sTWYuMNwkFctY7o6St5vjA==',NULL,'2026-10-12 07:06:29',NULL);
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

