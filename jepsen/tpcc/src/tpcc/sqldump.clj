(ns tpcc.sqldump)

(def sqldump "
-- MariaDB dump 10.19  Distrib 10.5.9-MariaDB, for Linux (x86_64)
--
-- Host: 127.0.0.1    Database: tpcc
-- ------------------------------------------------------
-- Server version	10.5.9-MariaDB-1:10.5.9+maria~focal

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `CUSTOMER`
--

DROP TABLE IF EXISTS `CUSTOMER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `CUSTOMER` (
  `C_W_ID` int(11) NOT NULL,
  `C_D_ID` int(11) NOT NULL,
  `C_ID` int(11) NOT NULL,
  `C_DISCOUNT` decimal(4,4) NOT NULL,
  `C_CREDIT` char(2) NOT NULL,
  `C_LAST` varchar(16) NOT NULL,
  `C_FIRST` varchar(16) NOT NULL,
  `C_CREDIT_LIM` decimal(12,2) NOT NULL,
  `C_BALANCE` decimal(12,2) NOT NULL,
  `C_YTD_PAYMENT` float NOT NULL,
  `C_PAYMENT_CNT` int(11) NOT NULL,
  `C_DELIVERY_CNT` int(11) NOT NULL,
  `C_STREET_1` varchar(20) NOT NULL,
  `C_STREET_2` varchar(20) NOT NULL,
  `C_CITY` varchar(20) NOT NULL,
  `C_STATE` char(2) NOT NULL,
  `C_ZIP` char(9) NOT NULL,
  `C_PHONE` char(16) NOT NULL,
  `C_SINCE` timestamp NOT NULL DEFAULT current_timestamp(),
  `C_MIDDLE` char(2) NOT NULL,
  `C_DATA` varchar(500) NOT NULL,
  PRIMARY KEY (`C_W_ID`,`C_D_ID`,`C_ID`),
  KEY `IDX_CUSTOMER_NAME` (`C_W_ID`,`C_D_ID`,`C_LAST`,`C_FIRST`),
  CONSTRAINT `FKEY_CUSTOMER_1` FOREIGN KEY (`C_W_ID`, `C_D_ID`) REFERENCES `DISTRICT` (`D_W_ID`, `D_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `CUSTOMER`
--

LOCK TABLES `CUSTOMER` WRITE;
/*!40000 ALTER TABLE `CUSTOMER` DISABLE KEYS */;
INSERT INTO `CUSTOMER` VALUES (1,1,1,0.4454,'GC','BARBARBAR','ueznwnwdcacdrlp',50000.00,-10.00,10,1,0,'phjqqkmofdqspkknu','fborjlbpcbiwtsjnprb','ittkjywahw','KZ','344511111','4588564228951770','2021-04-01 10:15:30','OE','vgxgivvcpfhixaaczxzlfdmoossntkupjqeirrqamuctblbsvpddyoihaoodznjtvhdciegtavmyzocivskunsmxekbewnzprpwpujabjxnqboxshoegmjsnbutgtifveqpsybdxexorpqddodzgbeloistrwxmeywvvhgmjkwljcchpkafraszeyqzcvlfslowtlbmppwppfpqsazptulstcdmodykzgsrfqtrftgcnmnxqqiyvuqzhvniphzwvbsgobyifdnnxutbbquynxozcsicgrtzssrhrojrgbhmheqjrdloqbeptobmylmigppdpolteuvdgatcgypqogoyyeskegblocbiyslqeygccipbxpnspkdytbewdhbhwvdplovhjpnygjuhkkhdasnfgzdaiwwqe'),(1,1,2,0.4064,'BC','BARBARBAR','fxrvmzqtkr',50000.00,-10.00,10,1,0,'fubwiqpzqtjbnwgd','vetqvpewvpni','dkzqouplqfeswnpi','KE','362111111','2428208762110070','2021-04-01 10:15:30','OE','ldkbszneaoljhtofhwevauagxmbutqqcvtkaqckksyuqzzstbffrtyzzvycgmkofwntrjlmzgmqqglmieqtsvyyydkoqthggqqyvbxyryjdafjlduxzmjbudonbqwdsantpwyjdcibaghrpphwpawxyxmjicvpcbfjxsovrtmflyufdtxkpvrrjfdkhotsxjagqziwzdmjjzccilkzlagfitofauaidjynifrwaviipzpakhabrqjeybwyfcrkxgzimcwmjvsxikymefxdvtccxkykaaafgjmsyyugrtwtdbdidnxagtifwbpdciztyerdcqdtfgkzojunaxncekqzsaheqlnlklewwwnfxxihyywriehptcdencgjctejzbclkywcumyjfsvdugpebyjyhvrpgwgtozsvit'),(1,1,3,0.0714,'GC','BARBARBAR','dthfzuhtxmee',50000.00,-10.00,10,1,0,'anxxzfyeqyokhrnerue','nydabzrwcextrmhtlj','xxibejrikkvu','QQ','316111111','3789341457100183','2021-04-01 10:15:30','OE','beztgrcbdwzrwcwpketonhksyqtdmjbgduskrzvgbutrczgtysykztihnnxwlxiihvfbywjkibexcexqvwmbzqabatwlmazovexbivoxmvlwejnevqzotzpplsanworqjjwazgxtirdprshydtmjrjfzllexzmuftctqbespsujiehbsaebkacledtefmqosmsrdjjeodspobkrsqegdevdnjpgcokbprwxtqxzswecdgxnhiwcgpnarmozfejosnhspybmahjqoadpqlgxqawnhkaqgntnxygvftlnnmlibxuwtpkufolkaazbikcjduznlkbwakdmlocqibfvdcduwvgceenwhvvvdtxbcuchdduohbpzmtpqcxxiaycdzefgntboifehfgalnd'),(1,2,1,0.4242,'GC','BARBARBAR','zexmlmjhmzahsa',50000.00,-10.00,10,1,0,'qkdjxypoitqhrblaz','jzcpkfktctozlqknwv','ylicseyrfngvocojo','AF','121311111','3234757387407219','2021-04-01 10:15:30','OE','tkvlduxapdiathtaeuhiwimwsfvmhkihhzqgojxfbgdoamegjpvacphwjbeaciwiwqvrqrzoahcikjizbmevglrsiotmicicppwhygpjjpwrbkhwnnixtpcogmhrgfutmuxivyozghpvbeydrxzzdvyzezucoxpixsgecpkqgkkioikthmfxxompqybzjtyzjpjtdtkadcnsncyfmmeyqnoygsgllgiieteuaubzfzbjgficcjnzmwqnqzerswknghqlfrmgzhiycedzgiohxipijfzpoqhgcfhdynavgfslgxjajycsaqtfcdxevbkmjfcbduyrgolagrhilkzbwhbwpgqgjxynvsmaddlmlxpbjxdxqomqptqxvsufjnkvocbjcsnaakxjozqlwfbnzblyklxeucribzgadmpsvzaerqjoaumihcghubxurkusijgmxcvkmqvlsbjgqzfbeknvoyujqefmxnvyyzmmgjjbefnw'),(1,2,2,0.0223,'GC','BARBARBAR','uqlshgkicijjk',50000.00,-10.00,10,1,0,'unqzmzauzgh','hcsqsmssfo','yvtaegkvtpatr','VT','756911111','4176084738615400','2021-04-01 10:15:30','OE','trgdpcfsdwgqotujviativvhsfkhepvknlaaljsmizfimimjhvypvdxkgcjsphufucqlrwqsaslcsteieeinpwnlkavbzcvaawonvqiqwvuhxwsgbajqobqeixgwgxyherkdzywfkkcgvrsyhsvpxvekjxfcmpamrkpojacyrrgmgdimmjizpixkhlflrofgiznowuffmpfyxipltnibkgeykucurynrojwseslzhjazsosbxsemrmfswgvxhneyztipfvmbnswgouxbrybxekbmblthrkbmwdgoxqthxgrgcmghkeobvdebqdmxxampokxnudgnevpwo'),(1,2,3,0.2499,'GC','BARBARBAR','ecafwnjjbp',50000.00,-10.00,10,1,0,'uvaveypsoktofcvmhlr','redeuocjzm','hhgoztahglnjg','IA','430911111','6143942661912944','2021-04-01 10:15:30','OE','psvnloqrziqekbnhvxijahvahrsaxmmvgshidofqtyzqesaolafvzsruozuxtrlolisqcmwrdrznugxpkecerdfvzwhgsfihepmuebsedtkjyvhhtiwyvcqexuzuptxpqzyatuhdvrexbtkoecvxesadsjjmuzyvkwargzrdbwywbnfguqtigrikviphyfzhwbynvxghmboeqctsotojrxjevwtnqxcrlbaeoyoceoazeyxzczufndcggkbnebshyjlbjgaypiluikoqjbcrajczzbnfyepdvcdcjfsjsfyzqwpxuxwkvcebfccriwqtpzdmwuahlgregfzateadjkkasscaqkiaxjwmujcuudjqzjsobmuha');
/*!40000 ALTER TABLE `CUSTOMER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `DISTRICT`
--

DROP TABLE IF EXISTS `DISTRICT`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `DISTRICT` (
  `D_W_ID` int(11) NOT NULL,
  `D_ID` int(11) NOT NULL,
  `D_YTD` decimal(12,2) NOT NULL,
  `D_TAX` decimal(4,4) NOT NULL,
  `D_NEXT_O_ID` int(11) NOT NULL,
  `D_NAME` varchar(10) NOT NULL,
  `D_STREET_1` varchar(20) NOT NULL,
  `D_STREET_2` varchar(20) NOT NULL,
  `D_CITY` varchar(20) NOT NULL,
  `D_STATE` char(2) NOT NULL,
  `D_ZIP` char(9) NOT NULL,
  PRIMARY KEY (`D_W_ID`,`D_ID`),
  CONSTRAINT `FKEY_DISTRICT_1` FOREIGN KEY (`D_W_ID`) REFERENCES `WAREHOUSE` (`W_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `DISTRICT`
--

LOCK TABLES `DISTRICT` WRITE;
/*!40000 ALTER TABLE `DISTRICT` DISABLE KEYS */;
INSERT INTO `DISTRICT` VALUES (1,1,30.00,0.0626,4,'wmkrdc','bxkfuilwdhbfxrfa','prugdflpd','hxxcxcuplwgd','PH','123456789'),(1,2,30.00,0.1145,4,'mjgmtv','fqqfvcupofyw','duebickpzkhkvmcj','wvktxbkapw','PE','123456789');
/*!40000 ALTER TABLE `DISTRICT` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `HISTORY`
--

DROP TABLE IF EXISTS `HISTORY`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `HISTORY` (
  `H_C_ID` int(11) NOT NULL,
  `H_C_D_ID` int(11) NOT NULL,
  `H_C_W_ID` int(11) NOT NULL,
  `H_D_ID` int(11) NOT NULL,
  `H_W_ID` int(11) NOT NULL,
  `H_DATE` timestamp NOT NULL DEFAULT current_timestamp(),
  `H_AMOUNT` decimal(6,2) NOT NULL,
  `H_DATA` varchar(24) NOT NULL,
  KEY `FKEY_HISTORY_1` (`H_C_W_ID`,`H_C_D_ID`,`H_C_ID`),
  KEY `FKEY_HISTORY_2` (`H_W_ID`,`H_D_ID`),
  CONSTRAINT `FKEY_HISTORY_1` FOREIGN KEY (`H_C_W_ID`, `H_C_D_ID`, `H_C_ID`) REFERENCES `CUSTOMER` (`C_W_ID`, `C_D_ID`, `C_ID`) ON DELETE CASCADE,
  CONSTRAINT `FKEY_HISTORY_2` FOREIGN KEY (`H_W_ID`, `H_D_ID`) REFERENCES `DISTRICT` (`D_W_ID`, `D_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `HISTORY`
--

LOCK TABLES `HISTORY` WRITE;
/*!40000 ALTER TABLE `HISTORY` DISABLE KEYS */;
INSERT INTO `HISTORY` VALUES (1,1,1,1,1,'2021-04-01 10:15:30',10.00,'pbrjkdgosafaprlw'),(2,1,1,1,1,'2021-04-01 10:15:30',10.00,'efjwefyvihcgnou'),(3,1,1,1,1,'2021-04-01 10:15:30',10.00,'waznutnins'),(1,2,1,2,1,'2021-04-01 10:15:30',10.00,'modfaxschctdndkgyxxbz'),(2,2,1,2,1,'2021-04-01 10:15:30',10.00,'zhopzxtlmcbyxsjtjg'),(3,2,1,2,1,'2021-04-01 10:15:30',10.00,'vdzjvqvlnu');
/*!40000 ALTER TABLE `HISTORY` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ITEM`
--

DROP TABLE IF EXISTS `ITEM`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ITEM` (
  `I_ID` int(11) NOT NULL,
  `I_NAME` varchar(24) NOT NULL,
  `I_PRICE` decimal(5,2) NOT NULL,
  `I_DATA` varchar(50) NOT NULL,
  `I_IM_ID` int(11) NOT NULL,
  PRIMARY KEY (`I_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ITEM`
--

LOCK TABLES `ITEM` WRITE;
/*!40000 ALTER TABLE `ITEM` DISABLE KEYS */;
INSERT INTO `ITEM` VALUES (1,'sxvnjhpdqdxvc',32.82,'astvybcwvmgnykrxvzxkgxtspsjdgyluegq',3657);
/*!40000 ALTER TABLE `ITEM` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `NEW_ORDER`
--

DROP TABLE IF EXISTS `NEW_ORDER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `NEW_ORDER` (
  `NO_W_ID` int(11) NOT NULL,
  `NO_D_ID` int(11) NOT NULL,
  `NO_O_ID` int(11) NOT NULL,
  PRIMARY KEY (`NO_W_ID`,`NO_D_ID`,`NO_O_ID`),
  CONSTRAINT `FKEY_NEW_ORDER_1` FOREIGN KEY (`NO_W_ID`, `NO_D_ID`, `NO_O_ID`) REFERENCES `OORDER` (`O_W_ID`, `O_D_ID`, `O_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `NEW_ORDER`
--

LOCK TABLES `NEW_ORDER` WRITE;
/*!40000 ALTER TABLE `NEW_ORDER` DISABLE KEYS */;
INSERT INTO `NEW_ORDER` VALUES (1,1,3),(1,2,3);
/*!40000 ALTER TABLE `NEW_ORDER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `OORDER`
--

DROP TABLE IF EXISTS `OORDER`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `OORDER` (
  `O_W_ID` int(11) NOT NULL,
  `O_D_ID` int(11) NOT NULL,
  `O_ID` int(11) NOT NULL,
  `O_C_ID` int(11) NOT NULL,
  `O_CARRIER_ID` int(11) DEFAULT NULL,
  `O_OL_CNT` decimal(2,0) NOT NULL,
  `O_ALL_LOCAL` decimal(1,0) NOT NULL,
  `O_ENTRY_D` timestamp NOT NULL DEFAULT current_timestamp(),
  PRIMARY KEY (`O_W_ID`,`O_D_ID`,`O_ID`),
  UNIQUE KEY `O_W_ID` (`O_W_ID`,`O_D_ID`,`O_C_ID`,`O_ID`),
  KEY `IDX_ORDER` (`O_W_ID`,`O_D_ID`,`O_C_ID`,`O_ID`),
  CONSTRAINT `FKEY_ORDER_1` FOREIGN KEY (`O_W_ID`, `O_D_ID`, `O_C_ID`) REFERENCES `CUSTOMER` (`C_W_ID`, `C_D_ID`, `C_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `OORDER`
--

LOCK TABLES `OORDER` WRITE;
/*!40000 ALTER TABLE `OORDER` DISABLE KEYS */;
INSERT INTO `OORDER` VALUES (1,1,1,3,9,15,1,'2021-04-01 10:15:30'),(1,1,2,1,9,5,1,'2021-04-01 10:15:30'),(1,1,3,2,NULL,5,1,'2021-04-01 10:15:30'),(1,2,1,3,9,13,1,'2021-04-01 10:15:30'),(1,2,2,1,7,13,1,'2021-04-01 10:15:30'),(1,2,3,2,NULL,11,1,'2021-04-01 10:15:30');
/*!40000 ALTER TABLE `OORDER` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ORDER_LINE`
--

DROP TABLE IF EXISTS `ORDER_LINE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `ORDER_LINE` (
  `OL_W_ID` int(11) NOT NULL,
  `OL_D_ID` int(11) NOT NULL,
  `OL_O_ID` int(11) NOT NULL,
  `OL_NUMBER` int(11) NOT NULL,
  `OL_I_ID` int(11) NOT NULL,
  `OL_DELIVERY_D` timestamp NULL DEFAULT NULL,
  `OL_AMOUNT` decimal(6,2) NOT NULL,
  `OL_SUPPLY_W_ID` int(11) NOT NULL,
  `OL_QUANTITY` decimal(2,0) NOT NULL,
  `OL_DIST_INFO` char(24) NOT NULL,
  PRIMARY KEY (`OL_W_ID`,`OL_D_ID`,`OL_O_ID`,`OL_NUMBER`),
  KEY `FKEY_ORDER_LINE_2` (`OL_SUPPLY_W_ID`,`OL_I_ID`),
  -- CONSTRAINT `FKEY_ORDER_LINE_1` FOREIGN KEY (`OL_W_ID`, `OL_D_ID`, `OL_O_ID`) REFERENCES `OORDER` (`O_W_ID`, `O_D_ID`, `O_ID`) ON DELETE CASCADE,
  CONSTRAINT `FKEY_ORDER_LINE_2` FOREIGN KEY (`OL_SUPPLY_W_ID`, `OL_I_ID`) REFERENCES `STOCK` (`S_W_ID`, `S_I_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ORDER_LINE`
--

LOCK TABLES `ORDER_LINE` WRITE;
/*!40000 ALTER TABLE `ORDER_LINE` DISABLE KEYS */;
INSERT INTO `ORDER_LINE` VALUES (1,1,1,1,1,'2021-04-01 10:15:30',0.00,1,5,'bheyvrkdbcgjrdorserfjqx'),(1,1,1,2,1,'2021-04-01 10:15:30',0.00,1,5,'hpklleasuvykwxwhnoiwfll'),(1,1,1,3,1,'2021-04-01 10:15:30',0.00,1,5,'pqklokufpofnljpdoamxoda'),(1,1,1,4,1,'2021-04-01 10:15:30',0.00,1,5,'kuarvidwgzptugghaiqpiwt'),(1,1,1,5,1,'2021-04-01 10:15:30',0.00,1,5,'ahfjiutexgxarrcwcpildgn'),(1,1,1,6,1,'2021-04-01 10:15:30',0.00,1,5,'duwdslpyseimwfeynpxntxz'),(1,1,1,7,1,'2021-04-01 10:15:30',0.00,1,5,'ikuzzpfbmoqqzgrtrhdyttx'),(1,1,1,8,1,'2021-04-01 10:15:30',0.00,1,5,'nglpvlnbvwaxjiuqkawsfkr'),(1,1,1,9,1,'2021-04-01 10:15:30',0.00,1,5,'jxubbkjhgsympucnwthsnaf'),(1,1,1,10,1,'2021-04-01 10:15:30',0.00,1,5,'cysfktrqzgzcctclzqhqblu'),(1,1,1,11,1,'2021-04-01 10:15:30',0.00,1,5,'aulguchlfcobtgvxqprfcqe'),(1,1,1,12,1,'2021-04-01 10:15:30',0.00,1,5,'xqfjtzimulkhoukizhkojcj'),(1,1,1,13,1,'2021-04-01 10:15:30',0.00,1,5,'pbpverzvrdhyzkdkbcwgnfc'),(1,1,1,14,1,'2021-04-01 10:15:30',0.00,1,5,'alcnqqmtfqtggspcdhnqtxf'),(1,1,1,15,1,'2021-04-01 10:15:30',0.00,1,5,'lxhxasbcxswthbjhvuiahck'),(1,1,2,1,1,'2021-04-01 10:15:30',0.00,1,5,'iynaszzrnykkjslbrclmzrx'),(1,1,2,2,1,'2021-04-01 10:15:30',0.00,1,5,'cqcykffwksfkuveldafgvuc'),(1,1,2,3,1,'2021-04-01 10:15:30',0.00,1,5,'fzpqqfwivgvojpjlilfsgag'),(1,1,2,4,1,'2021-04-01 10:15:30',0.00,1,5,'ajztrzfuceobnowdraiyjfx'),(1,1,2,5,1,'2021-04-01 10:15:30',0.00,1,5,'sibucykjcnqvxbirtvrpurh'),(1,1,3,1,1,NULL,5051.08,1,5,'hylkpefwzhpfjwkwuwsltzi'),(1,1,3,2,1,NULL,8264.63,1,5,'joinorabyuxwvarcoyiuxpe'),(1,1,3,3,1,NULL,9814.72,1,5,'bwiffuihehjzhkybjeadxcw'),(1,1,3,4,1,NULL,3908.98,1,5,'qwgimowtoowxrfyuacwlqbm'),(1,1,3,5,1,NULL,5781.15,1,5,'rfsivlypymlxqoieeitzrdf'),(1,2,1,1,1,'2021-04-01 10:15:30',0.00,1,5,'yyzohxvhsrozryixvazfmjo'),(1,2,1,2,1,'2021-04-01 10:15:30',0.00,1,5,'acoebkyeudumkvzwvktomgt'),(1,2,1,3,1,'2021-04-01 10:15:30',0.00,1,5,'oprqzohjberrejvzrlqedmm'),(1,2,1,4,1,'2021-04-01 10:15:30',0.00,1,5,'sudjfqrcetiydqxzkconblu'),(1,2,1,5,1,'2021-04-01 10:15:30',0.00,1,5,'duxyyewkqtekwfpkyswqump'),(1,2,1,6,1,'2021-04-01 10:15:30',0.00,1,5,'ugtqqfgenszsfqqqhixqizh'),(1,2,1,7,1,'2021-04-01 10:15:30',0.00,1,5,'pmsxxvewuvbhyksxtbidqjv'),(1,2,1,8,1,'2021-04-01 10:15:30',0.00,1,5,'udwggbyhygyongyzxtcbwbs'),(1,2,1,9,1,'2021-04-01 10:15:30',0.00,1,5,'yyzogyswsaxhuumqakajglq'),(1,2,1,10,1,'2021-04-01 10:15:30',0.00,1,5,'pmxumowbhbbshanfaxnzpau'),(1,2,1,11,1,'2021-04-01 10:15:30',0.00,1,5,'aihazhxcwmhezszklxoumuo'),(1,2,1,12,1,'2021-04-01 10:15:30',0.00,1,5,'pwfybhfzuaudvbfgrrntbma'),(1,2,1,13,1,'2021-04-01 10:15:30',0.00,1,5,'rbzdvslnmzxmckttpifluli'),(1,2,2,1,1,'2021-04-01 10:15:30',0.00,1,5,'kmgjmgfyyoyaovwvkhlcytz'),(1,2,2,2,1,'2021-04-01 10:15:30',0.00,1,5,'xhkfdvtgolxdudhzdcfvgef'),(1,2,2,3,1,'2021-04-01 10:15:30',0.00,1,5,'mgsjchvgpteywkbkeqwsnrv'),(1,2,2,4,1,'2021-04-01 10:15:30',0.00,1,5,'jrhmneddzcjsivdxllzswko'),(1,2,2,5,1,'2021-04-01 10:15:30',0.00,1,5,'ollmnvfiblayaseyupuppxc'),(1,2,2,6,1,'2021-04-01 10:15:30',0.00,1,5,'qybrngimfkfskfnhxqzikkl'),(1,2,2,7,1,'2021-04-01 10:15:30',0.00,1,5,'ibvilklctlnpenklvjflylg'),(1,2,2,8,1,'2021-04-01 10:15:30',0.00,1,5,'nwxwhzzwxlwbcrhzvhiszof'),(1,2,2,9,1,'2021-04-01 10:15:30',0.00,1,5,'zpbulsfxkfqnjigjybcgfpy'),(1,2,2,10,1,'2021-04-01 10:15:30',0.00,1,5,'faeruhazasrtdewauigwiqp'),(1,2,2,11,1,'2021-04-01 10:15:30',0.00,1,5,'zpciajoqitxrtlratehmsma'),(1,2,2,12,1,'2021-04-01 10:15:30',0.00,1,5,'klzouiunvhykasmupwdukas'),(1,2,2,13,1,'2021-04-01 10:15:30',0.00,1,5,'ngebwcaydaszjzlwybmexfn'),(1,2,3,1,1,NULL,2182.23,1,5,'xnhovycexdpnpwyzlnbpjjw'),(1,2,3,2,1,NULL,7344.05,1,5,'nhqkfesidfmtmbazwzusvjq'),(1,2,3,3,1,NULL,4352.75,1,5,'tsfyuxdqictiewdoytgtzoh'),(1,2,3,4,1,NULL,6431.54,1,5,'kbfibxjpbixdmqynnguivaw'),(1,2,3,5,1,NULL,2238.32,1,5,'vhcpgtphktdqnvktmtewerw'),(1,2,3,6,1,NULL,9027.19,1,5,'bfjviwqeoaabqqeyklooghb'),(1,2,3,7,1,NULL,401.28,1,5,'nvhhdnqztsilotwiooobvyh'),(1,2,3,8,1,NULL,7783.87,1,5,'fgeapsvhxenjvcrwimwkkha'),(1,2,3,9,1,NULL,729.94,1,5,'tofmcqkkvojjehrizeqsdnf'),(1,2,3,10,1,NULL,8212.62,1,5,'tjidkamquaputqwjjejnmkp'),(1,2,3,11,1,NULL,1569.15,1,5,'soyaspijzqeynpnojxrajns');
/*!40000 ALTER TABLE `ORDER_LINE` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `STOCK`
--

DROP TABLE IF EXISTS `STOCK`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `STOCK` (
  `S_W_ID` int(11) NOT NULL,
  `S_I_ID` int(11) NOT NULL,
  `S_QUANTITY` decimal(4,0) NOT NULL,
  `S_YTD` decimal(8,2) NOT NULL,
  `S_ORDER_CNT` int(11) NOT NULL,
  `S_REMOTE_CNT` int(11) NOT NULL,
  `S_DATA` varchar(50) NOT NULL,
  `S_DIST_01` char(24) NOT NULL,
  `S_DIST_02` char(24) NOT NULL,
  `S_DIST_03` char(24) NOT NULL,
  `S_DIST_04` char(24) NOT NULL,
  `S_DIST_05` char(24) NOT NULL,
  `S_DIST_06` char(24) NOT NULL,
  `S_DIST_07` char(24) NOT NULL,
  `S_DIST_08` char(24) NOT NULL,
  `S_DIST_09` char(24) NOT NULL,
  `S_DIST_10` char(24) NOT NULL,
  PRIMARY KEY (`S_W_ID`,`S_I_ID`),
  KEY `FKEY_STOCK_2` (`S_I_ID`),
  CONSTRAINT `FKEY_STOCK_1` FOREIGN KEY (`S_W_ID`) REFERENCES `WAREHOUSE` (`W_ID`) ON DELETE CASCADE,
  CONSTRAINT `FKEY_STOCK_2` FOREIGN KEY (`S_I_ID`) REFERENCES `ITEM` (`I_ID`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `STOCK`
--

LOCK TABLES `STOCK` WRITE;
/*!40000 ALTER TABLE `STOCK` DISABLE KEYS */;
INSERT INTO `STOCK` VALUES (1,1,46,0.00,0,0,'zxzozitkxjbozwdjmcbosyqqkcprrdczwmrlfxblg','prpgrntaqoosvxpkjpjlavs','ccrxfrrollhwhohfgcfwpnd','mwcsshwxqqykalaawcmxylm','algdeskkteesemprhrovkum','sxhelidqeoohoihegjoazbv','umchshgxzyxxqruicrijgqe','bwaxabqrirugzjuuvfyqovc','edxyfprlgsgzxsniavodtjk','qwhnwvpsamzkoudtwhiorjs','ziqypczmbywkdikokyngwpx');
/*!40000 ALTER TABLE `STOCK` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `WAREHOUSE`
--

DROP TABLE IF EXISTS `WAREHOUSE`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!40101 SET character_set_client = utf8 */;
CREATE TABLE `WAREHOUSE` (
  `W_ID` int(11) NOT NULL,
  `W_YTD` decimal(12,2) NOT NULL,
  `W_TAX` decimal(4,4) NOT NULL,
  `W_NAME` varchar(10) NOT NULL,
  `W_STREET_1` varchar(20) NOT NULL,
  `W_STREET_2` varchar(20) NOT NULL,
  `W_CITY` varchar(20) NOT NULL,
  `W_STATE` char(2) NOT NULL,
  `W_ZIP` char(9) NOT NULL,
  PRIMARY KEY (`W_ID`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `WAREHOUSE`
--

LOCK TABLES `WAREHOUSE` WRITE;
/*!40000 ALTER TABLE `WAREHOUSE` DISABLE KEYS */;
INSERT INTO `WAREHOUSE` VALUES (1,60.00,0.0977,'laqlocfl','bepowfnsomyarhaop','fojhhdxehxjbh','gsmzjgnlo','JV','123456789');
/*!40000 ALTER TABLE `WAREHOUSE` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2021-04-01 10:17:13

")