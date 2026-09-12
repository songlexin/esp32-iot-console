export type TelemetrySource = "SENSOR" | "SIMULATED";

export interface Device {
  id: string;
  name: string;
  online: boolean;
  lastSeen: string | null;
  ledOn: boolean;
  temperatureC: number | null;
  humidity: number | null;
  rssi: number | null;
  telemetrySource: TelemetrySource | null;
  secondsAgo: number | null;
}

export interface TelemetryPoint {
  tempC: number;
  humidity: number;
  led: boolean;
  rssi: number | null;
  source: TelemetrySource;
  ts: string;
}

export interface Health {
  status: string;
  mqtt: "CONNECTED" | "DISCONNECTED" | string;
  devices: number;
  online: number;
}

export interface LiveDeviceEvent {
  device: Device;
}

export interface LiveTelemetryEvent {
  deviceId: string;
  sample: TelemetryPoint;
  device: Device;
}
