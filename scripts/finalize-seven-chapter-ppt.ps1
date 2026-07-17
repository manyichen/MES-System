param(
    [string]$PresentationPath = (Join-Path $PSScriptRoot "..\docs\双星轮胎MES制造执行系统-项目答辩-七章充实版.pptx"),
    [string]$CheckDir = (Join-Path $PSScriptRoot "..\tmp\ppt-seven-final-check")
)

$ErrorActionPreference = "Stop"
$path = [System.IO.Path]::GetFullPath($PresentationPath)
$check = [System.IO.Path]::GetFullPath($CheckDir)

function Replace-ExactText($slide, [string]$oldText, [string]$newText) {
    foreach ($shape in $slide.Shapes) {
        try {
            if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.TextFrame.TextRange.Text -eq $oldText) {
                $shape.TextFrame.TextRange.Text = $newText
                return $true
            }
        }
        catch { }
    }
    return $false
}

$ppt = New-Object -ComObject PowerPoint.Application
$ppt.Visible = -1
$p = $ppt.Presentations.Open($path, 0, 0, 0)

try {
    if ($p.Slides.Count -ne 20) { throw "最终稿应为 20 页，实际为 $($p.Slides.Count) 页。" }

    foreach ($shape in $p.Slides.Item(3).Shapes) {
        try {
            if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.TextFrame.TextRange.Text -match "^项目定位") {
                $shape.TextFrame.TextRange.Text = "项目定位"
            }
        }
        catch { }
    }
    [void](Replace-ExactText $p.Slides.Item(9) "亮点六：一胎一码，全链路追溯" "亮点三：一胎一码，全链路追溯")

    foreach ($shape in $p.Slides.Item(19).Shapes) {
        try {
            if (-not ($shape.HasTextFrame -and $shape.TextFrame.HasText)) { continue }
            $text = $shape.TextFrame.TextRange.Text
            if ($text -match "跨模块状态互相依赖") {
                $shape.TextFrame.TextRange.Text = "Controller / Service / DAO 分层`r统一命名、响应结构与代码审查规则"
            }
            elseif ($text -match "批次、库位、总量数据割裂") {
                $shape.TextFrame.TextRange.Text = "需求确认 → 接口映射 → 分层实现`r联调测试 → 业务验收 → 文档归档"
            }
            elseif ($text -match "菜单可见不等于接口可调用") {
                $shape.TextFrame.TextRange.Text = "状态机、库存一致性与权限数据范围`r通过事务、前置校验和默认拒绝解决"
            }
            elseif ($text -match "接口、结构、业务语义冲突") {
                $shape.TextFrame.TextRange.Text = "BusinessException + 统一错误码`r示例：INVENTORY_NOT_ENOUGH（库存不足）"
            }
        }
        catch { }
    }

    [void](Replace-ExactText $p.Slides.Item(20) "18 / 18" "20 / 20")

    foreach ($slide in $p.Slides) {
        foreach ($shape in $slide.Shapes) {
            try {
                if ($shape.HasTextFrame -and $shape.TextFrame.HasText -and $shape.TextFrame.TextRange.Text -match "四川大学|SICHUAN UNIVERSITY") {
                    throw "仍检测到四川大学标识文字，页码：$($slide.SlideIndex)"
                }
            }
            catch {
                if ($_.Exception.Message -match "仍检测到") { throw }
            }
        }
    }

    $p.Save()
    if (Test-Path $check) { Remove-Item -LiteralPath $check -Recurse -Force }
    [System.IO.Directory]::CreateDirectory($check) | Out-Null
    foreach ($n in 3, 9, 19, 20) {
        $p.Slides.Item($n).Export((Join-Path $check ("slide-{0:D2}.png" -f $n)), "PNG", 1600, 900)
    }
    Write-Output "PPT=$path"
    Write-Output "SLIDES=$($p.Slides.Count)"
    Write-Output "CHECK=$check"
}
finally {
    if ($p) { $p.Close() }
    if ($ppt) { $ppt.Quit() }
    if ($p) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($p) }
    if ($ppt) { [void][System.Runtime.InteropServices.Marshal]::ReleaseComObject($ppt) }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
