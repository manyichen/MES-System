param(
    [string]$OutputPath = (Join-Path $PSScriptRoot "..\docs\双星轮胎MES制造执行系统-项目答辩.pptx"),
    [string]$ExportDir = (Join-Path $PSScriptRoot "..\tmp\ppt-output")
)

$ErrorActionPreference = "Stop"

function Color([int]$r, [int]$g, [int]$b) {
    return $r + ($g * 256) + ($b * 65536)
}

$C = @{
    Navy = Color 6 31 74
    Deep = Color 3 20 51
    Blue = Color 20 101 220
    Cyan = Color 15 177 255
    Sky = Color 207 235 255
    Pale = Color 240 248 255
    White = Color 255 255 255
    Ink = Color 18 43 76
    Gray = Color 95 117 142
    Line = Color 174 211 242
    Green = Color 10 162 139
    Orange = Color 255 139 38
    Red = Color 239 79 96
    Purple = Color 109 83 224
}

function Set-Fill($shape, [int]$color, [double]$transparency = 0) {
    $shape.Fill.Visible = -1
    $shape.Fill.Solid()
    $shape.Fill.ForeColor.RGB = $color
    $shape.Fill.Transparency = $transparency
}

function Set-Line($shape, [int]$color, [double]$weight = 1, [double]$transparency = 0) {
    $shape.Line.Visible = -1
    $shape.Line.ForeColor.RGB = $color
    $shape.Line.Weight = $weight
    $shape.Line.Transparency = $transparency
}

function Add-Rect($slide, [double]$x, [double]$y, [double]$w, [double]$h, [int]$fill, [int]$line = -1, [double]$radiusType = 1, [double]$transparency = 0) {
    $shapeType = if ($radiusType -eq 5) { 5 } else { 1 }
    $s = $slide.Shapes.AddShape($shapeType, $x, $y, $w, $h)
    Set-Fill $s $fill $transparency
    if ($line -ge 0) { Set-Line $s $line 1 } else { $s.Line.Visible = 0 }
    return $s
}

function Add-Text($slide, [string]$text, [double]$x, [double]$y, [double]$w, [double]$h,
                  [double]$size = 18, [int]$color = 0, [bool]$bold = $false,
                  [int]$align = 1, [string]$font = "Microsoft YaHei", [double]$margin = 0) {
    $s = $slide.Shapes.AddTextbox(1, $x, $y, $w, $h)
    $s.TextFrame2.MarginLeft = $margin
    $s.TextFrame2.MarginRight = $margin
    $s.TextFrame2.MarginTop = $margin
    $s.TextFrame2.MarginBottom = $margin
    $s.TextFrame2.WordWrap = -1
    $s.TextFrame2.AutoSize = 0
    $r = $s.TextFrame2.TextRange
    $r.Text = $text
    $r.Font.Name = $font
    $r.Font.NameFarEast = "微软雅黑"
    $r.Font.Size = $size
    $r.Font.Fill.ForeColor.RGB = $color
    $r.Font.Bold = if ($bold) { -1 } else { 0 }
    $r.ParagraphFormat.Alignment = $align
    $r.ParagraphFormat.SpaceAfter = 0
    return $s
}

function Add-Line($slide, [double]$x1, [double]$y1, [double]$x2, [double]$y2,
                  [int]$color, [double]$weight = 1.5, [bool]$arrow = $false, [double]$transparency = 0) {
    $s = $slide.Shapes.AddLine($x1, $y1, $x2, $y2)
    Set-Line $s $color $weight $transparency
    if ($arrow) { $s.Line.EndArrowheadStyle = 3 }
    return $s
}

function Add-Pill($slide, [string]$text, [double]$x, [double]$y, [double]$w,
                  [int]$fill, [int]$textColor = 0, [double]$size = 12) {
    $s = Add-Rect $slide $x $y $w 25 $fill -1 5 0
    [void](Add-Text $slide $text $x ($y + 3) $w 18 $size $textColor $true 2)
    return $s
}

function Add-Badge($slide, [string]$text, [double]$x, [double]$y, [int]$fill = 0) {
    $s = $slide.Shapes.AddShape(9, $x, $y, 30, 30)
    Set-Fill $s $fill 0
    $s.Line.Visible = 0
    [void](Add-Text $slide $text $x ($y + 3) 30 22 12 $C.White $true 2)
    return $s
}

function Add-Card($slide, [double]$x, [double]$y, [double]$w, [double]$h,
                  [string]$title, [string]$body, [int]$accent = 0, [string]$tag = "") {
    $shadow = Add-Rect $slide ($x + 4) ($y + 5) $w $h $C.Blue -1 5 0.88
    $card = Add-Rect $slide $x $y $w $h $C.White $C.Line 5 0.03
    [void](Add-Rect $slide $x $y 5 $h $accent -1 1 0)
    if ($tag) {
        [void](Add-Pill $slide $tag ($x + 18) ($y + 14) 70 $C.Pale $accent 10)
        [void](Add-Text $slide $title ($x + 100) ($y + 16) ($w - 118) 30 17 $C.Ink $true 1)
        [void](Add-Text $slide $body ($x + 18) ($y + 55) ($w - 36) ($h - 65) 11.5 $C.Gray $false 1)
    }
    else {
        [void](Add-Text $slide $title ($x + 18) ($y + 17) ($w - 36) 30 18 $C.Ink $true 1)
        [void](Add-Text $slide $body ($x + 18) ($y + 51) ($w - 36) ($h - 61) 12.5 $C.Gray $false 1)
    }
    return $card
}

function Add-Background($slide, [bool]$dark = $false) {
    $bg = $slide.Background.Fill
    $bg.Visible = -1
    $bg.Solid()
    $bg.ForeColor.RGB = if ($dark) { $C.Deep } else { $C.Pale }

    if (-not $dark) {
        [void](Add-Rect $slide 0 0 960 540 $C.White -1 1 0.20)
        $orb1 = $slide.Shapes.AddShape(9, 720, -120, 360, 360)
        Set-Fill $orb1 $C.Cyan 0.87
        $orb1.Line.Visible = 0
        $orb2 = $slide.Shapes.AddShape(9, -150, 360, 320, 320)
        Set-Fill $orb2 $C.Blue 0.93
        $orb2.Line.Visible = 0
        for ($i = 0; $i -lt 6; $i++) {
            [void](Add-Line $slide (60 + $i * 140) 516 (175 + $i * 140) 516 $C.Line 1 $false 0.25)
        }
    }
}

function Add-Header($slide, [int]$number, [string]$section, [string]$title, [string]$subtitle = "") {
    Add-Background $slide $false
    [void](Add-Rect $slide 48 28 8 34 $C.Blue -1 1 0)
    [void](Add-Text $slide $section 72 25 240 18 10.5 $C.Blue $true 1)
    [void](Add-Text $slide $title 72 43 650 40 27 $C.Navy $true 1)
    if ($subtitle) { [void](Add-Text $slide $subtitle 72 81 720 23 11.5 $C.Gray $false 1) }
    [void](Add-Line $slide 72 108 910 108 $C.Line 1.2)
    [void](Add-Pill $slide ("{0:D2}" -f $number) 866 30 44 $C.Blue $C.White 11)
    [void](Add-Text $slide "双星轮胎 MES · 项目答辩" 690 514 220 16 8.5 $C.Gray $false 3)
}

function Add-PictureContain($slide, [string]$path, [double]$x, [double]$y, [double]$w, [double]$h,
                            [bool]$border = $true, [int]$borderColor = 0) {
    if (-not (Test-Path $path)) { return $null }
    Add-Type -AssemblyName System.Drawing
    $img = [System.Drawing.Image]::FromFile($path)
    try {
        $ratio = [Math]::Min($w / $img.Width, $h / $img.Height)
        $pw = $img.Width * $ratio
        $ph = $img.Height * $ratio
        $px = $x + (($w - $pw) / 2)
        $py = $y + (($h - $ph) / 2)
        $pic = $slide.Shapes.AddPicture($path, 0, -1, $px, $py, $pw, $ph)
        if ($border) { Set-Line $pic $borderColor 1.25 }
        return $pic
    }
    finally { $img.Dispose() }
}

function Add-FlowNode($slide, [int]$n, [string]$title, [string]$sub,
                      [double]$x, [double]$y, [double]$w, [int]$accent) {
    [void](Add-Rect $slide $x $y $w 72 $C.White $C.Line 5 0)
    [void](Add-Badge $slide ([string]$n) ($x + 12) ($y + 12) $accent)
    [void](Add-Text $slide $title ($x + 52) ($y + 13) ($w - 62) 22 14 $C.Ink $true 1)
    [void](Add-Text $slide $sub ($x + 52) ($y + 38) ($w - 62) 25 10.5 $C.Gray $false 1)
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$assets = @{
    Logo = Join-Path $root "frontend\assets\mes-icon.png"
    Cockpit = Join-Path $root "backend\target\executive-overview.png"
    Live = Join-Path $root "backend\target\executive-live.png"
    Routes = Join-Path $root "backend\target\process-routes-visual.png"
    Inventory = Join-Path $root "backend\target\inventory-balance-visual.png"
    Dashboard = Join-Path $root "backend\target\vue-dashboard-desktop.png"
    Flow = Join-Path $root "docs\系统模块图.jpg"
    ErPlan = Join-Path $root "docs\生产计划与工单管理模块ER图.png"
    ErExec = Join-Path $root "docs\生产执行与仓储物流模块ER图.png"
    ErQuality = Join-Path $root "docs\质量设备与基础管理模块ER图.png"
}

$outputFull = [System.IO.Path]::GetFullPath($OutputPath)
$exportFull = [System.IO.Path]::GetFullPath($ExportDir)
[System.IO.Directory]::CreateDirectory([System.IO.Path]::GetDirectoryName($outputFull)) | Out-Null
if (Test-Path $exportFull) { Remove-Item -LiteralPath $exportFull -Recurse -Force }
[System.IO.Directory]::CreateDirectory($exportFull) | Out-Null

$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$presentation = $ppt.Presentations.Add()
$presentation.PageSetup.SlideWidth = 960
$presentation.PageSetup.SlideHeight = 540

try {
    # 01 封面
    $s = $presentation.Slides.Add(1, 12)
    Add-Background $s $true
    [void](Add-Rect $s 0 0 960 540 $C.Navy -1 1 0.16)
    for ($i = 0; $i -lt 8; $i++) {
        [void](Add-Line $s (520 + $i * 55) 0 (820 + $i * 55) 540 $C.Cyan 1 $false (0.70 + $i * 0.025))
    }
    $orb = $s.Shapes.AddShape(9, 650, 96, 245, 245)
    Set-Fill $orb $C.Blue 0.55
    Set-Line $orb $C.Cyan 2 0.2
    $orb2 = $s.Shapes.AddShape(9, 688, 134, 169, 169)
    Set-Fill $orb2 $C.Deep 0.12
    Set-Line $orb2 $C.Cyan 1 0.35
    [void](Add-PictureContain $s $assets.Logo 727 170 92 92 $false $C.Cyan)
    [void](Add-Pill $s "PROJECT DEFENSE · 2026" 62 50 196 $C.Cyan $C.Deep 10)
    [void](Add-Text $s "双星轮胎 MES" 62 123 560 70 38 $C.White $true 1)
    [void](Add-Text $s "制造执行系统" 62 188 560 68 36 $C.White $true 1)
    [void](Add-Rect $s 62 278 420 3 $C.Cyan -1 1 0)
    [void](Add-Text $s "从生产计划到质量追溯的全流程数字化闭环" 62 300 550 34 17 $C.Sky $false 1)
    [void](Add-Text $s "第 XX 组  ｜  成员：XXX、XXX、XXX" 62 405 500 25 14 $C.White $false 1)
    [void](Add-Text $s "项目答辩 · 可编辑演示稿" 62 441 360 20 10.5 $C.Sky $false 1)
    [void](Add-Text $s "01 / 18" 856 501 60 16 9 $C.Sky $false 3)

    # 02 项目背景
    $s = $presentation.Slides.Add(2, 12)
    Add-Header $s 2 "01 · PROJECT BACKGROUND" "项目背景与行业痛点" "轮胎制造链条长、角色多、状态复杂，信息断点会直接放大交付与质量风险"
    [void](Add-Card $s 62 136 250 188 "计划协同难" "订单、任务、工单彼此割裂；齐套情况不能及时反馈，排产经常被缺料打断。" $C.Blue "PLAN")
    [void](Add-Card $s 355 136 250 188 "现场透明度低" "接单、领料、报工、质检分散记录；管理者难以及时判断产线负荷与异常。" $C.Green "EXECUTE")
    [void](Add-Card $s 648 136 250 188 "质量追溯慢" "批次、工序、设备、质检结果关联不足；异常产品定位和责任回溯耗时。" $C.Orange "TRACE")
    [void](Add-Rect $s 62 350 836 112 $C.Navy -1 5 0)
    [void](Add-Text $s "项目机会" 84 371 120 24 15 $C.Cyan $true 1)
    [void](Add-Text $s "以统一业务状态机串联计划、执行、仓储、质量、设备与追溯，让每一次操作都有依据、每一条数据都可核验。" 84 404 750 38 18 $C.White $true 1)
    [void](Add-Pill $s "数据驱动" 785 370 86 $C.Cyan $C.Deep 11)

    # 03 项目概述
    $s = $presentation.Slides.Add(3, 12)
    Add-Header $s 3 "02 · PROJECT OVERVIEW" "项目概述与建设目标" "面向双星轮胎制造场景，打造可演示、可扩展、可追溯的 MES 一体化平台"
    [void](Add-Text $s "ONE PLATFORM" 62 138 220 22 11 $C.Blue $true 1)
    [void](Add-Text $s "一套系统贯穿生产经营全链路" 62 164 500 34 23 $C.Ink $true 1)
    [void](Add-Text $s "超级管理员可在单一账号中展示业务发起、接收、审核与追踪；业务角色仍保留权限与数据范围边界。" 62 208 470 55 13 $C.Gray $false 1)
    $metrics = @(
        @("12", "业务模块", $C.Blue),
        @("22", "流程节点", $C.Green),
        @("4", "闭环阶段", $C.Orange),
        @("1", "统一数据底座", $C.Purple)
    )
    for ($i = 0; $i -lt 4; $i++) {
        $x = 62 + $i * 210
        [void](Add-Rect $s $x 302 185 112 $C.White $C.Line 5 0)
        [void](Add-Text $s $metrics[$i][0] ($x + 18) 318 75 48 31 $metrics[$i][2] $true 1)
        [void](Add-Text $s $metrics[$i][1] ($x + 18) 371 145 24 13 $C.Ink $true 1)
    }
    [void](Add-Pill $s "目标：流程在线化" 62 438 170 $C.Sky $C.Navy 11)
    [void](Add-Pill $s "目标：状态可视化" 246 438 170 $C.Sky $C.Navy 11)
    [void](Add-Pill $s "目标：异常闭环化" 430 438 170 $C.Sky $C.Navy 11)
    [void](Add-Pill $s "目标：产品可追溯" 614 438 170 $C.Sky $C.Navy 11)

    # 04 方案价值
    $s = $presentation.Slides.Add(4, 12)
    Add-Header $s 4 "02 · SOLUTION" "总体解决方案" "以订单为起点、以产品追溯为终点，形成业务流、物流、质量流和数据流四流合一"
    $nodes = @(
        @("订单", "需求进入", $C.Blue), @("计划", "任务齐套", $C.Blue),
        @("执行", "工单报工", $C.Green), @("仓储", "领料配送", $C.Green),
        @("质量", "抽检返工", $C.Orange), @("追溯", "批次闭环", $C.Purple)
    )
    for ($i = 0; $i -lt 6; $i++) {
        $x = 54 + $i * 149
        [void](Add-Rect $s $x 162 122 92 $C.White $C.Line 5 0)
        [void](Add-Badge $s ([string]($i + 1)) ($x + 12) 176 $nodes[$i][2])
        [void](Add-Text $s $nodes[$i][0] ($x + 50) 176 62 22 15 $C.Ink $true 1)
        [void](Add-Text $s $nodes[$i][1] ($x + 12) 216 100 20 10.5 $C.Gray $false 2)
        if ($i -lt 5) { [void](Add-Line $s ($x + 122) 208 ($x + 147) 208 $C.Blue 2 $true 0.1) }
    }
    [void](Add-Rect $s 62 302 836 133 $C.Navy -1 5 0)
    $values = @(
        @("对管理层", "驾驶舱统一查看交付、质量、设备与经营风险"),
        @("对业务岗", "按角色进入工作台，减少跨表沟通与重复录入"),
        @("对答辩演示", "超级管理员单账号串联全部关键流程与审核动作")
    )
    for ($i = 0; $i -lt 3; $i++) {
        $x = 84 + $i * 270
        [void](Add-Text $s $values[$i][0] $x 325 220 22 13 $C.Cyan $true 1)
        [void](Add-Text $s $values[$i][1] $x 357 220 51 12.5 $C.White $false 1)
    }

    # 05 技术架构
    $s = $presentation.Slides.Add(5, 12)
    Add-Header $s 5 "03 · TECHNOLOGY" "技术栈与系统架构" "前后端分离、统一 API、分层业务逻辑与 PostgreSQL 数据底座"
    $layers = @(
        @("展示层", "Vue 3 · Vite · Pinia · Vue Router", $C.Blue),
        @("接口层", "REST API · JSON · 统一异常与响应", $C.Cyan),
        @("业务层", "Java 21 · Jakarta REST / Jersey · Service", $C.Green),
        @("数据层", "DAO · JDBC · PostgreSQL / RDS", $C.Purple)
    )
    for ($i = 0; $i -lt 4; $i++) {
        $y = 137 + $i * 76
        [void](Add-Rect $s 78 $y 516 58 $C.White $C.Line 5 0)
        [void](Add-Rect $s 78 $y 106 58 $layers[$i][2] -1 5 0)
        [void](Add-Text $s $layers[$i][0] 88 ($y + 17) 86 22 13 $C.White $true 2)
        [void](Add-Text $s $layers[$i][1] 205 ($y + 17) 365 23 14 $C.Ink $true 1)
        if ($i -lt 3) { [void](Add-Line $s 336 ($y + 58) 336 ($y + 75) $C.Blue 1.8 $true 0.2) }
    }
    [void](Add-Card $s 630 137 268 134 "安全与治理" "RBAC 权限编码`n角色数据范围`n职责分离与审计日志" $C.Orange "SECURITY")
    [void](Add-Card $s 630 289 268 134 "工程化能力" "模块化前端配置`nController / Service / DAO 分层`n数据库迁移与接口测试" $C.Green "ENGINEERING")
    [void](Add-Pill $s "Browser" 118 449 96 $C.Sky $C.Navy 10)
    [void](Add-Line $s 214 461 304 461 $C.Blue 1.5 $true)
    [void](Add-Pill $s "REST API" 305 449 106 $C.Sky $C.Navy 10)
    [void](Add-Line $s 411 461 501 461 $C.Blue 1.5 $true)
    [void](Add-Pill $s "PostgreSQL" 502 449 120 $C.Sky $C.Navy 10)

    # 06 人员分工与计划
    $s = $presentation.Slides.Add(6, 12)
    Add-Header $s 6 "04 · TEAM & PLAN" "人员分工与开发计划" "三人并行分层开发，公共规范先行，按业务闭环集成验收"
    $team = @(
        @("成员 A · XXX", "项目统筹 / 计划工单", "需求梳理、生产计划、工单流程、联调组织", $C.Blue),
        @("成员 B · XXX", "生产仓储 / 数据库", "生产执行、仓储物流、数据模型、接口实现", $C.Green),
        @("成员 C · XXX", "质量设备 / 前端", "质量闭环、设备维护、驾驶舱、前端体验", $C.Orange)
    )
    for ($i = 0; $i -lt 3; $i++) {
        $x = 62 + $i * 279
        [void](Add-Card $s $x 136 252 145 $team[$i][0] ($team[$i][1] + "`n" + $team[$i][2]) $team[$i][3] ("0" + ($i + 1)))
    }
    [void](Add-Text $s "迭代里程碑" 62 315 180 26 17 $C.Ink $true 1)
    $milestones = @(
        @("01", "需求与原型", "角色/用例/数据模型"),
        @("02", "核心开发", "模块并行与接口联调"),
        @("03", "流程打通", "端到端业务验收"),
        @("04", "优化交付", "权限/性能/答辩演练")
    )
    [void](Add-Line $s 120 391 844 391 $C.Line 4)
    for ($i = 0; $i -lt 4; $i++) {
        $x = 90 + $i * 230
        [void](Add-Badge $s $milestones[$i][0] $x 376 $C.Blue)
        [void](Add-Text $s $milestones[$i][1] ($x - 12) 414 110 20 12.5 $C.Ink $true 1)
        [void](Add-Text $s $milestones[$i][2] ($x - 12) 438 165 20 10 $C.Gray $false 1)
    }

    # 07 需求分析
    $s = $presentation.Slides.Add(7, 12)
    Add-Header $s 7 "05 · REQUIREMENTS" "需求分析、角色与用例" "围绕角色、业务状态、数据范围和操作权限设计完整工作流"
    [void](Add-Text $s "关键角色" 62 132 120 25 16 $C.Ink $true 1)
    $roles = @("超级管理员", "计划员 / PMC", "车间主管", "操作工", "仓库管理员", "质量主管", "设备主管")
    for ($i = 0; $i -lt $roles.Count; $i++) {
        $col = $i % 2
        $row = [Math]::Floor($i / 2)
        $x = 62 + $col * 178
        $y = 170 + $row * 55
        [void](Add-Pill $s $roles[$i] $x $y 155 $(if ($i -eq 0) { $C.Navy } else { $C.Sky }) $(if ($i -eq 0) { $C.White } else { $C.Navy }) 10.5)
    }
    [void](Add-Text $s "核心用例" 442 132 120 25 16 $C.Ink $true 1)
    $uses = @(
        @("发起", "订单、任务、领料、报修"),
        @("接收", "工单、领料、拣货、配送"),
        @("执行", "报工、质检、维护、入库"),
        @("审核", "工单、领料、报工、权限"),
        @("追踪", "进度、库存、批次、产品")
    )
    for ($i = 0; $i -lt 5; $i++) {
        $y = 166 + $i * 57
        [void](Add-Badge $s ([string]($i + 1)) 442 $y $(if ($i -lt 2) { $C.Blue } elseif ($i -lt 4) { $C.Green } else { $C.Orange }))
        [void](Add-Text $s $uses[$i][0] 484 ($y + 2) 70 22 13 $C.Ink $true 1)
        [void](Add-Text $s $uses[$i][1] 557 ($y + 2) 285 22 12 $C.Gray $false 1)
    }
    [void](Add-Rect $s 62 431 836 46 $C.Navy -1 5 0)
    [void](Add-Text $s "原型原则：统一导航 · 角色工作台 · 状态驱动按钮 · 表格与弹窗复用 · 桌面端优先兼顾移动端" 80 443 800 22 12.5 $C.White $true 2)

    # 08 全流程
    $s = $presentation.Slides.Add(8, 12)
    Add-Header $s 8 "06 · BUSINESS FLOW" "端到端业务流程" "22 个关键节点，覆盖计划、生产、仓储、质量、设备、追溯与经营看板"
    [void](Add-Rect $s 56 126 848 354 $C.White $C.Line 5 0)
    [void](Add-PictureContain $s $assets.Flow 68 136 824 334 $false $C.Blue)

    # 09 功能架构
    $s = $presentation.Slides.Add(9, 12)
    Add-Header $s 9 "06 · FUNCTION MAP" "系统功能结构" "12 个业务模块共享统一身份、权限、状态、通知与审计能力"
    $modules = @(
        @("经营驾驶舱", "经营概况 / 实时运行", $C.Blue), @("计划与工单", "订单 / 任务 / 齐套 / 工单", $C.Blue),
        @("生产报工", "接单 / 报工 / 计件", $C.Green), @("仓储物流", "库存 / 领料 / 拣货 / 配送", $C.Green),
        @("质量管理", "抽检 / 判定 / 返工", $C.Orange), @("设备维护", "报修 / 工单 / 保养", $C.Orange),
        @("工艺与主数据", "产品 / 路线 / 产线", $C.Purple), @("产品追溯", "批次 / 工序 / 质量", $C.Purple),
        @("管理反馈", "异常 / 通知 / 处置", $C.Cyan), @("用户与权限", "角色 / 权限 / 数据范围", $C.Cyan),
        @("个人资料", "账户 / 安全 / 会话", $C.Gray), @("系统审计", "日志 / 健康 / 监控", $C.Gray)
    )
    for ($i = 0; $i -lt 12; $i++) {
        $col = $i % 4
        $row = [Math]::Floor($i / 4)
        $x = 58 + $col * 222
        $y = 132 + $row * 102
        [void](Add-Rect $s $x $y 202 82 $C.White $C.Line 5 0)
        [void](Add-Rect $s $x $y 6 82 $modules[$i][2] -1 1 0)
        [void](Add-Text $s $modules[$i][0] ($x + 18) ($y + 14) 165 22 14 $C.Ink $true 1)
        [void](Add-Text $s $modules[$i][1] ($x + 18) ($y + 43) 170 22 10.5 $C.Gray $false 1)
    }
    [void](Add-Pill $s "公共能力：RBAC · 数据范围 · 状态机 · 消息通知 · 操作审计" 196 450 568 $C.Navy $C.White 11)

    # 10 驾驶舱
    $s = $presentation.Slides.Add(10, 12)
    Add-Header $s 10 "07 · CORE INTERFACE" "亮点一：经营驾驶舱" "公司概况与实时运行双页面，支撑管理层总览和现场态势感知"
    [void](Add-Pill $s "公司经营概况" 69 126 135 $C.Blue $C.White 10.5)
    [void](Add-Pill $s "公司实时运行" 502 126 135 $C.Green $C.White 10.5)
    [void](Add-Rect $s 62 157 407 262 $C.Deep $C.Cyan 5 0)
    [void](Add-Rect $s 493 157 407 262 $C.Deep $C.Cyan 5 0)
    [void](Add-PictureContain $s $assets.Cockpit 68 163 395 250 $false $C.Cyan)
    [void](Add-PictureContain $s $assets.Live 499 163 395 250 $false $C.Cyan)
    [void](Add-Text $s "订单、产出、合格率、设备可用率、风险事项" 68 433 390 22 11 $C.Gray $false 2)
    [void](Add-Text $s "在线产线、工单负荷、设备状态、实时风险" 499 433 390 22 11 $C.Gray $false 2)
    [void](Add-Pill $s "真实业务数据实时聚合" 366 469 228 $C.Navy $C.White 11)

    # 11 计划齐套
    $s = $presentation.Slides.Add(11, 12)
    Add-Header $s 11 "07 · CORE WORKFLOW" "亮点二：计划、齐套与欠料闭环" "生产任务不直接绑定产线，先基于物料需求完成齐套分析，再进入工单派发"
    $flow11 = @(
        @("1", "创建任务", "选择订单 / 数量", $C.Blue),
        @("2", "齐套分析", "BOM × 库存余额", $C.Blue),
        @("3", "欠料预警", "一次发布 / 可关闭", $C.Orange),
        @("4", "采购入库", "物料自动匹配仓库", $C.Green),
        @("5", "再次分析", "库存变化后转已齐套", $C.Green)
    )
    for ($i = 0; $i -lt 5; $i++) {
        $x = 47 + $i * 181
        Add-FlowNode $s ([int]$flow11[$i][0]) $flow11[$i][1] $flow11[$i][2] $x 148 152 $flow11[$i][3]
        if ($i -lt 4) { [void](Add-Line $s ($x + 152) 184 ($x + 178) 184 $C.Blue 2 $true 0.1) }
    }
    [void](Add-Card $s 62 270 252 143 "状态驱动" "只有缺料任务显示齐套分析；欠料预警发布后隐藏重复按钮；齐套后可生成制造工单。" $C.Blue "RULE")
    [void](Add-Card $s 354 270 252 143 "库存一致性" "采购入库写入标准物料、仓库和库位；齐套计算统一读取可用库存余额。" $C.Green "DATA")
    [void](Add-Card $s 646 270 252 143 "审核留痕" "任务发布、预警、工单接收与派发均记录操作者、时间和业务状态。" $C.Orange "AUDIT")
    [void](Add-Pill $s "异常不是终点：采购补齐 → 重新分析 → 恢复生产" 256 445 448 $C.Navy $C.White 11)

    # 12 生产质量闭环
    $s = $presentation.Slides.Add(12, 12)
    Add-Header $s 12 "07 · CORE WORKFLOW" "亮点三：生产报工与质量闭环" "工单接收、领料、报工、审核、质检、返工形成可验证的状态链"
    $steps12 = @(
        @("接收工单", "RECEIVED", $C.Blue), @("发起领料", "REQUESTED", $C.Green),
        @("生产报工", "SUBMITTED", $C.Green), @("审核报工", "APPROVED", $C.Blue),
        @("质量抽检", "PASSED / FAILED", $C.Orange), @("返工追踪", "REWORK", $C.Red)
    )
    for ($i = 0; $i -lt 6; $i++) {
        $col = $i % 3
        $row = [Math]::Floor($i / 3)
        $x = 66 + $col * 286
        $y = 145 + $row * 112
        [void](Add-Rect $s $x $y 250 86 $C.White $C.Line 5 0)
        [void](Add-Badge $s ([string]($i + 1)) ($x + 14) ($y + 16) $steps12[$i][2])
        [void](Add-Text $s $steps12[$i][0] ($x + 57) ($y + 14) 170 22 14 $C.Ink $true 1)
        [void](Add-Pill $s $steps12[$i][1] ($x + 57) ($y + 47) 150 $C.Pale $steps12[$i][2] 9.5)
        if (($col -lt 2)) { [void](Add-Line $s ($x + 250) ($y + 43) ($x + 282) ($y + 43) $C.Blue 1.7 $true 0.15) }
    }
    [void](Add-Rect $s 66 390 822 68 $C.Navy -1 5 0)
    [void](Add-Text $s "超级管理员答辩模式" 88 408 200 22 13 $C.Cyan $true 1)
    [void](Add-Text $s "单账号可完成接单、领料、报工和审核；正式业务账号仍按岗位职责进行权限隔离。" 280 407 570 30 13 $C.White $true 1)

    # 13 仓储物流
    $s = $presentation.Slides.Add(13, 12)
    Add-Header $s 13 "07 · CORE INTERFACE" "亮点四：仓储物流与库存一致性" "同一库存口径展示仓库、物料、规格、批次、库位、本批次库存、仓库可用总量与单位"
    [void](Add-Rect $s 58 132 582 337 $C.White $C.Line 5 0)
    [void](Add-PictureContain $s $assets.Inventory 66 140 566 321 $false $C.Blue)
    [void](Add-Card $s 665 132 235 96 "采购自动归仓" "选择物料后自动带出对应仓库，减少错误入库。" $C.Blue "01")
    [void](Add-Card $s 665 244 235 96 "余额口径统一" "批次余额与仓库可用总量同时呈现，支持齐套核验。" $C.Green "02")
    [void](Add-Card $s 665 356 235 96 "物流任务闭环" "领料审核后生成拣货、配送任务并跟踪交接状态。" $C.Orange "03")

    # 14 工艺设备
    $s = $presentation.Slides.Add(14, 12)
    Add-Header $s 14 "07 · CORE INTERFACE" "亮点五：完整工艺路线与设备协同" "按产品聚合展示完整工艺路线，避免将每个工序误当成一条路线"
    [void](Add-Rect $s 60 134 536 304 $C.White $C.Line 5 0)
    [void](Add-PictureContain $s $assets.Routes 68 142 520 288 $false $C.Blue)
    [void](Add-Pill $s "产品主数据 → 工艺路线 → 生产产线" 116 450 424 $C.Navy $C.White 11)
    [void](Add-Text $s "设备异常闭环" 635 140 220 27 17 $C.Ink $true 1)
    $deviceSteps = @(
        @("故障报修", "现场发现异常", $C.Orange),
        @("维修派单", "设备主管分配", $C.Blue),
        @("执行反馈", "维修过程记录", $C.Green),
        @("恢复生产", "状态回写产线", $C.Purple)
    )
    for ($i = 0; $i -lt 4; $i++) {
        $y = 184 + $i * 66
        [void](Add-Badge $s ([string]($i + 1)) 635 $y $deviceSteps[$i][2])
        [void](Add-Text $s $deviceSteps[$i][0] 677 ($y + 1) 115 20 13 $C.Ink $true 1)
        [void](Add-Text $s $deviceSteps[$i][1] 677 ($y + 24) 180 18 10.5 $C.Gray $false 1)
        if ($i -lt 3) { [void](Add-Line $s 650 ($y + 31) 650 ($y + 64) $C.Line 1.5 $true) }
    }

    # 15 产品追溯
    $s = $presentation.Slides.Add(15, 12)
    Add-Header $s 15 "07 · TRACEABILITY" "亮点六：一胎一码，全链路追溯" "以轮胎二维码为索引，串联订单、物料批次、工艺工序、设备、报工与质量记录"
    [void](Add-Rect $s 67 142 244 283 $C.Navy -1 5 0)
    [void](Add-Text $s "TIRE TRACE" 95 167 190 22 11 $C.Cyan $true 2)
    for ($r = 0; $r -lt 7; $r++) {
        for ($c2 = 0; $c2 -lt 7; $c2++) {
            $fill = if ((($r * 3 + $c2 * 5) % 4) -eq 0 -or $r -lt 2 -and $c2 -lt 2 -or $r -gt 4 -and $c2 -gt 4) { $C.White } else { $C.Deep }
            [void](Add-Rect $s (104 + $c2 * 18) (210 + $r * 18) 13 13 $fill -1 1 0)
        }
    }
    [void](Add-Text $s "SN：TBR-20260716-0001" 87 365 205 20 10.5 $C.White $true 2)
    [void](Add-Pill $s "扫码即查" 124 394 128 $C.Cyan $C.Deep 10)
    $traceItems = @(
        @("订单与产品", "客户订单、产品型号、计划数量", $C.Blue),
        @("物料与工艺", "原料批次、完整路线、工作中心", $C.Green),
        @("生产与设备", "制造工单、报工数量、设备记录", $C.Orange),
        @("质量与结果", "抽检判定、返工记录、最终状态", $C.Purple)
    )
    for ($i = 0; $i -lt 4; $i++) {
        $y = 145 + $i * 78
        [void](Add-Badge $s ([string]($i + 1)) 365 $y $traceItems[$i][2])
        [void](Add-Text $s $traceItems[$i][0] 410 ($y + 1) 160 22 14 $C.Ink $true 1)
        [void](Add-Text $s $traceItems[$i][1] 410 ($y + 28) 400 21 11.5 $C.Gray $false 1)
        if ($i -lt 3) { [void](Add-Line $s 380 ($y + 31) 380 ($y + 76) $C.Line 1.5 $true) }
    }
    [void](Add-Rect $s 365 459 475 4 $C.Cyan -1 1 0)

    # 16 项目成果
    $s = $presentation.Slides.Add(16, 12)
    Add-Header $s 16 "08 · DELIVERABLES" "项目成果与过程文档" "代码、数据库、接口、测试、部署与演练资料形成完整交付链"
    $deliverables = @(
        @("前后端源码", "Vue + Java 分层项目", $C.Blue),
        @("数据库设计", "ER 图 / SQL / 迁移脚本", $C.Green),
        @("接口与权限", "API 映射 / RBAC 边界", $C.Orange),
        @("测试与部署", "构建 / 接口 / 冒烟 / 云部署", $C.Purple)
    )
    for ($i = 0; $i -lt 4; $i++) {
        $x = 62 + $i * 211
        [void](Add-Rect $s $x 132 190 82 $C.White $C.Line 5 0)
        [void](Add-Badge $s ([string]($i + 1)) ($x + 12) 146 $deliverables[$i][2])
        [void](Add-Text $s $deliverables[$i][0] ($x + 53) 143 120 22 13 $C.Ink $true 1)
        [void](Add-Text $s $deliverables[$i][1] ($x + 14) 180 160 18 9.5 $C.Gray $false 1)
    }
    $erPaths = @($assets.ErPlan, $assets.ErExec, $assets.ErQuality)
    $erNames = @("计划与工单", "生产与仓储", "质量与设备")
    for ($i = 0; $i -lt 3; $i++) {
        $x = 62 + $i * 280
        [void](Add-Rect $s $x 244 258 188 $C.White $C.Line 5 0)
        [void](Add-PictureContain $s $erPaths[$i] ($x + 8) 252 242 145 $false $C.Blue)
        [void](Add-Text $s $erNames[$i] ($x + 12) 403 234 20 11 $C.Ink $true 2)
    }
    [void](Add-Pill $s "成果可复现 · 流程可演练 · 文档可追溯" 294 457 372 $C.Navy $C.White 11)

    # 17 难点与收获
    $s = $presentation.Slides.Add(17, 12)
    Add-Header $s 17 "09 · RETROSPECTIVE" "技术难点、解决方案与项目收获" "从功能可用推进到流程正确、数据一致、权限可信与演示稳定"
    $lessons = @(
        @("工作流状态一致", "难点：跨模块状态互相依赖`n方案：Service 编排 + 状态前置校验", $C.Blue),
        @("库存口径统一", "难点：批次、库位、总量数据割裂`n方案：标准物料关联 + 事务化余额更新", $C.Green),
        @("权限与数据范围", "难点：菜单可见不等于接口可调用`n方案：前后端双校验 + 默认拒绝", $C.Orange),
        @("新旧版本融合", "难点：接口、结构、业务语义冲突`n方案：接口映射 + 分层迁移 + 回归验证", $C.Purple)
    )
    for ($i = 0; $i -lt 4; $i++) {
        $col = $i % 2
        $row = [Math]::Floor($i / 2)
        $x = 62 + $col * 426
        $y = 137 + $row * 148
        [void](Add-Card $s $x $y 390 126 $lessons[$i][0] $lessons[$i][1] $lessons[$i][2] ("0" + ($i + 1)))
    }
    [void](Add-Rect $s 62 446 816 36 $C.Navy -1 5 0)
    [void](Add-Text $s "项目收获：代码规范 · 协作流程 · 问题定位 · 数据建模 · 接口设计 · 答辩表达" 75 454 790 21 12.5 $C.White $true 2)

    # 18 结束页
    $s = $presentation.Slides.Add(18, 12)
    Add-Background $s $true
    [void](Add-Rect $s 0 0 960 540 $C.Navy -1 1 0.14)
    for ($i = 0; $i -lt 6; $i++) {
        [void](Add-Line $s (80 + $i * 150) 96 (210 + $i * 150) 444 $C.Cyan 1 $false (0.78 + $i * 0.02))
    }
    $ring = $s.Shapes.AddShape(9, 387, 84, 186, 186)
    Set-Fill $ring $C.Blue 0.66
    Set-Line $ring $C.Cyan 2 0.2
    [void](Add-PictureContain $s $assets.Logo 434 131 92 92 $false $C.Cyan)
    [void](Add-Text $s "感谢聆听" 230 310 500 62 36 $C.White $true 2)
    [void](Add-Text $s "THANK YOU · 欢迎提问" 230 378 500 28 15 $C.Cyan $true 2)
    [void](Add-Pill $s "双星轮胎 MES 制造执行系统" 342 448 276 $C.Cyan $C.Deep 11)
    [void](Add-Text $s "18 / 18" 856 501 60 16 9 $C.Sky $false 3)

    # 全局校验：不得出现四川大学文字或 15–20 页范围外页数
    foreach ($slide in $presentation.Slides) {
        for ($i = $slide.Shapes.Count; $i -ge 1; $i--) {
            $shape = $slide.Shapes.Item($i)
            try {
                if ($shape.HasTextFrame -and $shape.TextFrame.HasText) {
                    $text = $shape.TextFrame.TextRange.Text
                    if ($text -match "四川大学|SICHUAN UNIVERSITY") { $shape.Delete() }
                }
            }
            catch { }
        }
    }
    if ($presentation.Slides.Count -lt 15 -or $presentation.Slides.Count -gt 20) {
        throw "幻灯片页数不在 15–20 页范围内：$($presentation.Slides.Count)"
    }

    if (Test-Path $outputFull) { Remove-Item -LiteralPath $outputFull -Force }
    $presentation.SaveAs($outputFull, 24)
    $presentation.Export($exportFull, "PNG", 1600, 900)
    Write-Output "PPT=$outputFull"
    Write-Output "SLIDES=$($presentation.Slides.Count)"
    Write-Output "EXPORT=$exportFull"
}
finally {
    if ($presentation) { $presentation.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($presentation) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($presentation) }
    if ($ppt) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
