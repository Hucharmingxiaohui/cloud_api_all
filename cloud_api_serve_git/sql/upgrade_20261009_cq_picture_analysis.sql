-- 重庆EUA图片先分析后上报需求：df_cq_dock_task_picture 增加图片类型与分析结果字段
-- 2026-10-09
ALTER TABLE df_cq_dock_task_picture
    ADD COLUMN point_type INT DEFAULT NULL COMMENT '图片类型 0可见光 1红外（EUA图片列表pointType）',
    ADD COLUMN request_id VARCHAR(64) DEFAULT NULL COMMENT '发送给智能分析服务的请求ID（可见光回调关联）',
    ADD COLUMN analysis_status INT DEFAULT 0 COMMENT '分析状态 0待分析 1已分析 2分析失败 3红外测温完成',
    ADD COLUMN analysis_image_url VARCHAR(512) DEFAULT NULL COMMENT '分析结果图地址（可见光回调resImageUrl）',
    ADD COLUMN point_val VARCHAR(1024) DEFAULT NULL COMMENT '分析/测温结论值',
    ADD COLUMN point_val_unit VARCHAR(256) DEFAULT NULL COMMENT '结论单位或失败描述';
