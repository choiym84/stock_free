import { useEffect, useRef } from 'react'
import { CandlestickSeries, ColorType, createChart, type CandlestickData, type IChartApi } from 'lightweight-charts'

type MarketChartProps = {
  data: CandlestickData[]
  height?: number
}

/** A small, data-source-agnostic chart surface for minute OHLC candles. */
export function MarketChart({ data, height = 360 }: MarketChartProps) {
  const containerRef = useRef<HTMLDivElement>(null)
  const chartRef = useRef<IChartApi | null>(null)

  useEffect(() => {
    if (!containerRef.current) return

    const chart = createChart(containerRef.current, {
      height,
      layout: { background: { type: ColorType.Solid, color: 'transparent' }, textColor: '#849089' },
      grid: { vertLines: { color: '#edf0ed' }, horzLines: { color: '#edf0ed' } },
      rightPriceScale: { borderColor: '#d9ded9' },
      timeScale: { borderColor: '#d9ded9', timeVisible: true },
    })
    const series = chart.addSeries(CandlestickSeries, {
      upColor: '#3d8c68', downColor: '#c9685d', borderVisible: false,
      wickUpColor: '#3d8c68', wickDownColor: '#c9685d',
    })
    series.setData(data)
    chart.timeScale().fitContent()
    chartRef.current = chart

    const resizeObserver = new ResizeObserver(() => chart.applyOptions({ width: containerRef.current?.clientWidth ?? 0 }))
    resizeObserver.observe(containerRef.current)
    return () => { resizeObserver.disconnect(); chart.remove(); chartRef.current = null }
  }, [data, height])

  return <div ref={containerRef} aria-label="주가 캔들 차트" />
}
