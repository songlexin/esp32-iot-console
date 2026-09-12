interface MetricCardProps {
  label: string;
  value: string;
  hint: string;
  tone: "temp" | "hum" | "rf";
}

export function MetricCard({ label, value, hint, tone }: MetricCardProps) {
  return (
    <article className={`metric metric-${tone}`}>
      <p>{label}</p>
      <strong>{value}</strong>
      <small>{hint}</small>
    </article>
  );
}
