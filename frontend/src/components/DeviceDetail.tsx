import type { Device, TelemetryPoint } from "../types";
import { LedSwitch } from "./LedSwitch";
import { MetricCard } from "./MetricCard";
import { TelemetryChart } from "./TelemetryChart";

interface DeviceDetailProps {
  device: Device;
  history: TelemetryPoint[];
  ledBusy: boolean;
  onToggleLed: () => void;
}

function formatSeen(device: Device): string {
  if (!device.lastSeen) {
    return "尚未收到心跳";
  }
  const ago = device.secondsAgo ?? 0;
  if (ago < 5) {
    return "刚刚更新";
  }
  if (ago < 60) {
    return `${ago} 秒前`;
  }
  return new Date(device.lastSeen).toLocaleString("zh-CN");
}

export function DeviceDetail({ device, history, ledBusy, onToggleLed }: DeviceDetailProps) {
  const recent = [...history].slice(-8).reverse();
  const sourceLabel =
    device.telemetrySource === "SENSOR" ? "DHT11 传感器" : device.telemetrySource === "SIMULATED" ? "固件回退 / 模拟" : "未知来源";

  return (
    <section className="detail">
      <div className="detail-head">
        <div>
          <p className="detail-kicker">{device.id}</p>
          <h2>{device.name}</h2>
        </div>
        <div className={`presence ${device.online ? "is-online" : "is-offline"}`}>
          <i />
          {device.online ? "在线" : "离线"}
          <span>{formatSeen(device)}</span>
        </div>
      </div>

      <div className="metric-grid">
        <MetricCard
          tone="temp"
          label="温度"
          value={device.temperatureC == null ? "—" : `${device.temperatureC.toFixed(1)}°C`}
          hint={sourceLabel}
        />
        <MetricCard
          tone="hum"
          label="湿度"
          value={device.humidity == null ? "—" : `${device.humidity.toFixed(0)}%`}
          hint={sourceLabel}
        />
        <MetricCard
          tone="rf"
          label="RSSI"
          value={device.rssi == null ? "—" : `${device.rssi} dBm`}
          hint="Wi-Fi 信号强度"
        />
      </div>

      <LedSwitch on={device.ledOn} busy={ledBusy} disabled={!device.online} onToggle={onToggleLed} />
      {!device.online && <p className="hint">设备离线时仍可排队显示目标状态，但 MQTT 指令不会送达。</p>}

      <div className="panel">
        <div className="panel-head">
          <h3>遥测历史</h3>
          <span>{history.length} 个采样点</span>
        </div>
        <TelemetryChart points={history} />
      </div>

      <div className="panel">
        <div className="panel-head">
          <h3>最近记录</h3>
        </div>
        {recent.length === 0 ? (
          <p className="chart-empty">暂无历史。</p>
        ) : (
          <table className="history-table">
            <thead>
              <tr>
                <th>时间</th>
                <th>温度</th>
                <th>湿度</th>
                <th>LED</th>
                <th>来源</th>
              </tr>
            </thead>
            <tbody>
              {recent.map((row, index) => (
                <tr key={`${row.ts}-${index}`}>
                  <td>{new Date(row.ts).toLocaleTimeString("zh-CN")}</td>
                  <td>{row.tempC.toFixed(1)}°C</td>
                  <td>{row.humidity.toFixed(0)}%</td>
                  <td>{row.led ? "开" : "关"}</td>
                  <td>{row.source === "SENSOR" ? "传感器" : "模拟"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </section>
  );
}
