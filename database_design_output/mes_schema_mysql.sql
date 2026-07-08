CREATE DATABASE IF NOT EXISTS mes_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE mes_db;

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '订单编号',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '客户名称',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '产品主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '产品编码快照',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(80) NOT NULL COMMENT '轮胎型号/规格',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '订单数量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NOT NULL DEFAULT '条' COMMENT '计量单位',
  $(System.Collections.Specialized.OrderedDictionary.name) DATE NOT NULL COMMENT '交付日期',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 3 COMMENT '优先级，1最高',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'PENDING_PLAN' COMMENT '订单状态',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL DEFAULT 'ERP' COMMENT '来源系统',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '备注',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_customer_order_order_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_customer_order_order_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户订单表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '任务主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '任务编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '来源订单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT 'PMC计划员',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '计划数量',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL COMMENT '计划开始时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL COMMENT '计划完成时间',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '目标产线',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'DRAFT' COMMENT '任务状态',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'NOT_ANALYZED' COMMENT '齐套状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '发布时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '闭环时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '备注',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_production_task_task_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_production_task_task_status ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_production_task_kitting_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产任务表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '分析主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '分析编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '生产任务',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '分析范围',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '分析结果',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL COMMENT '数据快照时间',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 0 COMMENT '物料是否齐套',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 0 COMMENT '产线是否可用',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 0 COMMENT '设备是否可用',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 0 COMMENT '工艺是否完整',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '分析人',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_kitting_analysis_analysis_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_kitting_analysis_result_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='齐套分析表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '缺口明细主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '齐套分析',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '生产任务',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '缺口类型 MATERIAL/LINE/EQUIPMENT/PROCESS',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '资源主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(60) NULL COMMENT '资源编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '资源名称',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NULL DEFAULT 0 COMMENT '需求数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NULL DEFAULT 0 COMMENT '可用数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NULL DEFAULT 0 COMMENT '缺口数量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '影响说明',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '处理建议',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  KEY idx_mes_kitting_shortage_item_shortage_type ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='齐套缺口明细表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '预警主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '预警编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '生产任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '齐套分析',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '预警类型',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NOT NULL DEFAULT 'MEDIUM' COMMENT '严重级别',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NULL COMMENT '接收角色',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(1000) NOT NULL COMMENT '预警内容',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '解决时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_shortage_alert_alert_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_shortage_alert_alert_type ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_shortage_alert_alert_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='欠料预警表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '工单编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '生产任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '产线',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '工序',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '计划数量',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '实际数量',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 3 COMMENT '优先级',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '工单状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '派发时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '接收时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '完成时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_work_order_work_order_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_work_order_work_order_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产工单表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '日志主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '工单',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '操作类型',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '操作前状态',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '操作后状态',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '操作人',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '操作原因',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 0 COMMENT '是否可撤销',
  PRIMARY KEY ($pk),
  KEY idx_mes_work_order_operation_log_operation_type ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单调度操作日志表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '领料任务主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '领料单号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '生产工单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '申请人',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '领料状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '审批人',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '审批时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '备注',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_material_requisition_requisition_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_material_requisition_request_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领料任务表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '明细主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '领料任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '物料',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '需求数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '已发数量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NOT NULL COMMENT '单位',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NULL COMMENT '指定批次',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '明细状态',
  PRIMARY KEY ($pk),
  KEY idx_mes_material_requisition_item_item_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='领料任务明细表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '物料主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '物料编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '物料名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '物料类型',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '规格',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NOT NULL COMMENT '单位',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NULL COMMENT '保质期天数',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_material_material_code ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_material_material_type ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='物料主数据表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '库存主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '物料',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '仓库',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '库位',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NOT NULL COMMENT '批次号',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '可用数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '占用数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '冻结数量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'QUALIFIED' COMMENT '质量状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '最近核对时间',
  PRIMARY KEY ($pk),
  KEY idx_mes_inventory_batch_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_inventory_quality_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '仓库主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '仓库编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '仓库名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '仓库类型',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_warehouse_warehouse_code ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仓库表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '库位主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '仓库',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '库位编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '库位名称',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_warehouse_location_location_code ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库位表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '流水主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '流水编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '物料',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '库存',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '变动类型',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '变动数量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '来源单据类型',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '来源单据ID',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '操作人',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_inventory_transaction_transaction_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_inventory_transaction_transaction_type ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='库存流水表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '拣货任务主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '拣货任务号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '领料任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '仓库',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '任务状态',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '拣货人',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '开始时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '完成时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_picking_task_picking_task_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_picking_task_task_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='备货拣货任务表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '机器人主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '机器人编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '机器人名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'IDLE' COMMENT '机器人状态',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(5,2) NULL COMMENT '电量百分比',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '当前位置',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_robot_robot_code ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_robot_robot_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运输机器人表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '配送任务主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '配送任务号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '拣货任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '机器人',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '起点库位',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '目标产线',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '配送状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '装载时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '交接时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_robot_delivery_task_delivery_task_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_robot_delivery_task_delivery_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='机器人配送任务表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '报工主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '报工编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '工单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '操作工',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '报工数量',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '合格数量',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '不合格数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(10,2) NULL DEFAULT 0 COMMENT '工时',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报工时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'SUBMITTED' COMMENT '报工状态',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_work_report_report_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_work_report_report_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产报工表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '计件工资主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '报工',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '操作工',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(10,4) NOT NULL DEFAULT 0 COMMENT '单件费率',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '合格数量',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(12,2) NOT NULL DEFAULT 0 COMMENT '工资金额',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'PENDING' COMMENT '结算状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  KEY idx_mes_piecework_wage_settlement_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='计件工资表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '抽检单主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '抽检单号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '工单',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL DEFAULT 0 COMMENT '抽检数量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '抽检状态',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '质检员',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '检验时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '判定结果',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_quality_inspection_inspection_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_quality_inspection_inspection_status ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_quality_inspection_judgement_result ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量抽检单表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '检验项目主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '抽检单',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '项目编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '项目名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '标准值',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '实测值',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '项目判定',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NULL COMMENT '备注',
  PRIMARY KEY ($pk),
  KEY idx_mes_quality_inspection_item_item_result ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量检验项目表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '返工单主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '返工单号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '来源工单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '来源质检单',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(500) NOT NULL COMMENT '返工原因',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '返工状态',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '返工产线',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '关闭时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_rework_order_rework_order_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_rework_order_rework_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='返工单表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '追溯主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '追溯编号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '订单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '工单',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NULL COMMENT '批次号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '质检单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '返工单',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'NORMAL' COMMENT '追溯状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_quality_trace_trace_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_quality_trace_batch_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_quality_trace_trace_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='质量追溯表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '设备主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '设备编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '设备名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '设备类型',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '所属产线',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'IDLE' COMMENT '设备状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '最近维护时间',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_equipment_equipment_code ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_equipment_equipment_type ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_equipment_equipment_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '报修主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '报修单号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '设备',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '关联工单',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NOT NULL DEFAULT 'GENERAL' COMMENT '故障等级',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(1000) NOT NULL COMMENT '故障描述',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '报修人',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '报修时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'REPORTED' COMMENT '报修状态',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_equipment_repair_report_repair_report_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_equipment_repair_report_repair_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备故障报修表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '维修工单主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '维修工单号',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '报修单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '设备',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '维修人',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'CREATED' COMMENT '维修状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '派发时间',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NULL COMMENT '完成时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(1000) NULL COMMENT '维修结果',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_maintenance_order_maintenance_order_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_maintenance_order_maintenance_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='维修工单表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '维护计划主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '设备',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '维护周期',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL COMMENT '下次计划时间',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'ACTIVE' COMMENT '计划状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  KEY idx_mes_maintenance_plan_next_plan_time ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_maintenance_plan_plan_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='设备维护计划表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '产品编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '产品名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(80) NOT NULL COMMENT '产品型号',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '规格',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_product_product_code ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_product_product_model ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品主数据表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT 'BOM主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '产品',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '物料',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '单位用量',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NOT NULL COMMENT '单位',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '使用工序',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品BOM表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '工序主键',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '产品',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '工序编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '工序名称',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NOT NULL COMMENT '工序顺序',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(10,2) NULL COMMENT '标准工时',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '所需设备类型',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  KEY idx_mes_process_route_process_seq ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工艺路线表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '产线主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '产线编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '产线名称',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '产线类型',
  $(System.Collections.Specialized.OrderedDictionary.name) INT NULL COMMENT '日产能',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'IDLE' COMMENT '产线状态',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_production_line_line_code ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_production_line_line_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='生产产线表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '同步日志主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '来源系统',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NOT NULL COMMENT '同步对象',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '业务键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '同步状态',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(1000) NULL COMMENT '错误信息',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '同步时间',
  PRIMARY KEY ($pk),
  KEY idx_mes_sync_log_source_system ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_sync_log_sync_object ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_sync_log_sync_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据同步日志表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '产品追溯主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(60) NOT NULL COMMENT '追溯码',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '订单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '任务',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '工单',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '产品',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NOT NULL COMMENT '生产批次',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'NORMAL' COMMENT '追溯状态',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_product_trace_trace_code ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_product_trace_batch_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_product_trace_trace_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品追溯表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '指标主键',
  $(System.Collections.Specialized.OrderedDictionary.name) DATE NOT NULL COMMENT '指标日期',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NOT NULL COMMENT '指标类型',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '指标名称',
  $(System.Collections.Specialized.OrderedDictionary.name) decimal(18,4) NOT NULL DEFAULT 0 COMMENT '指标值',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(20) NULL COMMENT '单位',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '维度键',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  PRIMARY KEY ($pk),
  KEY idx_mes_dashboard_metric_metric_date ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_dashboard_metric_metric_type ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='综合生产看板指标表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '反馈主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(40) NOT NULL COMMENT '反馈编号',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL COMMENT '反馈类型',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '关联单据类型',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NULL COMMENT '关联单据ID',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(1000) NOT NULL COMMENT '反馈内容',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(1000) NULL COMMENT '决策动作',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NOT NULL DEFAULT 'OPEN' COMMENT '处理状态',
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL COMMENT '创建人',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_management_feedback_feedback_no ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_management_feedback_feedback_type ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_management_feedback_feedback_status ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理决策反馈表';

DROP TABLE IF EXISTS $(System.Collections.Specialized.OrderedDictionary.name);
CREATE TABLE $(System.Collections.Specialized.OrderedDictionary.name) (
  $(System.Collections.Specialized.OrderedDictionary.name) BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户主键',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NOT NULL COMMENT '登录名',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NOT NULL COMMENT '姓名',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(50) NOT NULL COMMENT '角色编码',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(100) NULL COMMENT '部门',
  $(System.Collections.Specialized.OrderedDictionary.name) varchar(30) NULL COMMENT '电话',
  $(System.Collections.Specialized.OrderedDictionary.name) TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用',
  $(System.Collections.Specialized.OrderedDictionary.name) DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY ($pk),
  UNIQUE KEY uk_mes_user_username ($(System.Collections.Specialized.OrderedDictionary.name)),
  KEY idx_mes_user_role_code ($(System.Collections.Specialized.OrderedDictionary.name))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
