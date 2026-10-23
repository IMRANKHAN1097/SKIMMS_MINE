import { useEffect, useMemo, useRef, useState } from "react";
import { api, levelColor, type SimResult } from "../api";
import { LevelBadge, Panel } from "../components";
import { usePoll } from "../usePoll";
import { evaluate, IMPACT_THRESHOLD, MQ135_THRESHOLD, MQ4_THRESHOLD } from "../../shared/safety";

interface Packet {
  mq4: number; mq135: number; accel: number; ir: number;
  tempC: number; humidityPct: number; batteryPct: number; missing: number;
}

const NOMINAL: Packet = { mq4: 380, mq135: 261, accel: 9.8, ir: 49107, tempC: 28.5, humidityPct: 65, batteryPct: 92, missing: 0 };

const PRESETS: { label: string; packet: Partial<Packet> }[] = [
  { label: "Nominal", packet: {} },
  { label: "Methane pocket", packet: { mq4: 2650 } },
  { label: "Toxic fumes", packet: { mq135: 2300 } },
  { label: "Rockfall impact", packet: { accel: 31.2 } },
  { label: "Weak pulse", packet: { ir: 12000 } },
  { label: "Sensor offline", packet: { missing: 1 } },
  { label: "Compound hazard", packet: { mq4: 2800, accel: 28.4, ir: 9000 } },
];

const SLIDERS: { key: keyof Packet; label: string; min: number; max: number; step: number; limit?: number; unit: string }[] = [
  { key: "mq4", label: "MQ-4 Methane", min: 0, max: 4095, step: 5, limit: MQ4_THRESHOLD, unit: "ADC" },
  { key: "mq135", label: "MQ-135 Air quality", min: 0, max: 4095, step: 5, limit: MQ135_THRESHOLD, unit: "ADC" },
  { key: "accel", label: "MPU6050 Acceleration", min: 0, max: 40, step: 0.1, limit: IMPACT_THRESHOLD, unit: "m/s²" },
  { key: "ir", label: "MAX30105 IR count", min: 0, max: 120000, step: 100, unit: "" },
  { key: "tempC", label: "DHT Temperature", min: 10, max: 60, step: 0.5, unit: "°C" },
  { key: "humidityPct", label: "DHT Humidity", min: 0, max: 100, step: 1, unit: "%" },
  { key: "batteryPct", label: "Battery", min: 0, max: 100, step: 1, unit: "%" },
  { key: "missing", label: "Missing sensors", min: 0, max: 5, step: 1, unit: "" },
];

export default function Simulator() {
  const workers = usePoll(api.workers, 10000);
  const [workerId, setWorkerId] = useState("");
  const [packet, setPacket] = useState<Packet>(NOMINAL);
  const [streaming, setStreaming] = useState(false);
  const [ramp, setRamp] = useState(false);
  const [result, setResult] = useState<SimResult | null>(null);
  const [log, setLog] = useState<{ t: number; level: string; risk: number; alert: boolean }[]>([]);
  const [error, setError] = useState<string | null>(null);
  const packetRef = useRef(packet);
  packetRef.current = packet;

  useEffect(() => {
    if (!workerId && workers.data?.length) setWorkerId(workers.data[0].workerId);
  }, [workers.data, workerId]);

  // Instant local preview of the same rules the server applies (without trend/man-down history).
  const preview = useMemo(() => evaluate(packet), [packet]);

  const send = async () => {
    if (!workerId) return;
    const p = packetRef.current;
    try {
      const res = await api.simulate({
        workerId,
        sensors: { mq4: p.mq4, mq135: p.mq135, accel: p.accel, ir: p.ir, temp: p.tempC, humidity: p.humidityPct },
        status: { missing: p.missing, mpu_ok: true, max_ok: true, mq4_ok: true, mq135_ok: true },
        battery: p.batteryPct,
      });
      setResult(res);
      setError(null);
      setLog((l) => [{ t: Date.now(), level: res.alertLevel, risk: res.riskScore, alert: res.alertRaised }, ...l].slice(0, 12));
    } catch (e) {
      setError((e as Error).message);
    }
  };

  useEffect(() => {
    if (!streaming) return;
    send();
    const t = setInterval(() => {
      if (ramp) setPacket((p) => ({ ...p, mq4: Math.min(4095, p.mq4 + 120) }));
      send();
    }, 2000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [streaming, ramp, workerId]);

  const shown = result ?? { ...preview, alertRaised: false };

  return (
    <div className="sim">
      <div className="sim-grid">
        <Panel title="ESP32 telemetry test bench" kicker="Hardware & fault injector">
          <label>Target helmet
            <select value={workerId} onChange={(e) => { setWorkerId(e.target.value); setResult(null); }}>
              {workers.data?.map((w) => <option key={w.workerId} value={w.workerId}>{w.helmetNumber} — {w.workerName}</option>)}
            </select>
          </label>

          <div className="presets">
            {PRESETS.map((p) => (
              <button key={p.label} className="chip" onClick={() => { setPacket({ ...NOMINAL, ...p.packet }); setResult(null); }}>
                {p.label}
              </button>
            ))}
          </div>

          <div className="sliders">
            {SLIDERS.map((s) => {
              const v = packet[s.key];
              const over = s.limit !== undefined && v >= s.limit;
              return (
                <label key={s.key} className="slider">
                  <span className="slider-top">
                    <span>{s.label}{s.limit !== undefined && <small className="muted"> · limit {s.limit}</small>}</span>
                    <span className="mono" style={{ color: over ? "var(--critical)" : "var(--text)" }}>
                      {s.step < 1 ? v.toFixed(1) : v} {s.unit}
                    </span>
                  </span>
                  <input type="range" min={s.min} max={s.max} step={s.step} value={v}
                    onChange={(e) => { setPacket({ ...packet, [s.key]: Number(e.target.value) }); setResult(null); }} />
                </label>
              );
            })}
          </div>

          <div className="sim-actions">
            <button className="btn primary" onClick={send} disabled={!workerId || streaming}>Send 1 telemetry packet</button>
            <button className={`btn ${streaming ? "danger" : "cyan"}`} onClick={() => setStreaming(!streaming)} disabled={!workerId}>
              {streaming ? "Stop stream" : "Stream every 2s"}
            </button>
            <label className="check">
              <input type="checkbox" checked={ramp} onChange={(e) => setRamp(e.target.checked)} />
              Ramp methane +120/packet (trend AI)
            </label>
          </div>
          {error && <div className="error-bar">{error}</div>}
        </Panel>

        <div className="sim-side">
          <Panel title="Live safety evaluation" kicker={result ? "Server result" : "Local preview"}>
            <div className="eval" style={{ "--c": levelColor[shown.alertLevel] } as React.CSSProperties}>
              <div className="eval-top">
                <LevelBadge level={shown.alertLevel} />
                <span className="eval-risk mono">{shown.riskScore}<small>/100</small></span>
              </div>
              <div className="hw">
                <div><span className={`led ${shown.ledState.toLowerCase()}`} /> LED {shown.ledState.replace("_", " ")}</div>
                <div>🔔 Buzzer {shown.buzzerPeriodSec ? `every ${shown.buzzerPeriodSec}s` : "silent"}</div>
              </div>
              <ul>{shown.reasons.map((r) => <li key={r}>{r}</li>)}</ul>
              {result?.alertRaised && <div className="banner crit small">Alert raised in the Control Room feed</div>}
            </div>
          </Panel>

          <Panel title="Packet log" kicker="Last 12">
            {log.length === 0 && <div className="empty">No packets sent yet.</div>}
            <ul className="log mono">
              {log.map((l) => (
                <li key={l.t}>
                  <span className="muted">{new Date(l.t).toLocaleTimeString()}</span>
                  <span style={{ color: levelColor[l.level] }}>{l.level}</span>
                  <span>risk {l.risk}</span>
                  {l.alert && <span style={{ color: "var(--critical)" }}>▲ alert</span>}
                </li>
              ))}
            </ul>
          </Panel>

          <Panel title="Real helmet integration" kicker="Firmware endpoint">
            <p className="muted small-text">ESP32 nodes post JSON to <code>/api/telemetry</code> with the <code>x-device-key</code> header issued at registration:</p>
            <pre className="code mono">{`POST /api/telemetry
x-device-key: kavach_…
{
  "board_id": "ESP32_NODE_01",
  "gps": { "lat": 22.57, "lng": 88.36, "valid": true },
  "sensors": { "mq4": 380, "mq135": 261, "accel": 9.8,
               "ir": 49107, "temp": 28.5, "humidity": 65 },
  "status": { "missing": 0, "mpu_ok": true, "max_ok": true },
  "battery": 92
}`}</pre>
          </Panel>
        </div>
      </div>
    </div>
  );
}
