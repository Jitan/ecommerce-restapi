# ************************************************************
# Sequel Pro SQL dump
# Version 4135
#
# http://www.sequelpro.com/
# http://code.google.com/p/sequel-pro/
#
# Host: 127.0.0.1 (MySQL 5.6.22)
# Database: ecomm
# Generation Time: 2015-04-05 16:43:45 +0000
# ************************************************************


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


# Dump of table customer
# ------------------------------------------------------------

CREATE TABLE `customer` (
  `user_name` varchar(45) COLLATE utf8_swedish_ci NOT NULL DEFAULT '',
  `password` varchar(45) COLLATE utf8_swedish_ci NOT NULL DEFAULT '',
  `email` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `first_name` varchar(45) COLLATE utf8_swedish_ci NOT NULL DEFAULT '',
  `last_name` varchar(45) COLLATE utf8_swedish_ci NOT NULL DEFAULT '',
  `address` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `phone` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  PRIMARY KEY (`user_name`),
  UNIQUE KEY `user_name_UNIQUE` (`user_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_swedish_ci;



# Dump of table customer_cart
# ------------------------------------------------------------

CREATE TABLE `customer_cart` (
  `id_customer_cart` int(11) NOT NULL AUTO_INCREMENT,
  `id_product` int(11) NOT NULL,
  `user_name` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  PRIMARY KEY (`id_customer_cart`),
  KEY `id_item` (`id_product`),
  KEY `user_name` (`user_name`),
  CONSTRAINT `customer_cart_ibfk_1` FOREIGN KEY (`id_product`) REFERENCES `product` (`id_product`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `customer_cart_ibfk_2` FOREIGN KEY (`user_name`) REFERENCES `customer` (`user_name`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_swedish_ci;



# Dump of table order
# ------------------------------------------------------------

CREATE TABLE `order` (
  `id_order` int(11) NOT NULL AUTO_INCREMENT,
  `customer_name` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `created` date NOT NULL,
  `shipped` date DEFAULT NULL,
  PRIMARY KEY (`id_order`),
  UNIQUE KEY `id_order_UNIQUE` (`id_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_swedish_ci;



# Dump of table product
# ------------------------------------------------------------

CREATE TABLE `product` (
  `id_product` int(11) NOT NULL AUTO_INCREMENT,
  `title` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `category` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `manufacturer` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `description` varchar(100) COLLATE utf8_swedish_ci NOT NULL,
  `img` varchar(45) COLLATE utf8_swedish_ci NOT NULL,
  `price` double NOT NULL,
  `quantity` int(11) NOT NULL,
  PRIMARY KEY (`id_product`),
  UNIQUE KEY `id_product` (`id_product`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_swedish_ci;



# Dump of table product_order
# ------------------------------------------------------------

CREATE TABLE `product_order` (
  `id_product_order` int(11) NOT NULL AUTO_INCREMENT,
  `id_product` int(11) NOT NULL,
  `id_order` int(11) NOT NULL,
  PRIMARY KEY (`id_product_order`),
  KEY `id_order` (`id_order`),
  KEY `id_product` (`id_product`),
  CONSTRAINT `FK_id_order` FOREIGN KEY (`id_order`) REFERENCES `order` (`id_order`) ON DELETE CASCADE ON UPDATE CASCADE,
  CONSTRAINT `FK_id_product` FOREIGN KEY (`id_product`) REFERENCES `product` (`id_product`) ON DELETE CASCADE ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE=utf8_swedish_ci;




/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
