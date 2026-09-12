interface LedSwitchProps {
  on: boolean;
  disabled?: boolean;
  busy?: boolean;
  onToggle: () => void;
}

export function LedSwitch({ on, disabled, busy, onToggle }: LedSwitchProps) {
  return (
    <button
      type="button"
      className={`led-switch ${on ? "is-on" : ""}`}
      onClick={onToggle}
      disabled={disabled || busy}
      aria-pressed={on}
    >
      <span className="led-lamp" />
      <span className="led-copy">
        <strong>板载 LED</strong>
        <small>{busy ? "指令发送中…" : on ? "已点亮 · GPIO 2" : "已熄灭 · GPIO 2"}</small>
      </span>
      <span className="led-track">
        <span className="led-knob" />
      </span>
    </button>
  );
}
