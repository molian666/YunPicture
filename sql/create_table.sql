
create database if not exists yun_picture;
-- 切换库
use yun_picture;
-- 用户表
create table if not exists user
(
    id           bigint auto_increment comment 'id' primary key,
    userAccount  varchar(256)                           not null comment '账号',
    userPassword varchar(512)                           not null comment '密码',
    userName     varchar(256)                           not null comment '用户昵称',
    userAvatar   varchar(1024)                          not null comment '用户头像',
    userProfile  varchar(512)                           not null comment '用户简介',
    userRole     varchar(256) default 'user'            not null comment '用户角色user/admin',
    editTime     datetime     default CURRENT_TIMESTAMP not null comment '编辑时间',
    createTime   datetime     default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime   datetime     default CURRENT_TIMESTAMP not null comment '更新时间',
    isDelete     tinyint      default 0                 not null comment '是否删除',
    UNIQUE KEY uk_userAccount (userAccount),
    INDEX idx_userName (userName)
) comment '用户' collate = utf8mb4_unicode_ci;

-- 图片表
create table if not exists picture
(
    id           bigint auto_increment comment 'id' primary key,
    url          varchar(512)                       not null comment '图片 url',
    name         varchar(128)                       not null comment '图片名称',
    introduction varchar(512)                       null comment '简介',
    category     varchar(64)                        null comment '分类',
    tags         varchar(512)                      null comment '标签（JSON 数组）',
    picSize      bigint                             null comment '图片体积',
    picWidth     int                                null comment '图片宽度',
    picHeight    int                                null comment '图片高度',
    picScale     double                             null comment '图片宽高比例',
    picFormat    varchar(32)                        null comment '图片格式',
    userId       bigint                             not null comment '创建用户 id',
    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime     datetime default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime   datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete     tinyint  default 0                 not null comment '是否删除',
    INDEX idx_name (name),
    INDEX idx_introduction (introduction),
    INDEX idx_category (category),
    INDEX idx_tags (tags),
    INDEX idx_userId (userId)
) comment '图片' collate = utf8mb4_unicode_ci;


ALTER TABLE picture

    ADD COLUMN reviewStatus INT DEFAULT 0 NOT NULL COMMENT '审核状态：0-待审核; 1-通过; 2-拒绝',
    ADD COLUMN reviewMessage VARCHAR(512) NULL COMMENT '审核信息',
    ADD COLUMN reviewerId BIGINT NULL COMMENT '审核人 ID',
    ADD COLUMN reviewTime DATETIME NULL COMMENT '审核时间';


CREATE INDEX idx_reviewStatus ON picture (reviewStatus);


ALTER TABLE picture
    ADD COLUMN thumbnailUrl VARCHAR(512) NULL COMMENT '缩略图 URL';


create table if not exists space
(
    id          bigint auto_increment comment 'id' primary key,
    spaceName   varchar(128)                null comment '空间名称',
    spaceLevel  int         default 0       null comment '空间级别：0-普通版 1-专业版 2-旗舰版',
    maxSize     bigint      default 0       null comment '空间图片的最大总大小',
    maxCount    bigint      default 0       null comment '空间图片的最大数量',
    totalSize   bigint      default 0       null comment '当前空间下图片的总大小',
    totalCount  bigint      default 0       null comment '当前空间下的图片数量',
    userId      bigint                      not null comment '创建用户id',
    createTime  datetime    default CURRENT_TIMESTAMP not null comment '创建时间',
    editTime    datetime    default CURRENT_TIMESTAMP not null comment '编辑时间',
    updateTime  datetime    default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    isDelete    tinyint     default 0       not null comment '是否删除',
    -- 索引设计
    index idx_userId (userId),        -- 提升基于用户的查询效率
    index idx_spaceName (spaceName),  -- 提升基于空间名称的查询效率
    index idx_spaceLevel (spaceLevel) -- 提升按空间级别查询的效率
) comment '空间' collate = utf8mb4_unicode_ci;


ALTER TABLE picture
    ADD COLUMN spaceId bigint NULL COMMENT '空间id（为空表示公共空间）';

CREATE INDEX idx_spaceId ON picture (spaceId);

ALTER TABLE picture
    ADD COLUMN picColor varchar(16) NULL COMMENT '颜色主色调';

-- 支持空间类型添加新列
ALTER TABLE space
    ADD COLUMN spaceType int default 0 not NULL COMMENT '空间类型：0-私有 1-团队';

create index idx_spaceType on space (spaceType);


-- 空间成员表
create table if not exists space_user
(
    id          bigint auto_increment comment 'id' primary key,
    spaceId     bigint not null comment '空间 id',
    userId      bigint not null comment '用户 id',
    spaceRole   varchar(128) default 'viewer' null comment '空间角色: viewer/editor/admin',
    createTime  datetime default CURRENT_TIMESTAMP not null comment '创建时间',
    updateTime  datetime default CURRENT_TIMESTAMP not null on update CURRENT_TIMESTAMP comment '更新时间',
    -- 索引设计
    UNIQUE KEY uk_spaceId_userId (spaceId, userId), -- 唯一索引，用户在一个空间中只能有一个角色
    INDEX idx_spaceId (spaceId),                    -- 提升按空间查询的性能
    INDEX idx_userId (userId)                       -- 提升按用户查询的性能
) comment '空间用户关联' collate = utf8mb4_unicode_ci;

-- 创建默认分表，用于处理未分配空间的图片
create table if not exists picture_0 like picture;

-- 创建一些常用的分片表，避免在运行时出现表不存在的错误
create table if not exists picture_1 like picture;
create table if not exists picture_2 like picture;
create table if not exists picture_3 like picture;
create table if not exists picture_4 like picture;
create table if not exists picture_5 like picture;
create table if not exists picture_6 like picture;
create table if not exists picture_7 like picture;
create table if not exists picture_8 like picture;
create table if not exists picture_9 like picture;
create table if not exists picture_10 like picture;

USE yun_picture; DESC picture_0;
USE yun_picture; CREATE TABLE IF NOT EXISTS picture_1 LIKE picture; CREATE TABLE IF NOT EXISTS picture_2 LIKE picture; CREATE TABLE IF NOT EXISTS picture_3 LIKE picture;
USE yun_picture; SHOW TABLES LIKE 'picture%';
