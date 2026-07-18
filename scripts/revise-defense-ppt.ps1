param(
    [string]$PresentationPath = (Join-Path $PSScriptRoot "..\docs\双星轮胎MES制造执行系统-项目答辩.pptx"),
    [string]$ExportDir = (Join-Path $PSScriptRoot "..\tmp\ppt-revised-output")
)

<#
  答辩 PPT 二次修订器。
  外部依赖为 Microsoft PowerPoint COM。脚本打开现有 PPTX，按页查找/重建指定内容，
  插入最新系统截图，保存后导出 PNG 检查。输入文件会原地更新，finally 负责释放 COM。
#>
$ErrorActionPreference = "Stop"

# PowerPoint 使用 BGR 整数，统一转换避免直接书写难读颜色常量。
function Color([int]$r, [int]$g, [int]$b) { $r + ($g * 256) + ($b * 65536) }

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

# 设置形状填充。
function Set-Fill($shape, [int]$color, [double]$transparency = 0) {
    $shape.Fill.Visible = -1
    $shape.Fill.Solid()
    $shape.Fill.ForeColor.RGB = $color
    $shape.Fill.Transparency = $transparency
}

# 设置形状边框。
function Set-Line($shape, [int]$color, [double]$weight = 1, [double]$transparency = 0) {
    $shape.Line.Visible = -1
    $shape.Line.ForeColor.RGB = $color
    $shape.Line.Weight = $weight
    $shape.Line.Transparency = $transparency
}

# 添加可配置矩形，作为修订卡片和背景的基础。
function Add-Rect($slide, [double]$x, [double]$y, [double]$w, [double]$h,
                  [int]$fill, [int]$line = -1, [int]$shapeType = 1, [double]$transparency = 0) {
    $s = $slide.Shapes.AddShape($shapeType, $x, $y, $w, $h)
    Set-Fill $s $fill $transparency
    if ($line -ge 0) { Set-Line $s $line 1 } else { $s.Line.Visible = 0 }
    $s
}

# 添加并格式化文本框。
function Add-Text($slide, [string]$text, [double]$x, [double]$y, [double]$w, [double]$h,
                  [double]$size = 18, [int]$color = 0, [bool]$bold = $false,
                  [int]$align = 1, [double]$margin = 0) {
    $s = $slide.Shapes.AddTextbox(1, $x, $y, $w, $h)
    $s.TextFrame2.MarginLeft = $margin
    $s.TextFrame2.MarginRight = $margin
    $s.TextFrame2.MarginTop = $margin
    $s.TextFrame2.MarginBottom = $margin
    $s.TextFrame2.WordWrap = -1
    $s.TextFrame2.AutoSize = 0
    $r = $s.TextFrame2.TextRange
    $r.Text = $text
    $r.Font.Name = "Microsoft YaHei"
    $r.Font.NameFarEast = "微软雅黑"
    $r.Font.Size = $size
    $r.Font.Fill.ForeColor.RGB = $color
    $r.Font.Bold = if ($bold) { -1 } else { 0 }
    $r.ParagraphFormat.Alignment = $align
    $r.ParagraphFormat.SpaceAfter = 0
    $s
}

# 添加普通线或箭头连接线。
function Add-Line($slide, [double]$x1, [double]$y1, [double]$x2, [double]$y2,
                  [int]$color, [double]$weight = 1.5, [bool]$arrow = $false, [double]$transparency = 0) {
    $s = $slide.Shapes.AddLine($x1, $y1, $x2, $y2)
    Set-Line $s $color $weight $transparency
    if ($arrow) { $s.Line.EndArrowheadStyle = 3 }
    $s
}

# 添加胶囊标签。
function Add-Pill($slide, [string]$text, [double]$x, [double]$y, [double]$w,
                  [int]$fill, [int]$textColor, [double]$size = 11) {
    [void](Add-Rect $slide $x $y $w 25 $fill -1 5 0)
    [void](Add-Text $slide $text $x ($y + 3) $w 18 $size $textColor $true 2)
}

# 添加编号徽章。
function Add-Badge($slide, [string]$text, [double]$x, [double]$y, [int]$fill) {
    [void](Add-Rect $slide $x $y 32 32 $fill -1 9 0)
    [void](Add-Text $slide $text $x ($y + 5) 32 20 12 $C.White $true 2)
}

# 重绘修订页的统一浅色背景与装饰。
function Add-Background($slide) {
    $slide.Background.Fill.Visible = -1
    $slide.Background.Fill.Solid()
    $slide.Background.Fill.ForeColor.RGB = $C.Pale
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

# 重绘标准页眉、页码与页脚。
function Add-Header($slide, [int]$number, [string]$section, [string]$title, [string]$subtitle) {
    Add-Background $slide
    [void](Add-Rect $slide 48 28 8 34 $C.Blue -1 1 0)
    [void](Add-Text $slide $section 72 25 360 18 10.5 $C.Blue $true 1)
    [void](Add-Text $slide $title 72 43 690 40 27 $C.Navy $true 1)
    [void](Add-Text $slide $subtitle 72 81 720 23 11.5 $C.Gray $false 1)
    [void](Add-Line $slide 72 108 910 108 $C.Line 1.2)
    Add-Pill $slide ("{0:D2}" -f $number) 866 30 44 $C.Blue $C.White 11
    [void](Add-Text $slide "双星轮胎 MES · 项目答辩" 690 514 220 16 8.5 $C.Gray $false 3)
}

# 将截图等比缩放到目标区域，保持原始比例。
function Add-PictureContain($slide, [string]$path, [double]$x, [double]$y, [double]$w, [double]$h) {
    Add-Type -AssemblyName System.Drawing
    $img = [System.Drawing.Image]::FromFile($path)
    try {
        $ratio = [Math]::Min($w / $img.Width, $h / $img.Height)
        $pw = $img.Width * $ratio
        $ph = $img.Height * $ratio
        $px = $x + (($w - $pw) / 2)
        $py = $y + (($h - $ph) / 2)
        $slide.Shapes.AddPicture($path, 0, -1, $px, $py, $pw, $ph)
    }
    finally { $img.Dispose() }
}

# 遍历当前页文本框，返回第一个包含目标文字的 Shape，避免依赖脆弱的 Shape 索引。
function Find-TextShape($slide, [string]$text) {
    foreach ($shape in $slide.Shapes) {
        try {
            if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.TextFrame.TextRange.Text -eq $text) {
                return $shape
            }
        }
        catch { }
    }
    return $null
}

$root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$path = [System.IO.Path]::GetFullPath($PresentationPath)
$export = [System.IO.Path]::GetFullPath($ExportDir)
$dashboard = Join-Path $root "backend\target\vue-dashboard-desktop.png"

# 打开现有演示文稿；finally 中关闭并释放 COM。
$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$p = $ppt.Presentations.Open($path, 0, 0, 0)

try {
    if ($p.Slides.Count -ne 18) { throw "预期基础 PPT 为 18 页，实际为 $($p.Slides.Count) 页。" }

    # 删除原第 3 页“项目概述”，将其内容并入项目背景页，给目录、用例图和原型设计腾出页数。
    $p.Slides.Item(3).Delete()

    # 新增目录页（封面之后）。
    $toc = $p.Slides.Add(2, 12)
    Add-Background $toc
    [void](Add-Rect $toc 48 28 8 34 $C.Blue -1 1 0)
    [void](Add-Text $toc "CONTENTS" 72 25 240 18 10.5 $C.Blue $true 1)
    [void](Add-Text $toc "目录 · 七大章节" 72 43 560 40 27 $C.Navy $true 1)
    [void](Add-Text $toc "严格对应答辩要求 Part2–Part8，内容覆盖技术、流程、界面、成果与复盘" 72 81 720 23 11.5 $C.Gray $false 1)
    [void](Add-Line $toc 72 108 910 108 $C.Line 1.2)
    Add-Pill $toc "02" 866 30 44 $C.Blue $C.White 11
    [void](Add-Text $toc "双星轮胎 MES · 项目答辩" 690 514 220 16 8.5 $C.Gray $false 3)

    $chapters = @(
        @("01", "项目介绍与使用技术", "项目定位、总体方案、技术架构", "03–05", $C.Blue),
        @("02", "人员分工与开发计划", "三人协作、里程碑与交付节奏", "06", $C.Green),
        @("03", "项目亮点", "经营驾驶舱、齐套闭环、一胎一码", "07–09", $C.Orange),
        @("04", "需求分析、用例与原型", "角色需求、用例图、业务流程、原型规范", "10–13", $C.Purple),
        @("05", "功能结构与核心界面", "功能模块、生产、仓储、工艺设备", "14–17", $C.Cyan),
        @("06", "项目成果与过程文档", "源码、数据库、接口、测试和部署", "18", $C.Blue),
        @("07", "项目心得与收获", "代码规范、开发流程、难点和异常处理", "19", $C.Green)
    )
    for ($i = 0; $i -lt 7; $i++) {
        $col = $i % 2
        $row = [Math]::Floor($i / 2)
        $x = 66 + $col * 424
        $y = 132 + $row * 86
        $w = if ($i -eq 6) { 836 } else { 392 }
        [void](Add-Rect $toc $x $y $w 70 $C.White $C.Line 5 0)
        [void](Add-Rect $toc $x $y 58 70 $chapters[$i][4] -1 5 0)
        [void](Add-Text $toc $chapters[$i][0] ($x + 8) ($y + 20) 42 24 14 $C.White $true 2)
        [void](Add-Text $toc $chapters[$i][1] ($x + 76) ($y + 11) ($w - 150) 24 14.5 $C.Ink $true 1)
        [void](Add-Text $toc $chapters[$i][2] ($x + 76) ($y + 38) ($w - 150) 19 10 $C.Gray $false 1)
        [void](Add-Pill $toc $chapters[$i][3] ($x + $w - 64) ($y + 22) 48 $C.Pale $chapters[$i][4] 9.5)
    }

    # 此时基础页顺序：封面、目录、背景、方案、技术、团队、需求、流程、功能、驾驶舱、齐套、生产、仓储、工艺、追溯、成果、心得、致谢。
    # 新增 UML 风格用例图页。
    $usecase = $p.Slides.Add($p.Slides.Count + 1, 12)
    Add-Header $usecase 11 "CHAPTER 04 · 需求分析、用例与原型" "系统用例图" "以超级管理员完整演示为主线，同时保留计划、生产、仓储、质量与设备岗位边界"
    [void](Add-Rect $usecase 258 132 548 332 $C.White $C.Line 5 0)
    [void](Add-Text $usecase "双星轮胎 MES 系统边界" 420 142 220 22 13 $C.Blue $true 2)

    $actors = @(
        @("超级管理员", 68, 151, $C.Navy),
        @("计划 / 车间", 68, 248, $C.Blue),
        @("仓储 / 质量", 68, 345, $C.Green),
        @("设备 / 管理层", 820, 248, $C.Orange)
    )
    foreach ($a in $actors) {
        [void](Add-Rect $usecase $a[1] $a[2] 118 54 $C.White $C.Line 5 0)
        [void](Add-Rect $usecase $a[1] $a[2] 5 54 $a[3] -1 1 0)
        [void](Add-Text $usecase $a[0] ($a[1] + 12) ($a[2] + 17) 96 21 11.5 $C.Ink $true 2)
    }

    $cases = @(
        @("订单与生产任务", 302, 184, $C.Blue), @("齐套与工单派发", 548, 184, $C.Blue),
        @("接单、领料与报工", 302, 252, $C.Green), @("拣货、配送与入库", 548, 252, $C.Green),
        @("质量抽检与返工", 302, 320, $C.Orange), @("设备报修与维护", 548, 320, $C.Orange),
        @("产品追溯与驾驶舱", 302, 388, $C.Purple), @("用户权限与操作审计", 548, 388, $C.Purple)
    )
    foreach ($uc in $cases) {
        $ellipse = Add-Rect $usecase $uc[1] $uc[2] 206 42 $C.Pale $uc[3] 9 0
        $ellipse.Line.Weight = 1.3
        [void](Add-Text $usecase $uc[0] ($uc[1] + 8) ($uc[2] + 10) 190 20 11.5 $C.Ink $true 2)
    }
    [void](Add-Line $usecase 186 178 302 205 $C.Line 1.2 $false)
    [void](Add-Line $usecase 186 178 548 205 $C.Line 1.2 $false)
    [void](Add-Line $usecase 186 275 302 273 $C.Line 1.2 $false)
    [void](Add-Line $usecase 186 275 548 273 $C.Line 1.2 $false)
    [void](Add-Line $usecase 186 372 302 341 $C.Line 1.2 $false)
    [void](Add-Line $usecase 186 372 302 409 $C.Line 1.2 $false)
    [void](Add-Line $usecase 820 275 754 341 $C.Line 1.2 $false)
    [void](Add-Line $usecase 820 275 754 409 $C.Line 1.2 $false)
    Add-Pill $usecase "超级管理员：可发起、接收、执行、审核和追踪全部演示流程" 292 475 480 $C.Navy $C.White 10.5

    # 新增原型设计页。
    $prototype = $p.Slides.Add($p.Slides.Count + 1, 12)
    Add-Header $prototype 13 "CHAPTER 04 · 需求分析、用例与原型" "原型设计与交互规范" "采用统一导航、角色工作台、状态驱动操作和可复用业务组件，降低学习成本"
    [void](Add-Rect $prototype 58 132 568 330 $C.White $C.Line 5 0)
    [void](Add-PictureContain $prototype $dashboard 66 140 552 314)
    $prototypeCards = @(
        @("01", "统一信息架构", "左侧模块导航 + 顶部状态栏 + 主工作区，页面层级保持一致。", $C.Blue),
        @("02", "状态驱动交互", "按钮随权限与业务状态动态出现，避免无效操作和越权调用。", $C.Green),
        @("03", "组件复用与适配", "统一表格、搜索、弹窗、状态标签，并兼顾桌面和移动端。", $C.Orange)
    )
    for ($i = 0; $i -lt 3; $i++) {
        $y = 132 + $i * 110
        [void](Add-Rect $prototype 650 $y 250 94 $C.White $C.Line 5 0)
        [void](Add-Rect $prototype 650 $y 5 94 $prototypeCards[$i][3] -1 1 0)
        Add-Pill $prototype $prototypeCards[$i][0] 668 ($y + 12) 48 $C.Pale $prototypeCards[$i][3] 9.5
        [void](Add-Text $prototype $prototypeCards[$i][1] 727 ($y + 14) 155 22 14 $C.Ink $true 1)
        [void](Add-Text $prototype $prototypeCards[$i][2] 668 ($y + 48) 214 37 10.5 $C.Gray $false 1)
    }
    Add-Pill $prototype "原型 → Vue 组件 → 接口联调 → 浏览器冒烟 → 业务验收" 220 475 520 $C.Navy $C.White 10.5

    # 保存所有现有页对象引用，再按 7 章顺序重新编排。
    $cover = $p.Slides.Item(1)
    $background = $p.Slides.Item(3)
    $solution = $p.Slides.Item(4)
    $technology = $p.Slides.Item(5)
    $team = $p.Slides.Item(6)
    $requirements = $p.Slides.Item(7)
    $businessFlow = $p.Slides.Item(8)
    $functionMap = $p.Slides.Item(9)
    $cockpit = $p.Slides.Item(10)
    $kitting = $p.Slides.Item(11)
    $production = $p.Slides.Item(12)
    $warehouse = $p.Slides.Item(13)
    $process = $p.Slides.Item(14)
    $trace = $p.Slides.Item(15)
    $results = $p.Slides.Item(16)
    $retrospective = $p.Slides.Item(17)
    $thanks = $p.Slides.Item(18)

    $desired = @(
        $cover, $toc,
        $background, $solution, $technology,
        $team,
        $cockpit, $kitting, $trace,
        $requirements, $usecase, $businessFlow, $prototype,
        $functionMap, $production, $warehouse, $process,
        $results, $retrospective, $thanks
    )
    for ($i = 0; $i -lt $desired.Count; $i++) { $desired[$i].MoveTo($i + 1) }

    # 章节名称、页码与重点文案统一。
    $chapterRanges = @(
        @(3, 5, "CHAPTER 01 · 项目介绍与使用技术"),
        @(6, 6, "CHAPTER 02 · 人员分工与开发计划"),
        @(7, 9, "CHAPTER 03 · 项目亮点"),
        @(10, 13, "CHAPTER 04 · 需求分析、用例与原型"),
        @(14, 17, "CHAPTER 05 · 功能结构与核心界面"),
        @(18, 18, "CHAPTER 06 · 项目成果与过程文档"),
        @(19, 19, "CHAPTER 07 · 项目心得与收获")
    )
    foreach ($range in $chapterRanges) {
        for ($n = $range[0]; $n -le $range[1]; $n++) {
            foreach ($shape in $p.Slides.Item($n).Shapes) {
                try {
                    if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.Left -ge 60 -and $shape.Left -le 90 -and $shape.Top -le 35) {
                        $shape.TextFrame.TextRange.Text = $range[2]
                        $shape.TextFrame.TextRange.Font.Name = "Microsoft YaHei"
                        $shape.TextFrame.TextRange.Font.NameFarEast = "微软雅黑"
                        $shape.TextFrame.TextRange.Font.Size = 10.5
                        $shape.TextFrame.TextRange.Font.Bold = -1
                        $shape.TextFrame.TextRange.Font.Color.RGB = $C.Blue
                    }
                }
                catch { }
            }
        }
    }

    for ($n = 2; $n -le 19; $n++) {
        foreach ($shape in $p.Slides.Item($n).Shapes) {
            try {
                if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.Left -gt 850 -and $shape.Top -lt 60) {
                    $shape.TextFrame.TextRange.Text = "{0:D2}" -f $n
                }
            }
            catch { }
        }
    }

    $title = Find-TextShape $p.Slides.Item(3) "项目背景与行业痛点"
    if ($title) { $title.TextFrame.TextRange.Text = "项目背景与项目概述" }
    $sub = Find-TextShape $p.Slides.Item(3) "轮胎制造链条长、角色多、状态复杂，信息断点会直接放大交付与质量风险"
    if ($sub) { $sub.TextFrame.TextRange.Text = "面向双星轮胎制造场景，建设贯穿计划、执行、仓储、质量、设备和追溯的一体化 MES" }
    $opp = Find-TextShape $p.Slides.Item(3) "项目机会"
    if ($opp) { $opp.TextFrame.TextRange.Text = "项目定位 · 12 个业务模块 · 22 个流程节点 · 统一数据底座" }

    $retroTitle = Find-TextShape $p.Slides.Item(19) "技术难点、解决方案与项目收获"
    if ($retroTitle) { $retroTitle.TextFrame.TextRange.Text = "项目心得与收获：规范、难点与异常处理" }
    $replaceTexts = @{
        "工作流状态一致" = "代码规范"
        "难点：跨模块状态互相依赖`r方案：Service 编排 + 状态前置校验" = "Controller / Service / DAO 分层`r统一命名、响应结构与代码审查规则"
        "库存口径统一" = "开发流程规范"
        "难点：批次、库位、总量数据割裂`r方案：标准物料关联 + 事务化余额更新" = "需求确认 → 接口映射 → 分层实现`r联调测试 → 业务验收 → 文档归档"
        "权限与数据范围" = "技术难点解决"
        "难点：菜单可见不等于接口可调用`r方案：前后端双校验 + 默认拒绝" = "状态机、库存一致性与权限数据范围`r通过事务、前置校验和默认拒绝解决"
        "新旧版本融合" = "异常代码分享"
        "难点：接口、结构、业务语义冲突`r方案：接口映射 + 分层迁移 + 回归验证" = "BusinessException + 统一错误码`r示例：INVENTORY_NOT_ENOUGH 库存不足"
    }
    foreach ($shape in $p.Slides.Item(19).Shapes) {
        try {
            if ($shape.HasTextFrame -and $shape.TextFrame.HasText) {
                $old = $shape.TextFrame.TextRange.Text
                if ($replaceTexts.ContainsKey($old)) { $shape.TextFrame.TextRange.Text = $replaceTexts[$old] }
            }
        }
        catch { }
    }

    # 删除任何四川大学字样，最终页数必须为 20。
    foreach ($slide in $p.Slides) {
        for ($i = $slide.Shapes.Count; $i -ge 1; $i--) {
            $shape = $slide.Shapes.Item($i)
            try {
                if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.TextFrame.TextRange.Text -match "四川大学|SICHUAN UNIVERSITY") {
                    $shape.Delete()
                }
            }
            catch { }
        }
    }
    if ($p.Slides.Count -ne 20) { throw "最终 PPT 应为 20 页，实际为 $($p.Slides.Count) 页。" }

    $p.Save()
    if (Test-Path $export) { Remove-Item -LiteralPath $export -Recurse -Force }
    [System.IO.Directory]::CreateDirectory($export) | Out-Null
    $p.Export($export, "PNG", 1600, 900)
    Write-Output "PPT=$path"
    Write-Output "SLIDES=$($p.Slides.Count)"
    Write-Output "EXPORT=$export"
}
finally {
    if ($p) { $p.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($p) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($p) }
    if ($ppt) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
