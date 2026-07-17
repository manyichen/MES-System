param(
    [Parameter(Mandatory = $true)][string]$RequirementsPath,
    [Parameter(Mandatory = $true)][string]$TemplatePath,
    [Parameter(Mandatory = $true)][string]$OutputDir
)

$ErrorActionPreference = 'Stop'
$output = [IO.Path]::GetFullPath($OutputDir)
New-Item -ItemType Directory -Path $output -Force | Out-Null

function Shape-Text($shape) {
    try {
        if ($shape.HasTextFrame -and $shape.TextFrame.HasText) {
            return ($shape.TextFrame.TextRange.Text -replace "`r", ' ' -replace "`n", ' ').Trim()
        }
    } catch {}
    return ''
}

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
