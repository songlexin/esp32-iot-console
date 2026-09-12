interface EmptyStateProps {
  backendDown: boolean;
  onRetry: () => void;
}

export function EmptyState({ backendDown, onRetry }: EmptyStateProps) {
  return (
    <section className="empty">
      <div className="empty-card">
        <p className="detail-kicker">等待设备</p>
        <h2>{backendDown ? "后端还没有响应" : "控制台已就绪，还没有 ESP32 报到"}</h2>
        <p>
          {backendDown
            ? "确认 Java 服务已在 8080 端口启动，然后点击重试。"
            : "在另一个终端运行设备模拟器，或把固件烧到开发板。设备会通过 MQTT 自动注册。"}
        </p>
        <ol>
          <li>
            <code>cd simulator && npm start</code>
          </li>
          <li>
            或 <code>docker compose up --build</code>（已包含模拟器）
          </li>
        </ol>
        <button type="button" className="retry" onClick={onRetry}>
          重新检测
        </button>
      </div>
    </section>
  );
}
