param(
    [Parameter(Mandatory = $true)][string]$RequirementsPath,
    [Parameter(Mandatory = $true)][string]$TemplatePath,
    [Parameter(Mandatory = $true)][string]$OutputDir
)

<#
  PowerPoint 结构清点工具。
  通过 PowerPoint COM 只读打开需求文档和模板，输出每页/母版/自定义版式的 Shape 文本与坐标，
  同时导出 1600x900 PNG。输入文件不修改，输出为 UTF-8 inventory.txt 和图片目录。
#>
$ErrorActionPreference = 'Stop'
$output = [IO.Path]::GetFullPath($OutputDir)
New-Item -ItemType Directory -Path $output -Force | Out-Null

# 安全读取 Shape 文本并把换行压平；无文本或 COM 属性访问失败时返回空串。
function Shape-Text($shape) {
    try {
        if ($shape.HasTextFrame -and $shape.TextFrame.HasText) {
            $text = [string]$shape.TextFrame.TextRange.Text
            $text = $text.Replace("`r", ' ').Replace("`n", ' ')
            return $text.Trim()
        }
    }
    catch {
        return ''
    }
    return ''
}

# 清点一份 PPT 的页面尺寸、所有页/母版/自定义版式 Shape，并导出文本清单和 PNG。
function Inspect-Presentation($app, $path, $name) {
    $presentation = $app.Presentations.Open($path, $true, $true, $false)
    try {
        $lines = [Collections.Generic.List[string]]::new()
        $lines.Add("FILE=$path")
        $lines.Add("SLIDES=$($presentation.Slides.Count)")
        $lines.Add("SIZE=$($presentation.PageSetup.SlideWidth)x$($presentation.PageSetup.SlideHeight)")
        $lines.Add('')

        foreach ($slide in $presentation.Slides) {
            $lines.Add("=== SLIDE $($slide.SlideIndex) | layout=$($slide.CustomLayout.Name) ===")
            foreach ($shape in $slide.Shapes) {
                $text = Shape-Text $shape
                $lines.Add(("shape={0} type={1} x={2:N1} y={3:N1} w={4:N1} h={5:N1} text={6}" -f
                    $shape.Name, $shape.Type, $shape.Left, $shape.Top, $shape.Width, $shape.Height, $text))
            }
            $lines.Add('')
        }

        $lines.Add('=== MASTER SHAPES ===')
        foreach ($shape in $presentation.SlideMaster.Shapes) {
            $lines.Add(("shape={0} type={1} x={2:N1} y={3:N1} w={4:N1} h={5:N1} text={6}" -f
                $shape.Name, $shape.Type, $shape.Left, $shape.Top, $shape.Width, $shape.Height, (Shape-Text $shape)))
        }

        $lines.Add('=== CUSTOM LAYOUTS ===')
        foreach ($layout in $presentation.SlideMaster.CustomLayouts) {
            $lines.Add("-- layout=$($layout.Name) index=$($layout.Index)")
            foreach ($shape in $layout.Shapes) {
                $lines.Add(("shape={0} type={1} x={2:N1} y={3:N1} w={4:N1} h={5:N1} text={6}" -f
                    $shape.Name, $shape.Type, $shape.Left, $shape.Top, $shape.Width, $shape.Height, (Shape-Text $shape)))
            }
        }

        [IO.File]::WriteAllLines((Join-Path $output "$name-inventory.txt"), $lines, [Text.UTF8Encoding]::new($false))
        $images = Join-Path $output "$name-images"
        New-Item -ItemType Directory -Path $images -Force | Out-Null
        $presentation.Export($images, 'PNG', 1600, 900)
    }
    finally {
        $presentation.Close()
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($presentation)
    }
}

# 一个 PowerPoint 进程连续处理两份输入，最终统一退出并释放 COM。
$powerPoint = New-Object -ComObject PowerPoint.Application
try {
    Inspect-Presentation $powerPoint $RequirementsPath 'requirements'
    Inspect-Presentation $powerPoint $TemplatePath 'template'
}
finally {
    $powerPoint.Quit()
    [void][Runtime.InteropServices.Marshal]::ReleaseComObject($powerPoint)
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
