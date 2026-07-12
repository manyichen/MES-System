-- 将历史业务数据中已知的英文展示文案转换为中文。
-- 状态码、角色码、权限码和业务编号属于程序稳定编码，不做修改。

UPDATE mes_work_order_operation_log
SET operation_reason = CASE
    WHEN operation_reason = 'work order created' THEN '创建生产工单'
    WHEN operation_reason = 'work order dispatched' THEN '生产工单已派发'
    WHEN operation_reason = 'work order received' THEN '生产工单已接收'
    WHEN operation_reason LIKE 'work order dispatched to user %'
        THEN '生产工单已派发给用户 ' || substring(operation_reason from '[0-9]+$')
    ELSE operation_reason END
WHERE operation_reason LIKE 'work order %';

UPDATE mes_rework_order
SET rework_reason = '质检不合格，安排返工'
WHERE rework_reason = 'Quality rework due to inspection';
