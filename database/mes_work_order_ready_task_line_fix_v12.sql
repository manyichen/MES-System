BEGIN;

-- Fix demo tasks that are already kitted and ready for work-order creation
-- but have no usable target production line. Without this, the backend cannot
-- derive line_id when creating a work order and rejects the request as required.
WITH fallback_line AS (
    SELECT line_id
    FROM mes_production_line
    WHERE COALESCE(enabled, 1) = 1
      AND COALESCE(line_status, 'IDLE') NOT IN ('FAULT', 'DISABLED')
    ORDER BY line_id
    LIMIT 1
)
UPDATE mes_production_task task
SET target_line_id = fallback_line.line_id,
    updated_at = CURRENT_TIMESTAMP,
    remark = CONCAT_WS(
        '；',
        NULLIF(task.remark, ''),
        '修复已齐套任务缺少可用目标产线，确保可制定工单'
    )
FROM fallback_line
WHERE task.task_status = 'READY'
  AND task.kitting_status = 'READY'
  AND (
      task.target_line_id IS NULL
      OR NOT EXISTS (
          SELECT 1
          FROM mes_production_line current_line
          WHERE current_line.line_id = task.target_line_id
            AND COALESCE(current_line.enabled, 0) = 1
            AND COALESCE(current_line.line_status, 'IDLE') NOT IN ('FAULT', 'DISABLED')
      )
  );

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM mes_production_task task
        LEFT JOIN mes_production_line line ON line.line_id = task.target_line_id
        WHERE task.task_status = 'READY'
          AND task.kitting_status = 'READY'
          AND (
              task.target_line_id IS NULL
              OR line.line_id IS NULL
              OR COALESCE(line.enabled, 0) <> 1
              OR COALESCE(line.line_status, 'IDLE') IN ('FAULT', 'DISABLED')
          )
    ) THEN
        RAISE EXCEPTION '仍存在 READY 且已齐套但没有可用目标产线的生产任务';
    END IF;
END $$;

INSERT INTO mes_schema_migration (version_code, description)
VALUES ('v12-work-order-ready-task-line-fix', '修复已齐套生产任务缺少可用目标产线导致无法制定工单')
ON CONFLICT (version_code) DO UPDATE SET
    description = EXCLUDED.description,
    applied_at = CURRENT_TIMESTAMP;

COMMIT;
