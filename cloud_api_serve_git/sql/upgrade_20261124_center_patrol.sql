-- 人工审核并备份后执行；本文件不由应用自动执行。
-- 前置：upgrade_20261105_center_ordinary_task.sql、upgrade_20261106_center_ordinary_status.sql。
-- 切换前必须停止所有旧版本实例，备份MySQL两张旧任务表及Redis task_schedule:zset、task_schedule:hash:*。
-- 导出旧waiting/executing安排并逐项对照实际job/EUA任务：已飞、执行中和接收不确定的记录禁止重新下发。
-- 核验后将清单交上级确认：仅未执行且仍在未来的安排可按101/isenable=0重新下发；过期安排不自动补飞。
-- 未完成清单核验不可恢复调度；Redis原始数据保留用于审计，不清空，不由新扫描器消费。
-- EUA使用受保护PATROL独立计划和真实EUA执行记录，不生成本地job；当前沿用单工作空间部署，归属不唯一时明确拒绝。
-- EUA准备记录及关联提交后即使尚未调用下级就崩溃，也按DISPATCHING超时转UNKNOWN隔离：必须人工核验，不自动重试或重新发布。
-- MySQL保存上级安排，但EUA状态轮询仍依赖原Redis monitor；重启不从MySQL重建monitor，须保留Redis数据，本次不扩展恢复功能。
-- 执行表原唯一键仍为(sub_code,task_code,scheduled_time)：跨中心相同标识会被拒绝，尚不支持并存。
CREATE TABLE df_center_patrol_plan (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 center_code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 sub_code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 task_code VARCHAR(128) COLLATE utf8mb4_bin NOT NULL,
 task_name VARCHAR(512) NOT NULL,
 workspace_id VARCHAR(128) NULL,
 isenable INT NOT NULL DEFAULT 1 COMMENT '0启用 1停用 2软删除',
 command_json LONGTEXT NULL,
 execution_id BIGINT NULL,
 UNIQUE KEY uk_center_patrol_identity(center_code,sub_code,task_code),
 KEY idx_center_patrol_workspace(workspace_id,isenable)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
ALTER TABLE df_center_ordinary_task
 ADD COLUMN upper_plan_id BIGINT NULL,
 ADD COLUMN plan_type INT NULL,
 ADD COLUMN workspace_id VARCHAR(128) NULL,
 ADD COLUMN eua_record_id BIGINT NULL,
 ADD COLUMN eua_task_id VARCHAR(128) NULL,
 ADD KEY idx_upper_plan(upper_plan_id,id);
-- 旧Redis安排不自动迁移/补飞。停机核验后由上级重新下发；旧未领取普通安排隔离。
UPDATE df_center_ordinary_task SET status='UNKNOWN',message='升级前未关联上级计划，禁止自动补飞，请人工核验后重新下发'
 WHERE upper_plan_id IS NULL AND status='SCHEDULED';
