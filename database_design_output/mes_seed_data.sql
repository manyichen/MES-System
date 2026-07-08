USE mes_db;
INSERT INTO mes_user(username, real_name, role_code, department) VALUES
('pmc001','PMC计划员','PMC_PLANNER','计划部'),
('wh001','仓储人员','WAREHOUSE_KEEPER','仓储部'),
('op001','生产操作工','WORKSHOP_OPERATOR','生产车间'),
('qc001','质检员','QUALITY_INSPECTOR','质量部'),
('eq001','设备管理员','EQUIPMENT_ADMIN','设备部');

INSERT INTO mes_product(product_code, product_name, product_model, specification) VALUES
('TYRE-2055516','乘用轮胎205/55R16','205/55R16','半钢子午线轮胎'),
('TYRE-2256517','SUV轮胎225/65R17','225/65R17','半钢子午线轮胎');

INSERT INTO mes_production_line(line_code, line_name, line_type, daily_capacity) VALUES
('LINE-MIX-01','炼胶一线','MIXING',800),
('LINE-BLD-01','成型一线','BUILDING',600),
('LINE-CUR-01','硫化一线','CURING',500);