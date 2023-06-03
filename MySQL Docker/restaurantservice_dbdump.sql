-- Database export via SQLPro (https://www.sqlprostudio.com/allapps.html)
-- Exported by vi at 15-03-2023 17:05.
-- WARNING: This file may contain descructive statements such as DROPs.
-- Please ensure that you are running the script at the proper location.

USE restaurantservice;

-- BEGIN TABLE restaurant
DROP TABLE IF EXISTS restaurant;
CREATE TABLE `restaurant` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `location` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Inserting 4 rows into restaurant
-- Insert batch #1
INSERT INTO restaurant (id, location, name) VALUES
(1, 'Roma', 'Hostaria dell''Orso'),
(2, 'Roma', 'Baffetto'),
(3, 'Roma', 'L''Omo'),
(4, 'Milano', 'Seta');

-- END TABLE restaurant

-- BEGIN TABLE restaurant_menu_items
DROP TABLE IF EXISTS restaurant_menu_items;
CREATE TABLE `restaurant_menu_items` (
  `restaurant_id` bigint NOT NULL,
  `id` varchar(255) DEFAULT NULL,
  `name` varchar(255) DEFAULT NULL,
  `price` double NOT NULL,
  KEY `FKoj5vggogejym1yqbltb96fop5` (`restaurant_id`),
  CONSTRAINT `FKoj5vggogejym1yqbltb96fop5` FOREIGN KEY (`restaurant_id`) REFERENCES `restaurant` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Inserting 12 rows into restaurant_menu_items
-- Insert batch #1
INSERT INTO restaurant_menu_items (restaurant_id, id, name, price) VALUES
(1, 'CAR', 'Carbonara', 15),
(1, 'GRI', 'Gricia', 14),
(1, 'AMA', 'Amatriciana', 14),
(2, 'MAR', 'Pizza margherita', 6),
(2, 'CAP', 'Pizza Capricciosa', 8),
(2, 'FIO', 'Pizza fioridi zucca e alici', 7.5),
(3, 'CAR', 'Carbonara', 12),
(3, 'GRI', 'Gricia', 11),
(3, 'AMA', 'Amatriciana', 12),
(4, 'RIP', 'Risotto ai funghi porcini', 25),
(4, 'TAG', 'Tagliata di manzo', 35),
(4, 'CRE', 'Crema catalana', 12);

-- END TABLE restaurant_menu_items

