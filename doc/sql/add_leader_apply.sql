-- 新增开团状态字段
ALTER TABLE `leader`
ADD COLUMN `apply_status` TINYINT DEFAULT 0 COMMENT '开团状态: 0-待审核 1-已通过 2-已拒绝',
ADD COLUMN `apply_time` DATETIME COMMENT '申请时间';
