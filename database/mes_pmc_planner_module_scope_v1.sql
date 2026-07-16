-- Remove non-planning module access from PMC planner.
-- PMC keeps planning, trace, feedback, master/process read access, but no longer opens
-- warehouse logistics, quality management, or equipment maintenance modules directly.
DELETE FROM mes_role_permission rp
USING mes_role r, mes_permission p
WHERE rp.role_id = r.role_id
  AND rp.permission_id = p.permission_id
  AND r.role_code = 'PMC_PLANNER'
  AND p.permission_code IN ('warehouse.read', 'quality.read', 'equipment.read');
