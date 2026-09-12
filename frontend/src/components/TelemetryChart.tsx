import { useMemo, useState } from "react";
import type { TelemetryPoint } from "../types";

interface TelemetryChartProps {
  points: TelemetryPoint[];
}

const WIDTH = 720;
const HEIGHT = 220;
const PAD = { top: 18, right: 16, bottom: 28, left: 36 };

export function TelemetryChart({ points }: TelemetryChartProps) {
  const [hover, setHover] = useState<number | null>(null);

  const geometry = useMemo(() => {
    if (points.length === 0) {
      return null;
    }
    const temps = points.map((p) => p.tempC);
    const hums = points.map((p) => p.humidity);
    const minT = Math.min(...temps, 10);
    const maxT = Math.max(...temps, 35);
    const minH = Math.min(...hums, 20);
    const maxH = Math.max(...hums, 80);
    const innerW = WIDTH - PAD.left - PAD.right;
    const innerH = HEIGHT - PAD.top - PAD.bottom;
    const xOf = (index: number) =>
      PAD.left + (points.length === 1 ? innerW / 2 : (index / (points.length - 1)) * innerW);
    const yOf = (value: number, min: number, max: number) => {
      const span = Math.max(0.1, max - min);
      return PAD.top + innerH - ((value - min) / span) * innerH;
    };
    const tempLine = points.map((p, i) => `${xOf(i).toFixed(1)},${yOf(p.tempC, minT, maxT).toFixed(1)}`).join(" ");
    const humLine = points.map((p, i) => `${xOf(i).toFixed(1)},${yOf(p.humidity, minH, maxH).toFixed(1)}`).join(" ");
    return { xOf, tempLine, humLine, yOf, minT, maxT, minH, maxH };
  }, [points]);

  if (!geometry) {
    return <div className="chart-empty">收到遥测后会在这里绘制温度与湿度曲线。</div>;
  }

  const active = hover != null ? points[hover] : points[points.length - 1];
  const activeX = hover != null ? geometry.xOf(hover) : geometry.xOf(points.length - 1);

  return (
    <div className="chart-wrap">
      <svg
        viewBox={`0 0 ${WIDTH} ${HEIGHT}`}
        className="chart"
        onMouseLeave={() => setHover(null)}
      >
        {[0, 1, 2, 3].map((row) => {
          const y = PAD.top + ((HEIGHT - PAD.top - PAD.bottom) * row) / 3;
          return <line key={row} x1={PAD.left} x2={WIDTH - PAD.right} y1={y} y2={y} className="chart-grid" />;
        })}
        <polyline points={geometry.humLine} className="chart-hum" />
        <polyline points={geometry.tempLine} className="chart-temp" />
        <line x1={activeX} x2={activeX} y1={PAD.top} y2={HEIGHT - PAD.bottom} className="chart-cursor" />
        {points.map((_, index) => (
          <rect
            key={index}
            x={geometry.xOf(index) - 8}
            y={PAD.top}
            width={16}
            height={HEIGHT - PAD.top - PAD.bottom}
            className="chart-hit"
            onMouseEnter={() => setHover(index)}
          />
        ))}
      </svg>
      <div className="chart-legend">
        <span className="legend-temp">温度 {active.tempC.toFixed(1)}°C</span>
        <span className="legend-hum">湿度 {active.humidity.toFixed(0)}%</span>
        <span className="legend-time">{new Date(active.ts).toLocaleTimeString("zh-CN")}</span>
      </div>
    </div>
  );
}
