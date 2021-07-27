-- TODO: ipb_id
DROP TABLE IF EXISTS ipblocks;
CREATE TABLE ipblocks (
  ipb_id int NOT NULL,
  ipb_address varbinary(1024) NOT NULL,
  ipb_user int NOT NULL,
  ipb_by int NOT NULL,
  ipb_by_text varbinary(255) NOT NULL,
  ipb_reason varbinary(1024) NOT NULL,
  ipb_timestamp binary(14) NOT NULL,
  ipb_auto tinyint NOT NULL,
  ipb_anon_only tinyint NOT NULL,
  ipb_create_account tinyint NOT NULL ,
  ipb_enable_autoblock tinyint NOT NULL ,
  ipb_expiry varbinary(14) NOT NULL,
  ipb_range_start varbinary(1024) NOT NULL,
  ipb_range_end varbinary(1024) NOT NULL,
  ipb_deleted tinyint NOT NULL ,
  ipb_block_email tinyint NOT NULL ,
  ipb_allow_usertalk tinyint NOT NULL ,
  PRIMARY KEY (ipb_id),
);

-- TOOD: user_id
DROP TABLE IF EXISTS useracct;
CREATE TABLE useracct (
  user_id int NOT NULL,
  user_name varchar(255) NOT NULL,
  user_real_name varchar(255) NOT NULL,
  user_password varchar(1024) NOT NULL,
  user_newpassword varchar(1024) NOT NULL,
  user_newpass_time varchar(14) DEFAULT NULL,
  user_email varchar(1024) NOT NULL,
  user_options varchar(1024) NOT NULL,
  user_touched varchar(14) NOT NULL,
  user_token char(32) NOT NULL,
  user_email_authenticated char(14) DEFAULT NULL,
  user_email_token char(32) DEFAULT NULL,
  user_email_token_expires char(14) DEFAULT NULL,
  user_registration varchar(14) DEFAULT NULL,
  user_editcount int DEFAULT NULL,
  PRIMARY KEY (user_id),
);
CREATE INDEX IDX_USER_EMAIL_TOKEN ON useracct (user_email_token);

-- TODO: log_id
DROP TABLE IF EXISTS logging;
CREATE TABLE logging (
  log_id int NOT NULL,
  log_type varbinary(32) NOT NULL,
  log_action varbinary(32) NOT NULL,
  log_timestamp binary(14) NOT NULL,
  log_user int NOT NULL,
  log_namespace int NOT NULL,
  log_title varbinary(255) NOT NULL,
  log_comment varbinary(255) NOT NULL,
  log_params varbinary(1024) NOT NULL,
  log_deleted tinyint NOT NULL,
  log_user_text varbinary(255) NOT NULL,
  log_page int DEFAULT NULL,
  PRIMARY KEY (log_id)
);

-- TODO: page_id
DROP TABLE IF EXISTS page;
CREATE TABLE page (
  page_id int NOT NULL,
  page_namespace int NOT NULL,
  page_title varchar(255) NOT NULL,
  page_restrictions varchar(1024) NOT NULL,
  page_counter bigint NOT NULL,
  page_is_redirect tinyint NOT NULL,
  page_is_new tinyint NOT NULL,
  page_random double NOT NULL,
  page_touched binary(14) NOT NULL,
  page_latest int NOT NULL,
  page_len int NOT NULL,
  PRIMARY KEY (page_id),
);

-- TODO: page_id
DROP TABLE IF EXISTS page_backup;
CREATE TABLE page_backup (
  page_id int NOT NULL,
  page_namespace int NOT NULL,
  page_title varchar(255) NOT NULL,
  page_restrictions varchar(1024) NOT NULL,
  page_counter bigint NOT NULL,
  page_is_redirect tinyint NOT NULL,
  page_is_new tinyint NOT NULL,
  page_random double NOT NULL,
  page_touched binary(14) NOT NULL,
  page_latest int NOT NULL,
  page_len int NOT NULL,
  PRIMARY KEY (page_id),
);

DROP TABLE IF EXISTS page_restrictions;
CREATE TABLE page_restrictions (
  pr_page int NOT NULL,
  pr_type varbinary(60) NOT NULL,
  pr_level varbinary(60) NOT NULL,
  pr_cascade tinyint NOT NULL,
  pr_user int DEFAULT NULL,
  pr_expiry varbinary(14) DEFAULT NULL,
  pr_id int NOT NULL,
  PRIMARY KEY (pr_id),
  UNIQUE (pr_page,pr_type)
);

-- TOOD: rc_id
DROP TABLE IF EXISTS recentchanges;
CREATE TABLE recentchanges (
  rc_id int NOT NULL,
  rc_timestamp varbinary(14) NOT NULL,
  rc_cur_time varbinary(14) NOT NULL,
  rc_user int NOT NULL,
  rc_user_text varbinary(255) NOT NULL,
  rc_namespace int NOT NULL,
  rc_title varbinary(255) NOT NULL,
  rc_comment varbinary(255) NOT NULL,
  rc_minor tinyint NOT NULL,
  rc_bot tinyint NOT NULL,
  rc_new tinyint NOT NULL,
  rc_cur_id int NOT NULL,
  rc_this_oldid int NOT NULL,
  rc_last_oldid int NOT NULL,
  rc_type tinyint NOT NULL,
  rc_moved_to_ns tinyint NOT NULL,
  rc_moved_to_title varbinary(255) NOT NULL,
  rc_patrolled tinyint NOT NULL,
  rc_ip varbinary(40) NOT NULL,
  rc_old_len int DEFAULT NULL,
  rc_new_len int DEFAULT NULL,
  rc_deleted tinyint NOT NULL,
  rc_logid int NOT NULL,
  rc_log_type varbinary(255) DEFAULT NULL,
  rc_log_action varbinary(255) DEFAULT NULL,
  rc_params varbinary(1024),
  PRIMARY KEY (rc_id)
);

-- TODO: rev_id
DROP TABLE IF EXISTS revision;
CREATE TABLE revision (
  rev_id int NOT NULL,
  rev_page int NOT NULL,
  rev_text_id int NOT NULL,
  rev_comment varchar(1024) NOT NULL,
  rev_user int NOT NULL,
  rev_user_text varchar(255) NOT NULL,
  rev_timestamp binary(14) NOT NULL,
  rev_minor_edit tinyint NOT NULL,
  rev_deleted tinyint NOT NULL,
  rev_len int DEFAULT NULL,
  rev_parent_id int DEFAULT NULL,
  PRIMARY KEY (rev_id),
);

-- TODO old_id
DROP TABLE IF EXISTS text;
CREATE TABLE text (
  old_id int NOT NULL,
  old_text longvarchar NOT NULL,
  old_flags varchar(1024) NOT NULL,
  old_page int DEFAULT NULL,
  PRIMARY KEY (old_id)
);

DROP TABLE IF EXISTS user_groups;
CREATE TABLE user_groups (
  ug_user int NOT NULL REFERENCES useracct (user_id),
  ug_group varbinary(16) NOT NULL,
  UNIQUE (ug_user,ug_group)
);
CREATE INDEX IDX_UG_GROUP ON user_groups (ug_group);

DROP TABLE IF EXISTS value_backup;
CREATE TABLE value_backup (
  table_name varchar(255) DEFAULT NULL,
  maxid int DEFAULT NULL
);

DROP TABLE IF EXISTS watchlist;
CREATE TABLE watchlist (
  wl_user int NOT NULL,
  wl_namespace int NOT NULL,
  wl_title varchar(255) NOT NULL,
  wl_notificationtimestamp varchar(14) DEFAULT NULL,
);