
create table im_user(
  `user_id`  bigint(20) unsigned NOT NULL,
  `username` varchar(200) DEFAULT ''  COMMENT '用户名',
  `nickname` varchar(200) DEFAULT ''  COMMENT '用户名',
  `password` varchar(100) DEFAULT ''  COMMENT '密码',
  `status` int(1) DEFAULT 0  COMMENT '状态:0-离线1-在线',
  `create_time` timestamp NULL DEFAULT NULL,
  `heart_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create table im_friend(
  `a_user_id` bigint(20) unsigned NOT NULL,
  `b_user_id` bigint(20) unsigned NOT NULL,
  `a_status` int(1) DEFAULT 0  COMMENT '状态:0-未同意1-同意',
  `b_status` int(1) DEFAULT 0  COMMENT '状态:0-未同意1-同意',
  `create_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`a_user_id`,`b_user_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create table im_group(
  `group_id` bigint(20) unsigned  NOT NULL,
  `group_name` varchar(50) DEFAULT ''  COMMENT '群组名',
  `token` varchar(50) DEFAULT ''  COMMENT 'token 一个token只能创建一个群组',
  `creater` bigint(20) unsigned  NOT NULL,
  `create_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`group_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;


create table im_group_user(
  `user_id` bigint(20) unsigned  NOT NULL,
  `group_id` bigint(20) unsigned ,
  `create_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`user_id`,`group_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create table im_message(
  `msg_id` bigint(20) unsigned NOT NULL,
  `type` int(2) unsigned NOT NULL COMMENT '类型:1-好友消息2-系统推送消息' ,
  `msg_type` int(2) unsigned DEFAULT 1 COMMENT '消息类型:1-文本2-图片3-视频' ,
  `target` varchar(100) NOT NULL,
  `sender` varchar(100) NULL,
  `status` int(0) DEFAULT 0 COMMENT '状态:0-未读1-已读',
  `content` varchar(2000) DEFAULT ''  COMMENT '内容',
  `create_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`msg_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create table im_group_message(
  `msg_id` bigint(20) unsigned NOT NULL,
  `group_id` bigint(20) unsigned NOT NULL,
  `sender` bigint(20) unsigned NOT NULL,
  `status` int(0) DEFAULT 0 COMMENT '状态:0-未读1-已读',
  `content` varchar(2000) DEFAULT ''  COMMENT '内容',
  `create_time` timestamp NULL DEFAULT NULL,
  PRIMARY KEY (`msg_id`)
)ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

create unique index idx_username on im_user(username) ;
create unique index idx_group on im_group(token) ;


