-- 齐套分析数据一致性修复：清理已失去主数据引用的BOM，并阻止再次产生孤儿引用。
-- 执行前应备份 mes_product_bom、mes_product、mes_material、mes_customer_order、mes_production_task。

BEGIN;

LOCK TABLE mes_product_bom IN SHARE ROW EXCLUSIVE MODE;

DELETE FROM mes_product_bom bom
WHERE NOT EXISTS (
          SELECT 1 FROM mes_product product_row
          WHERE product_row.product_id = bom.product_id
      )
   OR NOT EXISTS (
          SELECT 1 FROM mes_material material
          WHERE material.material_id = bom.material_id
      );

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'mes_product_bom'::regclass
          AND conname = 'fk_mes_product_bom_product'
    ) THEN
        ALTER TABLE mes_product_bom
            ADD CONSTRAINT fk_mes_product_bom_product
            FOREIGN KEY (product_id) REFERENCES mes_product(product_id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'mes_product_bom'::regclass
          AND conname = 'fk_mes_product_bom_material'
    ) THEN
        ALTER TABLE mes_product_bom
            ADD CONSTRAINT fk_mes_product_bom_material
            FOREIGN KEY (material_id) REFERENCES mes_material(material_id) ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'mes_product_bom'::regclass
          AND conname = 'ck_mes_product_bom_usage_positive'
    ) THEN
        ALTER TABLE mes_product_bom
            ADD CONSTRAINT ck_mes_product_bom_usage_positive CHECK (usage_qty > 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'mes_product_bom'::regclass
          AND conname = 'ck_mes_product_bom_enabled'
    ) THEN
        ALTER TABLE mes_product_bom
            ADD CONSTRAINT ck_mes_product_bom_enabled CHECK (enabled IN (0, 1));
    END IF;
END $$;

COMMIT;
