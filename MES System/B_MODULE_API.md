# B Module API

This document describes developer B's implemented scope: warehouse logistics and production reporting.

## Data Source

All B module services use PostgreSQL through `com.example.messystem.common.Db`.

Required tables:

- `mes_work_order` (read status/planned quantity/batch, update actual quantity after report approval)
- `mes_material`
- `mes_inventory`
- `mes_warehouse`
- `mes_warehouse_location`
- `mes_inventory_transaction`
- `mes_material_requisition`
- `mes_material_requisition_item`
- `mes_picking_task`
- `mes_robot`
- `mes_robot_delivery_task`
- `mes_work_report`
- `mes_piecework_wage`

## Warehouse APIs

### Materials

- `GET /api/materials`
- `GET /api/materials/{id}`
- `POST /api/materials`

Example body:

```json
{
  "materialCode": "MAT-DEMO-001",
  "materialName": "天然橡胶",
  "materialType": "RAW",
  "specification": "RSS3",
  "unit": "kg"
}
```

### Warehouses And Locations

- `GET /api/warehouses`
- `GET /api/warehouses/{id}`
- `POST /api/warehouses`
- `GET /api/warehouses/locations`
- `GET /api/warehouses/locations/{id}`
- `POST /api/warehouses/locations`

Warehouse body:

```json
{
  "warehouseCode": "WH-DEMO-001",
  "warehouseName": "原材料仓",
  "warehouseType": "RAW"
}
```

Location body:

```json
{
  "warehouseId": 1,
  "locationCode": "LOC-DEMO-001",
  "locationName": "A-01"
}
```

### Inventory

- `GET /api/inventory`
- `GET /api/inventory/{id}`
- `POST /api/inventory`
- `GET /api/inventory/transactions`
- `GET /api/inventory/transactions/{id}`

Example body:

```json
{
  "materialId": 1,
  "warehouseId": 1,
  "locationId": 1,
  "batchNo": "BATCH-20260709-001",
  "availableQty": 500,
  "qualityStatus": "QUALIFIED"
}
```

### Requisitions

- `GET /api/requisitions`
- `GET /api/requisitions/{id}`
- `GET /api/requisitions/by-work-order/{workOrderId}`
- `POST /api/requisitions`
- `POST /api/requisitions/{id}/approve?approvedBy=1`

Example body:

```json
{
  "workOrderId": 1,
  "requestedBy": 1,
  "items": [
    {
      "materialId": 1,
      "requiredQty": 10,
      "unit": "kg",
      "batchNo": "BATCH-20260709-001"
    }
  ]
}
```

Approving a requisition first checks whether inventory is enough for every requisition item. If inventory is not enough, the approval fails and no picking task is created. If the check passes, approving changes the requisition from `CREATED` to `APPROVED` and creates a picking task. The detail endpoint returns the requisition header with `items`.

Creating a requisition validates `workOrderId` against the shared `mes_work_order` table. The work order must exist and its status must be `DISPATCHED`, `RECEIVED`, or `RUNNING`.

`GET /api/requisitions/by-work-order/{workOrderId}` only queries B-owned requisition tables, but the `workOrderId` itself is the shared key created by A in `mes_work_order`.

### Picking And Delivery

- `GET /api/picking-tasks`
- `GET /api/picking-tasks/{id}`
- `POST /api/picking-tasks/{id}/complete`
- `GET /api/robot-delivery-tasks`
- `GET /api/robot-delivery-tasks/{id}`
- `POST /api/robot-delivery-tasks/{id}/arrive`
- `POST /api/robot-delivery-tasks/{id}/confirm-receipt`

Completing a picking task creates a robot delivery task. Marking delivery as arrived only changes the delivery status to `ARRIVED`. Confirming receipt deducts inventory, writes inventory transactions, marks requisition items as completed, and changes the requisition status to `COMPLETED`.

### Robots

- `GET /api/robots`
- `GET /api/robots/{id}`
- `POST /api/robots`

Example body:

```json
{
  "robotCode": "ROB-DEMO-001",
  "robotName": "配送机器人一号",
  "robotStatus": "IDLE",
  "batteryLevel": 88,
  "currentLocation": "原材料仓"
}
```

## Production APIs

### Work Reports

- `GET /api/work-reports`
- `GET /api/work-reports/{id}`
- `GET /api/work-reports/by-work-order/{workOrderId}`
- `POST /api/work-reports`
- `POST /api/work-reports/{id}/approve`

Example body:

```json
{
  "workOrderId": 1,
  "batchNo": "BATCH-20260709-001",
  "operatorId": 1,
  "reportQty": 100,
  "qualifiedQty": 95,
  "defectQty": 5,
  "workHours": 8
}
```

Creating a work report validates the shared `mes_work_order` row. The work order must exist, its status must be `DISPATCHED`, `RECEIVED`, or `RUNNING`, and total reported quantity cannot exceed `planned_qty * 1.1`. If `batchNo` is omitted, B uses `mes_work_order.batch_no`.

Approving a submitted work report changes it to `APPROVED`, creates one piecework wage record, and updates `mes_work_order.actual_qty` by the approved qualified quantity. If actual quantity reaches planned quantity, the work order status is updated to `FINISHED`; otherwise it becomes `RUNNING`.

`GET /api/work-reports/by-work-order/{workOrderId}` returns B-owned work reports for the shared A work order. C can read approved reports from this table or from the API for quality inspection and traceability.

### Piecework Wages

- `GET /api/piecework-wages`
- `GET /api/piecework-wages/{id}`

Current piece rate is fixed in `ProductionDao` as `2.50`.

## Frontend

B module UI files:

- `src/main/webapp/index.html`
- `src/main/webapp/js/api.js`
- `src/main/webapp/js/warehouse.js`
- `src/main/webapp/js/production.js`
- `src/main/webapp/css/app.css`

Implemented UI capabilities:

- Basic material, warehouse, location, inventory, and robot lists.
- Detail buttons for basic data records.
- Inventory row "use" action to fill material ID and batch number into the requisition form.
- Warehouse list refresh.
- Demo material, warehouse, location, inventory, and robot creation.
- Requisition creation and approval.
- Picking completion.
- Delivery arrival and receipt confirmation.
- Inventory transaction list.
- Production report submission and approval.
- Piecework wage list.
- Detail buttons for requisitions, picking tasks, delivery tasks, inventory transactions, work reports, and wages.

## Verification

Run:

```powershell
cd "E:\Projects\xiaoxueqishixun\chenlun\MES-System\MES System"
.\mvnw.cmd test
```

The integration test writes `TEST-` prefixed data into the PostgreSQL database and cleans it before and after each test.
