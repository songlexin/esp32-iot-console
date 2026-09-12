import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { fetchDevices, fetchHealth, fetchTelemetry, setLed } from "../api";
import type { Device, Health, LiveDeviceEvent, LiveTelemetryEvent, TelemetryPoint } from "../types";

const HISTORY_LIMIT = 60;

function upsertDevice(list: Device[], incoming: Device): Device[] {
  const next = list.filter((item) => item.id !== incoming.id);
  next.push(incoming);
  next.sort((a, b) => a.name.localeCompare(b.name, "zh-CN"));
  return next;
}

export function useDashboard() {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [history, setHistory] = useState<TelemetryPoint[]>([]);
  const [health, setHealth] = useState<Health | null>(null);
  const [live, setLive] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [ledBusy, setLedBusy] = useState(false);
  const selectedIdRef = useRef<string | null>(null);

  useEffect(() => {
    selectedIdRef.current = selectedId;
  }, [selectedId]);

  const refresh = useCallback(async () => {
    try {
      const [nextDevices, nextHealth] = await Promise.all([fetchDevices(), fetchHealth()]);
      setDevices(nextDevices);
      setHealth(nextHealth);
      setError(null);
      setSelectedId((current) => {
        if (current && nextDevices.some((device) => device.id === current)) {
          return current;
        }
        return nextDevices[0]?.id ?? null;
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "无法连接后端");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void refresh();
    const timer = window.setInterval(() => {
      void fetchHealth()
        .then(setHealth)
        .catch(() => undefined);
    }, 8000);
    return () => window.clearInterval(timer);
  }, [refresh]);

  useEffect(() => {
    const id = selectedId;
    if (!id) {
      setHistory([]);
      return;
    }
    let cancelled = false;
    fetchTelemetry(id, HISTORY_LIMIT)
      .then((points) => {
        if (!cancelled) {
          setHistory(points);
        }
      })
      .catch(() => {
        if (!cancelled) {
          setHistory([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [selectedId]);

  useEffect(() => {
    let source: EventSource | null = null;
    let retry = 0;
    let timer: number | undefined;

    const connect = () => {
      source = new EventSource("/api/events");
      source.addEventListener("ready", () => {
        setLive(true);
        retry = 0;
        void refresh();
      });
      source.addEventListener("device", (event) => {
        const body = JSON.parse((event as MessageEvent).data) as LiveDeviceEvent;
        setDevices((current) => upsertDevice(current, body.device));
      });
      source.addEventListener("telemetry", (event) => {
        const body = JSON.parse((event as MessageEvent).data) as LiveTelemetryEvent;
        setDevices((current) => upsertDevice(current, body.device));
        if (body.deviceId === selectedIdRef.current) {
          setHistory((current) => [...current, body.sample].slice(-HISTORY_LIMIT));
        }
      });
      source.onerror = () => {
        setLive(false);
        source?.close();
        const delay = Math.min(8000, 800 * 2 ** retry);
        retry += 1;
        timer = window.setTimeout(connect, delay);
      };
    };

    connect();
    return () => {
      source?.close();
      if (timer) {
        window.clearTimeout(timer);
      }
    };
  }, [refresh]);

  const selected = useMemo(
    () => devices.find((device) => device.id === selectedId) ?? null,
    [devices, selectedId]
  );

  const toggleLed = useCallback(async () => {
    if (!selected) {
      return;
    }
    setLedBusy(true);
    const next = !selected.ledOn;
    setDevices((current) =>
      current.map((device) => (device.id === selected.id ? { ...device, ledOn: next } : device))
    );
    try {
      const updated = await setLed(selected.id, next);
      setDevices((current) => upsertDevice(current, updated));
      setError(null);
    } catch (err) {
      setDevices((current) =>
        current.map((device) => (device.id === selected.id ? { ...device, ledOn: !next } : device))
      );
      setError(err instanceof Error ? err.message : "LED 指令发送失败");
    } finally {
      setLedBusy(false);
    }
  }, [selected]);

  return {
    devices,
    selected,
    selectedId,
    setSelectedId,
    history,
    health,
    live,
    loading,
    error,
    ledBusy,
    toggleLed,
    refresh,
  };
}
