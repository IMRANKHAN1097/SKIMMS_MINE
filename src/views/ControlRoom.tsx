import { useState } from "react";
import { api, levelColor, timeAgo, type Alert, type Worker } from "../api";
import { LevelBadge, Panel } from "../components";
import { usePoll, useTick } from "../usePoll";
import { SIGNAL_LOST_MS } from "../../shared/safety";

const SECTORS = [
  "Sector 1 - Haulage Incline",
  "Sector 2 - Main Drift",
  "Sector 3 - Stope B (Active Face)",
  "Sector 4 - Return Airway Shaft",
];

const isLost = (w: Worker) => Date.now() - w.lastSeenMillis > SIGNAL_LOST_MS;

export default function ControlRoom() {
  useTick(2000);
  const workers = usePoll(api.workers, 3000);
  const alerts = usePoll(() => api.alerts(), 3000);
  const [registering, setRegistering] = useState(false);
  const [analysis, setAnalysis] = useState<{ alert: Alert; text?: string; error?: string } | null>(null);
  const [filter, setFilter] = useState<"all" | "open">("open");

  const list = workers.data ?? [];
  const feed = (alerts.data ?? []).filter((a) => filter === "all" || !a.acknowledged);
  const openCount = (alerts.data ?? []).filter((a) => !a.acknowledged).length;
  const atRisk = list.filter((w) => w.lastAlertLevel !== "SAFE").length;
  const lost = list.filter(isLost).length;

  const acknowledge = async (a: Alert) => {
    await api.ack(a.id, "Control Room Admin");
    alerts.refresh();
  };

  const analyze = async (a: Alert) => {
    setAnalysis({ alert: a });
    try {
      const { analysis: text } = await api.analyze(a.id);
      setAnalysis({ alert: a, text });
    } catch (e) {
      setAnalysis({ alert: a, error: (e as Error).message });
    }
  };

  const decommission = async (w: Worker) => {
    if (!confirm(`Decommission helmet ${w.helmetNumber} (${w.workerName})?`)) return;
    await api.removeWorker(w.workerId);
    workers.refresh();
  };

  return (
    <div className="control">
      <div className="stats">
        <Stat label="Helmets registered" value={list.length} />
        <Stat label="Live signal" value={list.length - lost} tone="var(--safe)" />
        <Stat label="Signal lost" value={lost} tone={lost ? "var(--danger)" : undefined} />
        <Stat label="Miners at risk" value={atRisk} tone={atRisk ? "var(--caution)" : undefined} />
        <Stat label="Open alerts" value={openCount} tone={openCount ? "var(--critical)" : undefined} />
      </div>

      {(workers.error || alerts.error) && <div className="error-bar">Connection issue: {workers.error ?? alerts.error}</div>}

      <div className="control-grid">
        <Panel
          title="Miners Fleet"
          kicker={`${list.length} helmets`}
          className="fleet"
          actions={<button className="btn primary" onClick={() => setRegistering(true)}>+ Register helmet</button>}
        >
          {!workers.data && <div className="empty">Loading fleet…</div>}
          <div className="fleet-list">
            {list.map((w) => {
              const signalLost = isLost(w);
              const level = signalLost ? "LOST" : w.lastAlertLevel;
              return (
                <article key={w.workerId} className="worker-card" style={{ "--c": levelColor[level] } as React.CSSProperties}>
                  <div className="wc-main">
                    <div className="wc-name">{w.workerName}</div>
                    <div className="wc-meta mono">{w.helmetNumber} · {w.workerId}</div>
                    <div className="wc-meta">{w.sector}</div>
                  </div>
                  <div className="wc-side">
                    <LevelBadge level={signalLost ? "SIGNAL LOST" : w.lastAlertLevel} />
                    <div className="wc-risk mono">risk {w.lastRiskScore}</div>
                    <div className="wc-meta mono">🔋 {w.batteryPct}% · {timeAgo(w.lastSeenMillis)}</div>
                    <button className="btn ghost small" onClick={() => decommission(w)}>Decommission</button>
                  </div>
                </article>
              );
            })}
          </div>
        </Panel>

        <Panel title="Underground Drift Schematic" kicker="Elevation −180 m" className="map">
          <SectorMap workers={list} alerts={alerts.data ?? []} />
        </Panel>

        <Panel
          title="Alert Feed"
          kicker={`${openCount} unacknowledged`}
          className="feed"
          actions={
            <div className="seg">
              <button className={filter === "open" ? "on" : ""} onClick={() => setFilter("open")}>Open</button>
              <button className={filter === "all" ? "on" : ""} onClick={() => setFilter("all")}>All</button>
            </div>
          }
        >
          {alerts.data && feed.length === 0 && <div className="empty">No {filter === "open" ? "open " : ""}alerts. All clear.</div>}
          <div className="feed-list">
            {feed.map((a) => (
              <article key={a.id} className={`alert ${a.acknowledged ? "acked" : ""}`} style={{ "--c": levelColor[a.alertLevel] } as React.CSSProperties}>
                <div className="alert-top">
                  <LevelBadge level={a.alertLevel} />
                  <span className="mono muted">{timeAgo(a.createdAtMillis)}</span>
                </div>
                <div className="alert-who">{a.workerName} <span className="mono muted">{a.helmetNumber}</span></div>
                <p className="alert-reason">{a.reason}</p>
                <div className="alert-foot">
                  <span className="mono muted">risk {a.riskScore} · {a.lat.toFixed(4)}, {a.lng.toFixed(4)}</span>
                  {a.acknowledged && <span className="muted">✓ {a.acknowledgedBy}</span>}
                </div>
                <div className="alert-actions">
                  {!a.acknowledged && <button className="btn primary small" onClick={() => acknowledge(a)}>Acknowledge</button>}
                  <button className="btn cyan small" onClick={() => analyze(a)}>Deep AI reasoning</button>
                </div>
              </article>
            ))}
          </div>
        </Panel>
      </div>

      {registering && <RegisterDialog onClose={() => setRegistering(false)} onDone={workers.refresh} />}

      {analysis && (
        <div className="modal-back" onClick={() => setAnalysis(null)}>
          <div className="modal wide" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal>
            <div className="kicker">Gemini incident diagnostician</div>
            <h3>{analysis.alert.workerName} · {analysis.alert.alertLevel}</h3>
            {!analysis.text && !analysis.error && (
              <div className="thinking"><span className="spinner" /> Reasoning over multi-sensor mine telemetry…</div>
            )}
            {analysis.error && <div className="error-bar">{analysis.error}</div>}
            {analysis.text && <pre className="analysis">{analysis.text}</pre>}
            <div className="modal-actions">
              <button className="btn ghost" onClick={() => setAnalysis(null)}>Dismiss</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function Stat({ label, value, tone }: { label: string; value: number; tone?: string }) {
  return (
    <div className="stat">
      <div className="stat-value" style={tone ? { color: tone } : undefined}>{value}</div>
      <div className="stat-label">{label}</div>
    </div>
  );
}

function SectorMap({ workers, alerts }: { workers: Worker[]; alerts: Alert[] }) {
  // Fixed bounding box around the mine site so positions stay stable as helmets move.
  const box = { minLat: 22.569, maxLat: 22.576, minLng: 88.360, maxLng: 88.368 };
  const W = 600, H = 380;
  const x = (lng: number) => ((lng - box.minLng) / (box.maxLng - box.minLng)) * W;
  const y = (lat: number) => H - ((lat - box.minLat) / (box.maxLat - box.minLat)) * H;
  const sos = new Set(alerts.filter((a) => !a.acknowledged && (a.alertLevel === "SOS" || a.alertLevel === "CRITICAL")).map((a) => a.workerId));

  return (
    <svg viewBox={`0 0 ${W} ${H}`} className="mine-map" role="img" aria-label="Mine sector map with helmet positions">
      <defs>
        <pattern id="grid" width="30" height="30" patternUnits="userSpaceOnUse">
          <path d="M30 0H0V30" fill="none" stroke="var(--line)" strokeWidth="0.6" />
        </pattern>
      </defs>
      <rect width={W} height={H} fill="url(#grid)" />
      {/* Drifts and shafts */}
      <g className="drifts">
        <path d="M30 300 L250 250 L420 190 L570 120" />
        <path d="M250 250 L300 80" />
        <path d="M420 190 L470 330" />
        <path d="M110 280 L80 90 L300 80 L520 50" />
      </g>
      <g className="sector-labels">
        <text x="40" y="320">S1 · Haulage Incline</text>
        <text x="90" y="75">S2 · Main Drift</text>
        <text x="300" y="275">S3 · Stope B</text>
        <text x="440" y="40">S4 · Return Airway</text>
      </g>
      <g className="vent">
        <path d="M520 50 l14 -6 -4 10z" />
        <text x="540" y="64">VENT ↑</text>
      </g>
      {workers.map((w) => {
        const lost = isLost(w);
        const color = levelColor[lost ? "LOST" : w.lastAlertLevel];
        const cx = Math.max(10, Math.min(W - 10, x(w.lng)));
        const cy = Math.max(10, Math.min(H - 10, y(w.lat)));
        return (
          <g key={w.workerId} transform={`translate(${cx} ${cy})`}>
            {(sos.has(w.workerId) || w.lastAlertLevel === "CRITICAL") && !lost && <circle r="10" className="pulse" stroke={color} />}
            <circle r="7" fill={color} stroke="var(--bg)" strokeWidth="2" />
            <text y="-12" textAnchor="middle" className="pin-label">{w.helmetNumber}</text>
          </g>
        );
      })}
    </svg>
  );
}

function RegisterDialog({ onClose, onDone }: { onClose: () => void; onDone: () => void }) {
  const [form, setForm] = useState({ workerId: "", helmetNumber: "", workerName: "", workerPin: "", sector: SECTORS[1] });
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [deviceKey, setDeviceKey] = useState<string | null>(null);
  const set = (k: keyof typeof form) => (e: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) =>
    setForm({ ...form, [k]: e.target.value });

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const res = await api.registerWorker(form);
      setDeviceKey(res.deviceKey);
      onDone();
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="modal-back" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal>
        {deviceKey ? (
          <>
            <div className="kicker">Device key generated</div>
            <h3>{form.helmetNumber} is registered</h3>
            <p className="muted">Flash this key into the ESP32 firmware. It is sent as the <code>x-device-key</code> header to <code>POST /api/telemetry</code> and won't be shown again.</p>
            <pre className="key mono">{deviceKey}</pre>
            <div className="modal-actions">
              <button className="btn primary" onClick={() => { navigator.clipboard?.writeText(deviceKey); onClose(); }}>Copy key & finish</button>
            </div>
          </>
        ) : (
          <form onSubmit={submit}>
            <div className="kicker">New helmet</div>
            <h3>Register miner helmet</h3>
            <label>ESP32 board ID<input required value={form.workerId} onChange={set("workerId")} placeholder="ESP32_NODE_04" /></label>
            <label>Helmet tag number<input required value={form.helmetNumber} onChange={set("helmetNumber")} placeholder="HLM-055" /></label>
            <label>Miner full name<input required value={form.workerName} onChange={set("workerName")} /></label>
            <label>4-digit worker login PIN<input required value={form.workerPin} onChange={set("workerPin")} inputMode="numeric" pattern="\d{4}" maxLength={4} /></label>
            <label>Assigned sector
              <select value={form.sector} onChange={set("sector")}>{SECTORS.map((s) => <option key={s}>{s}</option>)}</select>
            </label>
            {error && <div className="error-bar">{error}</div>}
            <div className="modal-actions">
              <button type="button" className="btn ghost" onClick={onClose}>Cancel</button>
              <button className="btn primary" disabled={busy}>{busy ? "Saving…" : "Generate key & save"}</button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
