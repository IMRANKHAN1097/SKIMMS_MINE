import { useState } from "react";
import { api, timeAgo, type Worker } from "../api";
import { Gauge, LevelBadge, Panel, RiskRing } from "../components";
import { usePoll, useTick } from "../usePoll";
import { MQ135_THRESHOLD, MQ4_THRESHOLD, IMPACT_THRESHOLD, SIGNAL_LOST_MS } from "../../shared/safety";

const SESSION_KEY = "skimms-worker";

export default function WorkerView() {
  const [session, setSession] = useState<{ workerId: string; pin: string } | null>(() => {
    try { return JSON.parse(sessionStorage.getItem(SESSION_KEY) ?? "null"); } catch { return null; }
  });

  const signOut = () => { sessionStorage.removeItem(SESSION_KEY); setSession(null); };

  if (!session) {
    return <Login onLogin={(s) => { sessionStorage.setItem(SESSION_KEY, JSON.stringify(s)); setSession(s); }} />;
  }
  return <Dashboard workerId={session.workerId} pin={session.pin} onSignOut={signOut} />;
}

function Login({ onLogin }: { onLogin: (s: { workerId: string; pin: string }) => void }) {
  const workers = usePoll(api.workers, 15000);
  const [workerId, setWorkerId] = useState("");
  const [pin, setPin] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const submit = async (e: React.FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await api.login(workerId, pin);
      onLogin({ workerId, pin });
    } catch (err) {
      setError((err as Error).message);
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="login-wrap">
      <form className="panel login" onSubmit={submit}>
        <div className="kicker">Miner sign-in</div>
        <h2>Pair with your helmet</h2>
        <p className="muted">Select your helmet and enter the 4-digit PIN issued by the control room.</p>
        <label>Helmet
          <select required value={workerId} onChange={(e) => setWorkerId(e.target.value)}>
            <option value="" disabled>{workers.data ? "Choose helmet…" : "Loading…"}</option>
            {workers.data?.map((w) => (
              <option key={w.workerId} value={w.workerId}>{w.helmetNumber} — {w.workerName}</option>
            ))}
          </select>
        </label>
        <label>PIN
          <input required type="password" inputMode="numeric" pattern="\d{4}" maxLength={4} value={pin}
            onChange={(e) => setPin(e.target.value)} className="pin-input" />
        </label>
        {error && <div className="error-bar">{error}</div>}
        <button className="btn primary block" disabled={busy}>{busy ? "Verifying…" : "Connect helmet"}</button>
        <p className="hint">Demo helmets: HLM-042 · 1234, HLM-019 · 5678, HLM-033 · 9999</p>
      </form>
    </div>
  );
}

function Dashboard({ workerId, pin, onSignOut }: { workerId: string; pin: string; onSignOut: () => void }) {
  useTick(1000);
  const workers = usePoll(api.workers, 3000);
  const telemetry = usePoll(() => api.telemetry(workerId), 3000, [workerId]);
  const alerts = usePoll(() => api.alerts(workerId), 4000, [workerId]);
  const [sosOpen, setSosOpen] = useState(false);
  const [sosSent, setSosSent] = useState(false);

  const worker: Worker | undefined = workers.data?.find((w) => w.workerId === workerId);
  const latest = telemetry.data?.[0];
  if (workers.data && !worker) {
    return (
      <div className="login-wrap">
        <div className="panel login">
          <h2>Helmet not found</h2>
          <p className="muted">This helmet was decommissioned by the control room.</p>
          <button className="btn primary block" onClick={onSignOut}>Back to sign-in</button>
        </div>
      </div>
    );
  }
  if (!worker) return <div className="empty">Connecting to helmet…</div>;

  const lost = Date.now() - worker.lastSeenMillis > SIGNAL_LOST_MS;
  const level = worker.lastAlertLevel;
  const reasons = latest?.aiReasons ? latest.aiReasons.split(" | ") : [];

  const sendSos = async () => {
    await api.sos(workerId, pin);
    setSosOpen(false);
    setSosSent(true);
    alerts.refresh();
    setTimeout(() => setSosSent(false), 6000);
  };

  return (
    <div className="worker">
      <div className="worker-head">
        <div>
          <div className="kicker">SKIMMS Kavach · {worker.helmetNumber}</div>
          <h1>{worker.workerName}</h1>
          <div className="muted">{worker.sector}</div>
        </div>
        <button className="btn ghost small" onClick={onSignOut}>Sign out</button>
      </div>

      {lost && (
        <div className="banner warn">
          Helmet signal lost — no packet for {Math.round((Date.now() - worker.lastSeenMillis) / 1000)}s. Check helmet power and Wi-Fi bridge.
        </div>
      )}
      {sosSent && <div className="banner crit">SOS broadcast to the control room. Stay where you are if safe; rescue is being dispatched.</div>}

      <div className="worker-grid">
        <Panel title="Live safety status" kicker={latest ? `Last packet ${timeAgo(latest.timestampMillis)}` : "Awaiting first packet"}>
          <div className="status-row">
            <RiskRing score={worker.lastRiskScore} level={level} />
            <div className="status-reasons">
              <LevelBadge level={level} />
              <ul>
                {(reasons.length ? reasons : ["No telemetry yet. Readings appear here once the helmet transmits."]).map((r) => (
                  <li key={r}>{r}</li>
                ))}
              </ul>
              <div className="mono muted">🔋 {worker.batteryPct}% · {worker.lat.toFixed(4)}, {worker.lng.toFixed(4)}</div>
            </div>
          </div>
        </Panel>

        <button className="sos" onClick={() => setSosOpen(true)}>
          <span>SOS</span>
          <small>Emergency trigger</small>
        </button>

        <Panel title="Helmet sensors" kicker="ESP32 node">
          {latest ? (
            <div className="gauges">
              <Gauge label="MQ-4 Methane" value={latest.mq4} max={3500} unit="ADC" limit={MQ4_THRESHOLD} />
              <Gauge label="MQ-135 Toxic air" value={latest.mq135} max={3500} unit="ADC" limit={MQ135_THRESHOLD} />
              <Gauge label="MPU6050 Acceleration" value={latest.accel} max={40} unit="m/s²" limit={IMPACT_THRESHOLD} digits={1} />
              <Gauge label="MAX30105 IR pulse" value={latest.ir} max={120000} unit="" />
              <Gauge label="Temperature" value={latest.tempC} max={60} unit="°C" limit={45} digits={1} />
              <Gauge label="Humidity" value={latest.humidityPct} max={100} unit="%" />
            </div>
          ) : (
            <div className="empty">No sensor readings yet. Use the Test Bench to send packets for this helmet.</div>
          )}
        </Panel>

        <Panel title="My alerts" kicker={`${alerts.data?.length ?? 0} logged`}>
          {alerts.data?.length === 0 && <div className="empty">No active alerts logged for this helmet.</div>}
          <div className="feed-list compact">
            {alerts.data?.slice(0, 8).map((a) => (
              <div key={a.id} className={`alert ${a.acknowledged ? "acked" : ""}`}>
                <div className="alert-top">
                  <LevelBadge level={a.alertLevel} />
                  <span className="mono muted">{timeAgo(a.createdAtMillis)}{a.acknowledged ? " · acknowledged" : ""}</span>
                </div>
                <p className="alert-reason">{a.reason}</p>
              </div>
            ))}
          </div>
        </Panel>
      </div>

      {sosOpen && (
        <div className="modal-back" onClick={() => setSosOpen(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()} role="dialog" aria-modal>
            <div className="kicker" style={{ color: "var(--critical)" }}>Emergency</div>
            <h3>Trigger SOS alert?</h3>
            <p className="muted">The control room is alerted immediately with your helmet ID and last known position.</p>
            <div className="modal-actions">
              <button className="btn ghost" onClick={() => setSosOpen(false)}>Cancel</button>
              <button className="btn danger" onClick={sendSos}>Broadcast SOS</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
