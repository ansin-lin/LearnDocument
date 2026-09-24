USE paid_leave_training;

INSERT INTO departments (code, display_name, sort_order) VALUES
  ('development', 'システム開発部', 10),
  ('quality', '品質管理部', 20),
  ('sales', '営業部', 30),
  ('general-affairs', '総務部', 40),
  ('human-resources', '人事部', 50)
ON DUPLICATE KEY UPDATE
  display_name = VALUES(display_name),
  sort_order = VALUES(sort_order);

-- 用户通过注册 API 创建。初始化脚本不提供固定账号或固定密码。
