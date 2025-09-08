-- 创建库
create database if not exists yun_picture;

-- 切换库
use yun_picture;

create table if not exists user
(
    id           bigint auto_increment comment '用户id'
        primary key,
    userAccount  varchar(256) charset utf8mb4                           not null comment '账号',
    userPassword varchar(512) charset utf8mb4                           not null comment '密码',
    userName     varchar(256) charset utf8mb4                           null comment '用户昵称',
    userAvatar   varchar(1024) charset utf8mb4                          null comment '用户头像',
    userProfile  varchar(512) charset utf8mb4                           null comment '用户简介',
    userRole     varchar(256) charset utf8mb4 default 'user'            not null comment '用户角色：user/admin',
    editTime     datetime                     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime                     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime                     default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint                      default 0                 not null comment '是否删除',
    constraint user_userAccount_uindex
        unique (userAccount)
)
    comment '用户表' collate = utf8mb4_unicode_ci;

create index user_userName_index
    on user (userName);