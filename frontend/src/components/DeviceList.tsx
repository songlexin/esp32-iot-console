import type { Device } from "../types";

interface DeviceListProps {
  devices: Device[];
  selectedId: string | null;
  onSelect: (id: string) => void;
}

function formatMetric(value: number | null, digits: number, suffix: string): string {
  if (value == null) {
    return "—";
  }
  return `${value.toFixed(digits)}${suffix}`;
}

export function DeviceList({ devices, selectedId, onSelect }: DeviceListProps) {
  return (
    <aside className="sidebar">
      <div className="sidebar-head">
        <h2>设备</h2>
        <span>{devices.length} 台</span>
      </div>
      {devices.length === 0 ? (
        <p className="sidebar-empty">还没有设备报到。启动模拟器或烧录固件后会自动出现。</p>
      ) : (
        <ul className="device-list">
          {devices.map((device) => (
            <li key={device.id}>
              <button
                type="button"
                className={`device-item ${selectedId === device.id ? "is-active" : ""}`}
                onClick={() => onSelect(device.id)}
              >
                <span className={`status-orb ${device.online ? "is-online" : "is-offline"}`} />
                <span className="device-item-body">
                  <strong>{device.name}</strong>
                  <small>{device.id}</small>
                  <em>
                    {formatMetric(device.temperatureC, 1, "°C")} · {formatMetric(device.humidity, 0, "%")}
                    {device.ledOn ? " · LED 开" : ""}
                  </em>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
    </aside>
  );
}
