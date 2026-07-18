<script setup>
/**
 * 通用动态动作对话框。
 * 输入是 modules.js 中的 action/fields；输出是经过类型转换的 JSON 请求体，由 ModuleWorkspace 执行接口。
 * 支持普通输入、枚举选择、远程单/多选、依赖联动和领料明细行，是配置驱动 CRUD 的表单引擎。
 */
import { reactive, ref, watch } from 'vue'
import { Plus, Trash2, X } from 'lucide-vue-next'
import { api } from '../api/http'
import { businessValue, localizeMessage, localizeText } from '../utils/display.js'

const props = defineProps({ action: Object, row: Object, busy: Boolean })
const emit = defineEmits(['close', 'submit'])
// values 保存待提交值；optionRows/optionLoading 保存每个远程字段的候选项和加载状态。
const values = reactive({})
const optionRows = reactive({})
const optionLoading = reactive({})
const optionsError = ref('')
let loadVersion = 0

/** 将数组、items、records 和单对象统一成候选项数组。 */
function normalize(data) {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.items)) return data.items
  if (Array.isArray(data?.records)) return data.records
  return data ? [data] : []
}

/** 按 a.b.c 路径读取嵌套响应，供系统维护等复合接口复用。 */
function dataAtPath(data, path) {
  return path ? path.split('.').reduce((value, key) => value?.[key], data) : data
}

/** 深拷贝表单默认值，防止编辑时直接修改 action 配置或当前表格行。 */
function clone(value) {
  return value == null ? value : JSON.parse(JSON.stringify(value))
}

/** 统一判断必填值；0 和 false 是合法值，空数组不是。 */
function isBlankValue(value) {
  return value === '' || value === null || value === undefined
    || (typeof value === 'number' && Number.isNaN(value))
    || (Array.isArray(value) && value.length === 0)
}

/** 把 number/decimal 控件值转换为有限 Number，非法输入返回 null 交给校验处理。 */
function numericValue(value) {
  if (isBlankValue(value)) return null
  const next = Number(value)
  return Number.isFinite(next) ? next : null
}

/**
 * 每次打开/切换动作时重建表单，填入默认值或当前行值，并加载远程选项。
 * loadVersion 防止用户快速切换对话框时旧请求覆盖新动作的数据。
 */
async function initialize(action) {
  const version = ++loadVersion
  for (const key of Object.keys(values)) delete values[key]
  for (const key of Object.keys(optionRows)) delete optionRows[key]
  for (const key of Object.keys(optionLoading)) delete optionLoading[key]
  optionsError.value = ''
  for (const field of action?.fields || []) {
    const initial = action?.defaults?.[field.key] ?? props.row?.[field.key]
    if (field.type === 'line-items') {
      values[field.key] = clone(initial ?? field.example ?? [{ materialId: '', requiredQty: '', unit: '', batchNo: '' }])
    } else if (field.type === 'multi-lookup') {
      values[field.key] = clone(initial ?? [])
    } else {
      values[field.key] = initial ?? (field.type === 'json' ? JSON.stringify(field.example ?? [], null, 2) : '')
    }
  }

  const fields = (action?.fields || []).filter(field => field.source)
  const requests = new Map()
  await Promise.all(fields.map(async field => {
    optionLoading[field.key] = true
    try {
      let request = requests.get(field.source.endpoint)
      if (!request) {
        request = api.get(field.source.endpoint)
        requests.set(field.source.endpoint, request)
      }
      const payload = await request
      if (version !== loadVersion) return
      optionRows[field.key] = normalize(dataAtPath(payload, field.source.dataPath))
    } catch (cause) {
      if (version !== loadVersion) return
      optionRows[field.key] = []
      optionsError.value = optionsError.value || `可选内容加载未完成：${localizeMessage(cause.message)}`
    } finally {
      if (version === loadVersion) optionLoading[field.key] = false
    }
  }))
  if (version === loadVersion) reconcileDependentValues()
}

watch(() => props.action, initialize, { immediate: true })

/** 按 dependsOn、filter 等配置得到当前字段真正可选的远程记录。 */
function availableOptions(field) {
  const rows = optionRows[field.key] || []
  return field.source?.filter
    ? rows.filter(row => field.source.filter(row, values, optionRows))
    : rows
}

function optionValue(field, row) {
  return row?.[field.source.valueKey]
}

function optionLabel(field, row) {
  const label = typeof field.source.optionLabel === 'function'
    ? field.source.optionLabel(row)
    : row?.[field.source.optionLabel || field.source.valueKey] ?? optionValue(field, row)
  return localizeText(label)
}

function selectedOption(field, value = values[field.key]) {
  return availableOptions(field).find(row => String(optionValue(field, row)) === String(value))
}

function selectOptionValue(option) {
  return option && typeof option === 'object'
    ? option.value ?? option.key ?? option.code ?? option.label
    : option
}

function selectOptionLabel(field, option) {
  const label = option && typeof option === 'object'
    ? option.label ?? option.name ?? option.value ?? option.code
    : option
  return businessValue(field.key, label)
}

function selectedSelectOption(field) {
  return (field.options || []).find(option => String(selectOptionValue(option)) === String(values[field.key]))
}

/** 远程选择变化后执行 assign 映射，并重新协调依赖字段。 */
function onLookupChange(field) {
  const selected = selectedOption(field)
  const assignedKeys = []
  for (const [targetKey, sourceKey] of Object.entries(field.source.assign || {})) {
    values[targetKey] = clone(selected?.[sourceKey] ?? '')
    assignedKeys.push(targetKey)
  }
  reconcileDependentValues(field.key)
  for (const targetKey of assignedKeys) reconcileDependentValues(targetKey)
}

function onSelectChange(field) {
  const selected = selectedSelectOption(field)
  for (const [targetKey, sourceKey] of Object.entries(field.assign || {})) {
    values[targetKey] = selected?.[sourceKey] ?? ''
  }
}

/** 上游字段变化时清除已经不在候选范围内的下游值，防止提交失效关联。 */
function reconcileDependentValues(changedKey = null) {
  for (const field of props.action?.fields || []) {
    if (!field.source?.dependsOn || optionLoading[field.key]) continue
    const dependencies = Array.isArray(field.source.dependsOn) ? field.source.dependsOn : [field.source.dependsOn]
    if (changedKey && !dependencies.includes(changedKey)) continue
    if (values[field.key] !== '' && values[field.key] != null && !selectedOption(field)) {
      values[field.key] = field.type === 'multi-lookup' ? [] : ''
      onLookupChange(field)
    }
  }
}

/** 为领料等主从表单增加一条空明细。 */
function addLine(field) {
  values[field.key].push({ materialId: '', requiredQty: '', unit: '', batchNo: '' })
}

function removeLine(field, index) {
  values[field.key].splice(index, 1)
  if (!values[field.key].length) addLine(field)
}

/** 选中物料后自动回填单位，减少数量单位录入错误。 */
function onLineMaterialChange(field, line) {
  const material = availableOptions(field).find(row => String(optionValue(field, row)) === String(line.materialId))
  line.unit = material?.unit || ''
}

/**
 * 前端提交前校验必填项和明细行，并完成 number、decimal、JSON、多选等类型转换。
 * 该校验用于即时反馈；后端 Service 必须再次校验，因为客户端数据可以被绕过或伪造。
 */
function submit() {
  const output = {}
  for (const field of props.action?.fields || []) {
    let value = values[field.key]
    if (field.type === 'hidden' && (value === '' || value == null) && !field.required) continue
    if (field.type === 'number' || field.type === 'decimal') {
      value = numericValue(value)
    } else if (field.type === 'lookup') {
      value = isBlankValue(value) ? null : (field.valueType === 'string' ? String(value) : numericValue(value))
    } else if (field.type === 'multi-lookup') {
      value = (value || [])
        .map(item => isBlankValue(item) ? null : (field.valueType === 'string' ? String(item) : numericValue(item)))
        .filter(item => !isBlankValue(item))
    } else if (field.type === 'line-items') {
      const invalidIndex = (value || []).findIndex(line => !line.materialId || !line.requiredQty || Number(line.requiredQty) <= 0)
      if (invalidIndex >= 0) {
        return window.alert(`第 ${invalidIndex + 1} 条领料明细必须选择物料并填写大于 0 的数量`)
      }
      value = (value || []).map((line, index) => {
        return { ...line, materialId: Number(line.materialId), requiredQty: Number(line.requiredQty) }
      })
    } else if (field.type === 'json') {
      try { value = JSON.parse(value || 'null') } catch { return window.alert(`${field.label}必须是有效 JSON`) }
    }
    if (field.required && isBlankValue(value)) {
      return window.alert(`请填写${field.label}`)
    }
    output[field.key] = value
  }
  emit('submit', output)
}
</script>

<template>
  <!-- 字段 type 决定渲染控件；所有控件最终都写入同一个 values 对象。 -->
  <div class="dialog-mask" @mousedown.self="emit('close')">
    <section class="dialog" role="dialog" aria-modal="true">
      <header>
        <div><span>业务操作</span><h2>{{ action.label }}</h2></div>
        <button type="button" class="icon-button" title="关闭" @click="emit('close')"><X :size="20" /></button>
      </header>
      <form @submit.prevent="submit">
        <template v-for="field in action.fields || []" :key="field.key">
          <input v-if="field.type === 'hidden'" v-model="values[field.key]" type="hidden" />
          <div v-else-if="field.type === 'line-items'" class="dialog-field dialog-wide">
            <div class="dialog-field-head"><span>{{ field.label }}</span><button type="button" @click="addLine(field)"><Plus :size="15" />添加物料</button></div>
            <div class="line-item-head"><span>物料</span><span>数量</span><span>单位</span><span>批次</span><span></span></div>
            <div v-for="(line, index) in values[field.key]" :key="index" class="line-item-row">
              <select v-model="line.materialId" required :disabled="optionLoading[field.key]" @change="onLineMaterialChange(field, line)">
                <option value="" disabled>{{ optionLoading[field.key] ? '加载中...' : '请选择物料' }}</option>
                <option v-for="option in availableOptions(field)" :key="optionValue(field, option)" :value="optionValue(field, option)">{{ optionLabel(field, option) }}</option>
              </select>
              <input v-model.number="line.requiredQty" type="number" min="0.01" step="0.01" required />
              <input v-model="line.unit" type="text" readonly />
              <input v-model="line.batchNo" type="text" placeholder="可选" />
              <button type="button" class="line-item-remove" title="删除该项" @click="removeLine(field, index)"><Trash2 :size="16" /></button>
            </div>
          </div>
          <label v-else>
          <span>{{ field.label }}</span>
          <select v-if="field.type === 'lookup'" v-model="values[field.key]" :required="field.required" :disabled="optionLoading[field.key]" @change="onLookupChange(field)">
            <option value="" :disabled="field.required">{{ optionLoading[field.key] ? '正在加载...' : (availableOptions(field).length ? '请选择' : '当前无可选内容') }}</option>
            <option v-for="option in availableOptions(field)" :key="optionValue(field, option)" :value="optionValue(field, option)">{{ optionLabel(field, option) }}</option>
          </select>
          <select v-else-if="field.type === 'multi-lookup'" v-model="values[field.key]" multiple :required="field.required" :disabled="optionLoading[field.key]">
            <option v-for="option in availableOptions(field)" :key="optionValue(field, option)" :value="optionValue(field, option)">{{ optionLabel(field, option) }}</option>
          </select>
          <select v-else-if="field.type === 'select'" v-model="values[field.key]" :required="field.required" @change="onSelectChange(field)">
            <option value="" :disabled="field.required">请选择</option>
            <option v-for="option in field.options" :key="selectOptionValue(option)" :value="selectOptionValue(option)">{{ selectOptionLabel(field, option) }}</option>
          </select>
          <textarea v-else-if="field.type === 'json'" v-model="values[field.key]" rows="7" :required="field.required" />
          <input v-else v-model="values[field.key]" :type="field.type === 'decimal' ? 'number' : field.type" :step="field.type === 'decimal' ? '0.01' : undefined" :required="field.required" />
          </label>
        </template>
        <p v-if="optionsError" class="dialog-options-error">{{ optionsError }}</p>
        <footer>
          <button type="button" class="secondary" @click="emit('close')">取消</button>
          <button type="submit" :disabled="busy">{{ busy ? '处理中...' : '确认' }}</button>
        </footer>
      </form>
    </section>
  </div>
</template>
