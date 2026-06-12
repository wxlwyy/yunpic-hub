-- 创建库
create database if not exists yun_picture;

-- 切换库
use yun_picture;

# 用户表
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

# 图片表
create table picture
(
    id            bigint auto_increment comment '主键'
        primary key,
    url           varchar(512)                       not null comment '图片的url',
    name          varchar(128)                       not null comment '图片名称',
    introduction  varchar(512)                       null comment '图片简介',
    category      varchar(64)                        null comment '分类',
    tags          varchar(512)                       null comment '标签（json数组）',
    picSize       bigint                             null comment '图片体积',
    picWidth      int                                null comment '图片宽度',
    picHeight     int                                null comment '图片高度',
    picScale      double                             null comment '图片宽高比例',
    picFormat     varchar(64)                        null comment '图片格式',
    userId        bigint                             not null comment '创建图片的用户id',
    reviewerId    bigint                             null comment '审核人id',
    reviewTime    datetime                           null comment '审核时间',
    reviewStatus  int      default 0                 not null comment '审核状态：0-待审核；1-通过；2-拒绝',
    reviewMessage varchar(512)                       null comment '审核信息',
    createTime    datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime      datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime    datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete      tinyint  default 0                 not null comment '是否删除'
)
    comment '图片表' collate = utf8mb4_unicode_ci;

create index picture_category_index
    on picture (category);

create index picture_introduction_index
    on picture (introduction);

create index picture_name_index
    on picture (name);

create index picture_reviewStatus_index
    on picture (reviewStatus);

create index picture_tags_index
    on picture (tags);

create index picture_userId_index
    on picture (userId);

ALTER TABLE picture
-- 添加新列
    ADD COLUMN thumbnailUrl varchar(512) NULL COMMENT '缩略图 url';
-- 添加新列
ALTER TABLE picture
    ADD COLUMN spaceId  bigint  null comment '空间 id（为空表示公共空间）';

-- 创建索引
CREATE INDEX idx_spaceId ON picture (spaceId);

-- 新增 picColor 字段（存储16进制颜色值，如#FF0000，长度设10足够）
ALTER TABLE picture
    ADD COLUMN picColor VARCHAR(16) NULL COMMENT '图片主色调（16进制RGB值，如#FF0000）'


-- 空间表
create table if not exists space
(
    id         bigint auto_increment comment 'id' primary key,
    spaceName  varchar(128)                       null comment '空间名称',
    spaceLevel int      default 0                 null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize    bigint   default 0                 null comment '空间图片的最大总大小',
    maxCount   bigint   default 0                 null comment '空间图片的最大数量',
    totalSize  bigint   default 0                 null comment '当前空间下图片的总大小',
    totalCount bigint   default 0                 null comment '当前空间下的图片数量',
    userId     bigint                             not null comment '创建用户 id',
    createTime datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime   datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete   tinyint  default 0                 not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;

-- 空间成员表
CREATE TABLE IF NOT EXISTS space_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    space_id BIGINT NOT NULL COMMENT '空间ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    space_role VARCHAR(50) NOT NULL COMMENT '空间角色：viewer/editor/admin',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '成员加入时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改成员角色时间',
    INDEX idx_space_id(space_id),
    INDEX idx_user_id(user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='空间成员表';