import type { Device, Health, TelemetryPoint } from "./types";

async function parseJson<T>(response: Response): Promise<T> {
  if (!response.ok) {
    let detail = response.statusText;
    try {
      const body = (await response.json()) as { error?: string };
      if (body.error) {
        detail = body.error;
      }
    } catch {
      // keep status text
    }
    throw new Error(detail);
  }
  return response.json() as Promise<T>;
}

export function fetchDevices(): Promise<Device[]> {
  return fetch("/api/devices").then((r) => parseJson<Device[]>(r));
}

export function fetchDevice(id: string): Promise<Device> {
  return fetch(`/api/devices/${encodeURIComponent(id)}`).then((r) => parseJson<Device>(r));
}

export function fetchTelemetry(id: string, limit = 60): Promise<TelemetryPoint[]> {
  return fetch(`/api/devices/${encodeURIComponent(id)}/telemetry?limit=${limit}`).then((r) =>
    parseJson<TelemetryPoint[]>(r)
  );
}

export function fetchHealth(): Promise<Health> {
  return fetch("/api/health").then((r) => parseJson<Health>(r));
}

export function setLed(id: string, led: boolean): Promise<Device> {
  return fetch(`/api/devices/${encodeURIComponent(id)}/led`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ led }),
  }).then((r) => parseJson<Device>(r));
}
