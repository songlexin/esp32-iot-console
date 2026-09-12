import type { Health } from "../types";

interface HeaderProps {
  health: Health | null;
  live: boolean;
  deviceCount: number;
}

export function Header({ health, live, deviceCount }: HeaderProps) {
  const mqttUp = health?.mqtt === "CONNECTED";
  return (
    <header className="topbar">
      <div className="brand">
        <span className="brand-mark" aria-hidden="true">
          <span className="brand-chip" />
        </span>
        <div>
          <p className="brand-kicker">ESP32 · MQTT · Java</p>
          <h1>物联控制台</h1>
        </div>
      </div>
      <div className="top-pills">
        <span className={`pill ${live ? "pill-live" : "pill-warn"}`}>
          <i />
          {live ? "实时通道已连接" : "实时通道重连中"}
        </span>
        <span className={`pill ${mqttUp ? "pill-live" : "pill-warn"}`}>
          <i />
          MQTT {mqttUp ? "已连接" : "未连接"}
        </span>
        <span className="pill">
          在线 {health?.online ?? 0}/{health?.devices ?? deviceCount}
        </span>
      </div>
    </header>
  );
}
