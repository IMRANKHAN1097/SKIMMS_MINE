export type Level = "SAFE" | "CAUTION" | "DANGER" | "CRITICAL" | "SOS";

export interface Worker {
  workerId: string;
  helmetNumber: string;
  workerName: string;
  sector: string;
  status: string;
  batteryPct: number;
  lat: number;
  lng: number;
  lastRiskScore: number;
  lastAlertLevel: Level;
  lastSeenMillis: number;
}

export interface Telemetry {
  id: number;
  workerId: string;
  mq4: number;
  mq135: number;
  accel: number;
  ir: number;
  tempC: number;
  humidityPct: number;
  batteryPct: number;
  lat: number;
  lng: number;
  missing: number;
  riskScore: number;
  alertLevel: Level;
  aiReasons: string;
  timestampMillis: number;
}

export interface Alert {
  id: number;
  workerId: string;
  workerName: string;
  helmetNumber: string;
  alertLevel: Level;
  reason: string;
  riskScore: number;
  acknowledged: boolean;
  acknowledgedBy: string;
  acknowledgedAtMillis: number;
  lat: number;
  lng: number;
  createdAtMillis: number;
}

async function request<T>(path: string, init?: RequestInit & { json?: unknown }): Promise<T> {
  const res = await fetch(path, {
    ...init,
    headers: init?.json !== undefined ? { "Content-Type": "application/json" } : undefined,
    body: init?.json !== undefined ? JSON.stringify(init.json) : init?.body,
  });
  const data = await res.json().catch(() => ({}));
  if (!res.ok) throw new Error(data.error ?? `Request failed (${res.status})`);
  return data as T;
}

export const api = {
  workers: () => request<Worker[]>("/api/workers"),
  registerWorker: (w: { workerId: string; helmetNumber: string; workerName: string; workerPin: string; sector: string }) =>
    request<{ worker: Worker; deviceKey: string }>("/api/workers", { method: "POST", json: w }),
  removeWorker: (id: string) => request(`/api/workers/${encodeURIComponent(id)}`, { method: "DELETE" }),
  login: (workerId: string, pin: string) => request<Worker>("/api/login", { method: "POST", json: { workerId, pin } }),
  telemetry: (workerId: string) => request<Telemetry[]>(`/api/telemetry?workerId=${encodeURIComponent(workerId)}`),
  simulate: (packet: Record<string, unknown>) => request<SimResult>("/api/simulate", { method: "POST", json: packet }),
  sos: (workerId: string, pin: string, reason?: string) =>
    request("/api/sos", { method: "POST", json: { workerId, pin, reason } }),
  alerts: (workerId?: string) =>
    request<Alert[]>(workerId ? `/api/alerts?workerId=${encodeURIComponent(workerId)}` : "/api/alerts"),
  ack: (id: number, by: string) => request(`/api/alerts/${id}/ack`, { method: "POST", json: { by } }),
  analyze: (alertId: number) => request<{ analysis: string }>("/api/analyze", { method: "POST", json: { alertId } }),
};

export interface SimResult {
  riskScore: number;
  alertLevel: Level;
  reasons: string[];
  predictedGasSpikeIn60s: boolean;
  isManDown: boolean;
  isHardwareFault: boolean;
  buzzerPeriodSec: number;
  ledState: string;
  alertRaised: boolean;
}

export const levelColor: Record<string, string> = {
  SAFE: "var(--safe)",
  CAUTION: "var(--caution)",
  DANGER: "var(--danger)",
  CRITICAL: "var(--critical)",
  SOS: "var(--critical)",
  LOST: "var(--muted)",
};

export function timeAgo(ms: number) {
  const s = Math.max(0, Math.round((Date.now() - ms) / 1000));
  if (s < 60) return `${s}s ago`;
  if (s < 3600) return `${Math.floor(s / 60)}m ago`;
  if (s < 86400) return `${Math.floor(s / 3600)}h ago`;
  return `${Math.floor(s / 86400)}d ago`;
}
