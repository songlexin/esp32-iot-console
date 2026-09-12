import mqtt from "mqtt";

const HOST = process.env.MQTT_HOST ?? "localhost";
const PORT = Number(process.env.MQTT_PORT ?? 1883);
const USERNAME = process.env.MQTT_USERNAME || undefined;
const PASSWORD = process.env.MQTT_PASSWORD || undefined;
const INTERVAL_MS = Number(process.env.INTERVAL_MS ?? 4000);
const DEVICE_COUNT = Math.max(1, Number(process.env.DEVICE_COUNT ?? 2));

function parseIds() {
  if (process.env.DEVICE_IDS) {
    return process.env.DEVICE_IDS.split(",")
      .map((id) => id.trim())
      .filter(Boolean);
  }
  return Array.from({ length: DEVICE_COUNT }, (_, index) => {
    const n = String(index + 1).padStart(2, "0");
    return `esp32-sim-${n}`;
  });
}

function displayName(id, index) {
  if (process.env.DEVICE_NAMES) {
    const names = process.env.DEVICE_NAMES.split(",").map((name) => name.trim());
    if (names[index]) {
      return names[index];
    }
  }
  return `模拟器 ${String(index + 1).padStart(2, "0")}`;
}

class SimulatedDevice {
  constructor(id, name) {
    this.id = id;
    this.name = name;
    this.led = false;
    this.tempBase = 22 + Math.random() * 4;
    this.humBase = 50 + Math.random() * 10;
    this.rssiBase = -42 - Math.floor(Math.random() * 18);
  }

  topics() {
    return {
      status: `devices/${this.id}/status`,
      telemetry: `devices/${this.id}/telemetry`,
      command: `devices/${this.id}/command`,
    };
  }

  statusPayload(online) {
    return JSON.stringify({
      state: online ? "online" : "offline",
      name: this.name,
    });
  }

  telemetryPayload() {
    const wobble = Date.now() / 1000;
    return JSON.stringify({
      tempC: Number((this.tempBase + Math.sin(wobble / 15) * 1.4).toFixed(1)),
      humidity: Number((this.humBase + Math.cos(wobble / 12) * 4.5).toFixed(1)),
      led: this.led,
      rssi: this.rssiBase - Math.floor(Math.random() * 4),
      ts: Date.now(),
      source: "SIMULATED",
    });
  }
}

const devices = parseIds().map((id, index) => new SimulatedDevice(id, displayName(id, index)));
const url = `mqtt://${HOST}:${PORT}`;

console.log(`[simulator] connecting ${devices.length} device(s) to ${url}`);

const client = mqtt.connect(url, {
  username: USERNAME,
  password: PASSWORD,
  reconnectPeriod: 2000,
  connectTimeout: 8000,
  clean: true,
  clientId: `iot-simulator-${process.pid}`,
});

function publishBirth() {
  for (const device of devices) {
    const topics = device.topics();
    client.publish(topics.status, device.statusPayload(true), { qos: 1, retain: true });
    client.subscribe(topics.command, { qos: 1 });
    console.log(`[${device.id}] online as "${device.name}"`);
  }
}

client.on("connect", () => {
  console.log("[simulator] MQTT connected");
  publishBirth();
});

client.on("reconnect", () => {
  console.log("[simulator] MQTT reconnecting");
});

client.on("error", (error) => {
  console.error("[simulator] MQTT error:", error.message);
});

client.on("message", (topic, payload) => {
  const text = payload.toString();
  const match = topic.match(/^devices\/([^/]+)\/command$/);
  if (!match) {
    return;
  }
  const device = devices.find((item) => item.id === match[1]);
  if (!device) {
    return;
  }
  try {
    const body = JSON.parse(text);
    if (typeof body.led === "boolean") {
      device.led = body.led;
      console.log(`[${device.id}] LED -> ${device.led ? "on" : "off"}`);
      client.publish(device.topics().telemetry, device.telemetryPayload(), { qos: 0 });
    }
  } catch {
    console.log(`[${device.id}] ignored command: ${text}`);
  }
});

const timer = setInterval(() => {
  if (!client.connected) {
    return;
  }
  for (const device of devices) {
    const body = device.telemetryPayload();
    client.publish(device.topics().telemetry, body, { qos: 0 });
  }
}, INTERVAL_MS);

function shutdown() {
  clearInterval(timer);
  for (const device of devices) {
    client.publish(device.topics().status, device.statusPayload(false), { qos: 1, retain: true });
  }
  client.end(false, () => process.exit(0));
  setTimeout(() => process.exit(0), 1500);
}

process.on("SIGINT", shutdown);
process.on("SIGTERM", shutdown);
