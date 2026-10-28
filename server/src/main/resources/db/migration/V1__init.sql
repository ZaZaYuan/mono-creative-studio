CREATE TABLE templates (
  id VARCHAR(64) PRIMARY KEY,
  title VARCHAR(120) NOT NULL,
  category VARCHAR(60) NOT NULL,
  description VARCHAR(500) NOT NULL,
  cover_url VARCHAR(500) NOT NULL,
  required_image_count INT NOT NULL,
  accepted_mime_types VARCHAR(120) NOT NULL,
  max_file_size_bytes BIGINT NOT NULL
);
CREATE TABLE tasks (
  id CHAR(36) PRIMARY KEY,
  owner_id CHAR(36) NOT NULL,
  template_id VARCHAR(64) NOT NULL,
  status VARCHAR(20) NOT NULL,
  current_stage VARCHAR(30),
  failure_reason VARCHAR(500),
  result_file_id CHAR(36),
  fail_at_stage VARCHAR(30),
  created_at DATETIME(3) NOT NULL,
  updated_at DATETIME(3) NOT NULL,
  INDEX idx_tasks_owner_created (owner_id, created_at),
  CONSTRAINT fk_task_template FOREIGN KEY (template_id) REFERENCES templates(id)
);
CREATE TABLE files (
  id CHAR(36) PRIMARY KEY,
  task_id CHAR(36) NOT NULL,
  kind VARCHAR(20) NOT NULL,
  original_name VARCHAR(255) NOT NULL,
  mime_type VARCHAR(80) NOT NULL,
  storage_name VARCHAR(100) NOT NULL,
  created_at DATETIME(3) NOT NULL,
  INDEX idx_files_task (task_id),
  CONSTRAINT fk_file_task FOREIGN KEY (task_id) REFERENCES tasks(id)
);
INSERT INTO templates VALUES
('postcard','远方来信','旅行影像','把旅途瞬间制成一张有温度的明信片。','/covers/postcard.svg',2,'image/jpeg,image/png',5242880),
('pet-portrait','毛茸茸的主角','宠物故事','为宠物制作一幅充满个性的故事肖像。','/covers/pet.svg',1,'image/jpeg,image/png',5242880),
('memory','时光切片','纪念时刻','用两张照片定格值得珍藏的片刻。','/covers/memory.svg',2,'image/jpeg,image/png',5242880),
('travel','日光收藏家','旅行影像','把光影与风景收进一张旅行纪念画。','/covers/travel.svg',1,'image/jpeg,image/png',5242880),
('celebration','庆祝这一刻','纪念时刻','用三张照片记录相聚的心情。','/covers/celebration.svg',3,'image/jpeg,image/png',5242880),
('nature','自然手帖','创意图片','让自然纹理成为你的创作素材。','/covers/nature.svg',1,'image/jpeg,image/png',5242880);
