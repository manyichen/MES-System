#requires -Version 5.1

<#
.SYNOPSIS
为 MES Java 源码补充适合项目答辩阅读的中文结构化注释。

.DESCRIPTION
脚本按 controller、service、dao、entity、security、test 等代码层识别文件职责，
在不修改 Java 语句、方法签名和 SQL 字符串的前提下补充三类内容：
1. 文件级“答辩定位”，说明该层在调用链中的位置；
2. 类型级职责说明，解释类对应的业务模块；
3. 方法和字段级说明；控制器方法还会组合 JAX-RS 注解，写出完整 HTTP 路径。

脚本具有幂等性：以“答辩定位”和现有 Javadoc 为标记，重复执行不会重复插入。
SQL 文件不在扫描范围内，符合项目“数据库脚本保持原样”的约束。
#>
[CmdletBinding()]
param(
    [string]$SourceRoot = "backend/src"
)

$ErrorActionPreference = "Stop"
$utf8NoBom = [System.Text.UTF8Encoding]::new($false)

# 将包路径映射到答辩时常用的业务模块名称。
$moduleNames = [ordered]@{
    ".access." = "访问控制与系统维护"
    ".auth." = "登录认证与会话"
    ".common." = "公共基础设施"
    ".dashboard." = "驾驶舱、反馈与产品追溯"
    ".equipment." = "设备与维修保养"
    ".master." = "主数据与用户"
    ".planning." = "订单、计划、齐套与工单"
    ".production." = "生产报工与计件工资"
    ".quality." = "质检、质量追溯与返工"
    ".security." = "授权策略与数据范围"
    ".trace." = "轮胎标签与公开追溯"
    ".warehouse." = "仓储、领料、拣货与机器人物流"
}

# 方法动词映射既用于普通方法，也用于 HTTP 用例说明；未命中时保留真实方法名，避免猜错业务含义。
$actionNames = [ordered]@{
    "cleanupExpiredSessions" = "清理已经过期的登录会话"
    "markSyncLogHandled" = "将同步异常标记为已处理"
    "revokeUserSessions" = "撤销指定用户的全部登录会话"
    "confirmReceipt" = "确认物流任务已经收货"
    "externalPurchase" = "模拟外部采购入库"
    "generateFromInspection" = "根据质检结果生成轮胎追溯记录"
    "generateAdvice" = "生成 AI 辅助排产建议"
    "generate" = "生成业务结果"
    "create" = "创建业务记录"
    "insert" = "写入业务记录并返回主键"
    "update" = "更新业务记录"
    "delete" = "删除业务记录"
    "remove" = "移除业务记录"
    "restore" = "恢复已删除的业务记录"
    "disable" = "停用业务对象"
    "enable" = "启用业务对象"
    "list" = "查询列表"
    "findAll" = "查询全部可见记录"
    "findById" = "按主键查询记录"
    "findBy" = "按业务条件查询记录"
    "find" = "查询匹配记录"
    "get" = "查询单条记录或详情"
    "load" = "装载业务数据"
    "inspect" = "检查运行状态"
    "login" = "校验账号密码并创建会话"
    "logout" = "注销当前会话"
    "authenticate" = "校验访问令牌并还原当前用户"
    "dispatch" = "派发业务任务"
    "assign" = "分配执行人员或资源"
    "receive" = "接收已派发任务"
    "release" = "下达业务任务"
    "start" = "开始执行业务任务"
    "finish" = "完成业务任务"
    "complete" = "完成业务任务"
    "close" = "关闭业务事项"
    "approve" = "审核通过业务事项"
    "reject" = "驳回业务事项"
    "review" = "审核业务申请"
    "apply" = "执行已审核的变更"
    "accept" = "受理业务事项"
    "report" = "提交生产或维修报告"
    "submit" = "提交业务事项"
    "cancel" = "取消业务事项"
    "arrive" = "确认物流到达"
    "unlock" = "解除账号锁定"
    "revoke" = "撤销会话或授权"
    "adjust" = "执行库存调整"
    "outbound" = "执行库存出库"
    "inbound" = "执行库存入库"
    "kitting" = "执行物料齐套分析"
    "print" = "登记标签打印"
    "preview" = "生成文件预览"
    "download" = "下载业务文件"
    "qrCode" = "读取二维码文件"
    "label" = "读取标签图片"
    "pdf" = "读取产品信息 PDF"
    "ping" = "检查数据库连通性"
    "snapshot" = "构建当前用户的数据范围快照"
    "map" = "把 JDBC 结果行映射为领域对象"
    "validate" = "校验业务输入与约束"
    "normalize" = "规范化输入并补齐默认值"
    "hash" = "计算不可逆摘要"
    "verify" = "校验输入与已保存摘要是否匹配"
}

# 高频字段采用人工定义，剩余字段会由驼峰单词规则生成可读说明。
$fieldNames = @{
    "id" = "当前记录的数据库主键"
    "userId" = "用户主键，用于关联账号、角色和审计信息"
    "username" = "登录账号，认证时作为用户唯一标识之一"
    "password" = "本次请求携带的明文密码；仅在认证边界短暂使用，不应持久化"
    "passwordHash" = "采用 PBKDF2 生成的加盐密码摘要"
    "token" = "登录后返回给客户端的原始访问令牌"
    "tokenHash" = "访问令牌摘要；数据库只保存摘要以降低泄露风险"
    "roles" = "当前用户拥有的角色编码集合"
    "permissions" = "由角色展开得到的权限点编码集合"
    "lineIds" = "当前用户被授权访问的生产线主键集合"
    "warehouseIds" = "当前用户被授权访问的仓库主键集合"
    "expiresAt" = "会话或业务对象的失效时间"
    "createdAt" = "记录创建时间，用于排序、追溯和审计"
    "updatedAt" = "记录最后更新时间"
    "createdBy" = "创建人的用户主键"
    "updatedBy" = "最后更新人的用户主键"
    "orderId" = "客户订单主键，是计划到生产链路的起点"
    "taskId" = "生产任务主键，连接客户订单与生产工单"
    "workOrderId" = "生产工单主键，连接计划、报工、质检与追溯"
    "productId" = "产品主键，关联轮胎规格、BOM 与工艺路线"
    "lineId" = "生产线主键，用于排产和数据范围隔离"
    "warehouseId" = "仓库主键，用于库存归属和数据范围隔离"
    "locationId" = "库位主键，标识库存的具体存放位置"
    "materialId" = "物料主键，关联 BOM、库存与领料明细"
    "equipmentId" = "设备主键，关联产线、维修和保养记录"
    "inspectionId" = "质量检验单主键"
    "status" = "业务状态；服务层依据它校验允许的状态流转"
    "enabled" = "是否启用；停用记录通常保留历史关联但不再参与新业务"
    "remark" = "可选业务备注"
    "quantity" = "本次业务数量"
    "plannedQuantity" = "计划生产数量"
    "reportedQuantity" = "已经报工的累计数量"
    "qualifiedQuantity" = "检验合格数量"
    "defectQuantity" = "检验不合格数量"
    "batchNo" = "生产批次号，用于质量与产品全链路追溯"
    "serialNo" = "轮胎唯一序列号"
    "traceCode" = "产品追溯业务编码"
    "accessToken" = "公开追溯页面使用的随机访问凭证"
    "publicUrl" = "二维码指向的公开追溯地址"
    "service" = "业务服务依赖；控制器只通过它编排用例，不直接访问数据库"
    "dao" = "数据访问依赖，集中封装 JDBC、SQL 参数绑定和结果映射"
}

function Get-ModuleName([string]$path) {
    $normalized = $path.Replace("\", "/")
    foreach ($entry in $moduleNames.GetEnumerator()) {
        if ($normalized.Contains($entry.Key.Replace(".", "/"))) { return $entry.Value }
    }
    return "MES 应用基础"
}

function Get-Layer([string]$path, [string]$className) {
    $normalized = $path.Replace("\", "/")
    if ($normalized.Contains("/src/test/")) { return "test" }
    if ($normalized.Contains("/controller/")) { return "controller" }
    if ($normalized.Contains("/service/")) { return "service" }
    if ($normalized.Contains("/dao/")) { return "dao" }
    if ($normalized.Contains("/entity/")) { return "entity" }
    if ($className.EndsWith("Filter") -or $className.EndsWith("Policy")) { return "security" }
    if ($className.EndsWith("Config") -or $className -in @("Db", "MesApplication", "MesBackendApplication")) { return "infrastructure" }
    return "support"
}

function Get-LayerDescription([string]$layer) {
    switch ($layer) {
        "controller" { return "HTTP 接口层：解析路径、查询参数和 JSON 请求体，取得登录用户，调用 Service，并统一包装响应。它不直接执行 SQL。" }
        "service" { return "业务服务层：实现一个或一组用例，负责必填校验、角色边界、状态机和跨 DAO 编排；数据库细节下沉到 DAO。" }
        "dao" { return "数据访问层：使用 JDBC 和 PreparedStatement 访问 PostgreSQL，集中处理 SQL 参数绑定、结果映射及需要原子性的事务。" }
        "entity" { return "领域/传输模型：承载数据库字段或接口 JSON。Jackson 通过公开字段、构造器或 record 组件完成序列化与反序列化。" }
        "security" { return "安全边界：在业务方法执行前完成身份、权限或数据范围判断，避免只依赖前端隐藏按钮。" }
        "infrastructure" { return "运行基础设施：负责应用注册、服务器启动、配置读取或数据库连接，是业务模块共享的外部依赖边界。" }
        "test" { return "自动化回归测试：固定关键业务规则、接口契约和架构边界，防止重构时出现静默回归。" }
        default { return "公共支撑代码：提供多个业务模块共享的响应、异常、编码或工具能力。" }
    }
}

function Get-CallChain([string]$layer) {
    switch ($layer) {
        "controller" { return "浏览器/Vue -> /api -> AuthFilter -> Resource -> Service -> DAO -> PostgreSQL。" }
        "service" { return "Resource -> 当前 Service -> DAO；外部 AI、文件系统等依赖也由服务边界统一编排。" }
        "dao" { return "Service -> 当前 DAO -> Db.getConnection() -> PostgreSQL；查询结果再映射为 entity/record。" }
        "entity" { return "PostgreSQL/JDBC <-> DAO <-> 当前模型 <-> Jackson JSON <-> Vue 页面。" }
        "test" { return "Maven Surefire -> JUnit 5 -> 被测类；测试替身用于隔离远程数据库或文件系统。" }
        default { return "由应用启动、HTTP 过滤器或各业务模块按需调用。" }
    }
}

function Get-ActionDescription([string]$methodName) {
    if ($actionNames.Contains($methodName)) { return $actionNames[$methodName] }
    foreach ($entry in $actionNames.GetEnumerator()) {
        if ($methodName.StartsWith($entry.Key, [StringComparison]::OrdinalIgnoreCase)) {
            return $entry.Value
        }
    }
    return "执行 $methodName 对应的业务步骤"
}

function Get-FieldDescription([string]$fieldName) {
    if ($fieldNames.ContainsKey($fieldName)) { return $fieldNames[$fieldName] }
    if ($fieldName.EndsWith("Id")) { return "$fieldName 对应的关联记录主键" }
    if ($fieldName.EndsWith("No")) { return "$fieldName 对应的业务单号，便于人工识别和检索" }
    if ($fieldName.EndsWith("Code")) { return "$fieldName 对应的稳定业务编码" }
    if ($fieldName.EndsWith("Name")) { return "$fieldName 对应的展示名称" }
    if ($fieldName.EndsWith("Status")) { return "$fieldName 对应的业务状态，决定后续可执行动作" }
    if ($fieldName.EndsWith("At") -or $fieldName.EndsWith("Time")) { return "$fieldName 对应的业务时间点" }
    if ($fieldName.EndsWith("Qty") -or $fieldName.EndsWith("Quantity")) { return "$fieldName 对应的业务数量" }
    if ($fieldName.EndsWith("Ids")) { return "$fieldName 对应的关联主键集合" }
    return "$fieldName 业务字段；具体取值由创建/更新用例校验后写入"
}

function Repair-PlaceholderFieldComments([string]$path) {
    $lines = [IO.File]::ReadAllLines($path, [Text.Encoding]::UTF8)
    $changed = $false
    for ($i = 0; $i -lt $lines.Length - 1; $i++) {
        if (-not $lines[$i].Contains('** $fieldName 业务字段；具体取值由创建/更新用例校验后写入。 */')) {
            continue
        }
        $fieldLine = $lines[$i + 1]
        if ($fieldLine -notmatch '^\s+public\s+(?!class\b|record\b|interface\b|enum\b|static\s+[^;=]+\()([\w<>?,.\[\] ]+)\s+(\w+)\s*(?:=[^;]+)?;') {
            continue
        }
        $fieldName = $Matches[2]
        $indent = [regex]::Match($lines[$i], '^\s*').Value
        $lines[$i] = "$indent/** $(Get-FieldDescription $fieldName)。 */"
        $changed = $true
    }
    if ($changed) {
        [IO.File]::WriteAllText($path, ([string]::Join("`n", $lines) + "`n"), $utf8NoBom)
    }
    return $changed
}

function Find-InsertIndex([string[]]$lines, [int]$index) {
    $insert = $index
    while ($insert -gt 0 -and $lines[$insert - 1].TrimStart().StartsWith("@")) { $insert-- }
    return $insert
}

function Has-DocBefore([string[]]$lines, [int]$index) {
    $probe = $index - 1
    while ($probe -ge 0 -and [string]::IsNullOrWhiteSpace($lines[$probe])) { $probe-- }
    if ($probe -lt 0 -or -not $lines[$probe].TrimEnd().EndsWith("*/")) { return $false }
    for ($i = $probe; $i -ge [Math]::Max(0, $probe - 30); $i--) {
        if ($lines[$i].Contains("/**")) { return $true }
        if ($lines[$i].Contains("/*") -and -not $lines[$i].Contains("/**")) { return $false }
    }
    return $false
}

function Add-Insertion([hashtable]$insertions, [int]$index, [string[]]$comment) {
    if (-not $insertions.ContainsKey($index)) { $insertions[$index] = [System.Collections.Generic.List[string]]::new() }
    foreach ($line in $comment) { $insertions[$index].Add($line) }
}

function Get-MethodComment(
    [string]$layer,
    [string]$className,
    [string]$methodName,
    [string]$httpMethod,
    [string]$route,
    [bool]$isPrivate,
    [string]$indent
) {
    $action = Get-ActionDescription $methodName
    $details = [System.Collections.Generic.List[string]]::new()
    $details.Add("$indent/**")
    if ($httpMethod) {
        $details.Add("$indent * 接口：$httpMethod $route。")
        $details.Add("$indent * 用例：$action；请求先经过 AuthFilter 的登录、权限校验，本方法再处理参数和数据范围。")
        $details.Add("$indent * 返回：成功时由 ResourceSupport/ApiResponse 形成统一 JSON；业务异常转换为 4xx，未知异常转换为 5xx。")
    } elseif ($layer -eq "dao") {
        $details.Add("$indent * 数据访问：$action。")
        $details.Add("$indent * 实现要点：SQL 使用 PreparedStatement 绑定变量，避免拼接用户输入；ResultSet 在当前调用边界内完成映射和关闭。")
        $details.Add("$indent * 调用方：对应模块的 Service；SQLException 由服务层转换为稳定的业务异常。")
    } elseif ($layer -eq "service") {
        $details.Add("$indent * 业务用例：$action。")
        $details.Add("$indent * 服务层在调用 DAO 前完成输入、关联关系和状态流转校验，保证前端绕过页面限制时规则仍然成立。")
        $details.Add("$indent * 异常语义：参数或状态不合法抛 BadRequestException，记录不存在抛 NotFoundException，数据库故障保留原因为 5xx。")
    } elseif ($layer -eq "test") {
        $details.Add("$indent * 回归场景：验证 $methodName 所描述的行为。")
        $details.Add("$indent * 测试只固定可观察结果和关键边界，失败表示接口契约、权限规则、状态流或架构约束发生变化。")
    } elseif ($isPrivate) {
        $details.Add("$indent * 内部实现步骤：$action。")
        $details.Add("$indent * 该方法不构成外部接口，只用于收拢重复细节并保持主流程可读。")
    } else {
        $details.Add("$indent * 公共能力：$action。")
        $details.Add("$indent * 由 $className 的上层调用者使用；返回值或异常继续遵循当前类的职责边界。")
    }
    $details.Add("$indent */")
    return $details.ToArray()
}

function Add-JavaComments([string]$path) {
    $repairedPlaceholders = Repair-PlaceholderFieldComments $path
    $text = [IO.File]::ReadAllText($path, [Text.Encoding]::UTF8)
    if ($text.Contains("答辩定位：")) { return $repairedPlaceholders }
    $lines = [IO.File]::ReadAllLines($path, [Text.Encoding]::UTF8)
    $code = [string]::Join("`n", $lines)
    $classMatch = [regex]::Match($code, "(?m)^\s*(?:public\s+)?(?:final\s+|abstract\s+)?(?:class|record|interface|enum)\s+(\w+)")
    if (-not $classMatch.Success) { return $repairedPlaceholders }
    $className = $classMatch.Groups[1].Value
    $layer = Get-Layer $path $className
    $module = Get-ModuleName $path
    $insertions = @{}

    # 文件头直接回答“它属于哪个模块、哪一层、由谁调用”三个答辩高频问题。
    Add-Insertion $insertions 0 @(
        "/*",
        " * 答辩定位：$module 模块的 $className。",
        " * 分层职责：$(Get-LayerDescription $layer)",
        " * 典型调用链：$(Get-CallChain $layer)",
        " * 阅读提示：公开方法是本类对上层暴露的契约；private 方法只服务于本类内部实现。",
        " */"
    )

    $classLine = -1
    for ($i = 0; $i -lt $lines.Length; $i++) {
        if ($lines[$i] -match "^\s*(?:public\s+)?(?:final\s+|abstract\s+)?(?:class|record|interface|enum)\s+$([regex]::Escape($className))\b") {
            $classLine = $i
            $insert = Find-InsertIndex $lines $i
            if (-not (Has-DocBefore $lines $insert)) {
                $classPathMatches = [regex]::Matches([string]::Join("`n", $lines[0..$i]), '@Path\("([^\"]*)"\)')
                $basePath = if ($classPathMatches.Count -gt 0) {
                    $classPathMatches[$classPathMatches.Count - 1].Groups[1].Value
                } else {
                    ""
                }
                $classDetail = if ($layer -eq "controller") {
                    "JAX-RS 控制器，统一挂载在 /api$basePath；每个带 HTTP 注解的方法对应一个可被前端或 Postman 调用的接口。"
                } elseif ($layer -eq "entity") {
                    "$module 的数据模型；字段名与接口 JSON/数据库列保持可追踪关系，不在模型中实现业务规则。"
                } else {
                    "$module 的 $className，承担当前文件头所述职责，并保持与相邻层的单向依赖。"
                }
                Add-Insertion $insertions $insert @("/**", " * $classDetail", " */")
            }
            break
        }
    }

    $basePath = ""
    if ($classLine -ge 0) {
        $beforeClass = [string]::Join("`n", $lines[0..$classLine])
        $pathMatches = [regex]::Matches($beforeClass, '@Path\("([^\"]*)"\)')
        if ($pathMatches.Count -gt 0) { $basePath = $pathMatches[$pathMatches.Count - 1].Groups[1].Value }
    }

    for ($i = 0; $i -lt $lines.Length; $i++) {
        $line = $lines[$i]

        # DTO/entity 的公开字段就是 JSON 契约的一部分，需要逐字段解释。
        if ($line -match "^\s+public\s+(?!class\b|record\b|interface\b|enum\b|static\s+[^;=]+\()([\w<>?,.\[\] ]+)\s+(\w+)\s*(?:=[^;]+)?;") {
            $fieldName = $Matches[2]
            if (-not (Has-DocBefore $lines $i)) {
                Add-Insertion $insertions $i @("    /** $(Get-FieldDescription $fieldName)。 */")
            }
            continue
        }

        # 依赖字段和常量写简短说明；局部变量由所属方法注释解释，避免逐行噪声。
        if ($line -match "^\s+private\s+(?:static\s+)?(?:final\s+)?[\w<>?,.\[\] ]+\s+(\w+)\s*(?:=[^;]+)?;") {
            $fieldName = $Matches[1]
            if (-not (Has-DocBefore $lines $i) -and ($fieldName -in @("service", "dao") -or $fieldName.EndsWith("Service") -or $fieldName.EndsWith("Dao"))) {
                Add-Insertion $insertions $i @("    /** $(Get-FieldDescription $(if ($fieldName.EndsWith('Service')) {'service'} elseif ($fieldName.EndsWith('Dao')) {'dao'} else {$fieldName}))。 */")
            }
        }

        $visibility = ""
        $methodName = ""
        if ($line -match "^\s*(public|protected|private)\s+(?:(?:static|final|synchronized|abstract|default)\s+)*[\w<>,.?\[\] ]*?(\w+)\s*\(") {
            $visibility = $Matches[1]
            $methodName = $Matches[2]
        } elseif ($layer -eq "test" -and $line -match "^\s*(?:(?:static|final|synchronized)\s+)*(?:void|[A-Z][\w<>,.?\[\] ]*)\s+(\w+)\s*\(") {
            $visibility = "package"
            $methodName = $Matches[1]
        } else {
            continue
        }
        if ($methodName -in @("if", "for", "while", "switch", "catch")) { continue }
        $insert = Find-InsertIndex $lines $i
        if (Has-DocBefore $lines $insert) { continue }

        $annotationText = if ($insert -lt $i) { [string]::Join("`n", $lines[$insert..($i - 1)]) } else { "" }
        $httpMatch = [regex]::Match($annotationText, "@(GET|POST|PUT|DELETE|PATCH)\b")
        $httpMethod = if ($httpMatch.Success) { $httpMatch.Groups[1].Value } else { "" }
        $methodPathMatch = [regex]::Match($annotationText, '@Path\("([^\"]*)"\)')
        $methodPath = if ($methodPathMatch.Success) { $methodPathMatch.Groups[1].Value } else { "" }
        $route = ("/api/" + $basePath.Trim("/") + "/" + $methodPath.Trim("/")).Replace("//", "/").TrimEnd("/")
        if ([string]::IsNullOrEmpty($route)) { $route = "/api" }
        $indent = [regex]::Match($line, "^\s*").Value
        Add-Insertion $insertions $insert (Get-MethodComment $layer $className $methodName $httpMethod $route ($visibility -eq "private") $indent)
    }

    $output = [System.Collections.Generic.List[string]]::new()
    for ($i = 0; $i -le $lines.Length; $i++) {
        if ($insertions.ContainsKey($i)) {
            foreach ($commentLine in $insertions[$i]) { $output.Add($commentLine) }
        }
        if ($i -lt $lines.Length) { $output.Add($lines[$i]) }
    }
    [IO.File]::WriteAllText($path, ([string]::Join("`n", $output) + "`n"), $utf8NoBom)
    return $true
}

$root = (Resolve-Path $SourceRoot).Path
$changed = 0
Get-ChildItem -LiteralPath $root -Recurse -Filter "*.java" | Sort-Object FullName | ForEach-Object {
    if (Add-JavaComments $_.FullName) {
        $script:changed++
        Write-Host "已注释 $($_.FullName.Substring($root.Length + 1))"
    }
}
Write-Host "完成：新增或补充注释的 Java 文件 $changed 个。"
