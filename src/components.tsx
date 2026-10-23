import type { ReactNode } from "react";
import { levelColor } from "./api";

export function LevelBadge({ level }: { level: string }) {
  return (
    <span className="badge" style={{ "--c": levelColor[level] ?? "var(--muted)" } as React.CSSProperties}>
      {level}
    </span>
  );
}

export function Panel({ title, kicker, actions, children, className = "" }: {
  title: string; kicker?: string; actions?: ReactNode; children: ReactNode; className?: string;
}) {
  return (
    <section className={`panel ${className}`}>
      <header className="panel-head">
        <div>
          {kicker && <div className="kicker">{kicker}</div>}
          <h2>{title}</h2>
        </div>
        {actions}
      </header>
      {children}
    </section>
  );
}

export function Gauge({ label, value, max, unit, limit, digits = 0 }: {
  label: string; value: number; max: number; unit: string; limit?: number; digits?: number;
}) {
  const pct = Math.min(100, (value / max) * 100);
  const over = limit !== undefined && value >= limit;
  const near = limit !== undefined && value >= limit * 0.75;
  const color = over ? "var(--critical)" : near ? "var(--caution)" : "var(--cyan)";
  return (
    <div className="gauge">
      <div className="gauge-top">
        <span className="gauge-label">{label}</span>
        <span className="gauge-value" style={{ color }}>
          {value.toFixed(digits)} <small>{unit}</small>
        </span>
      </div>
      <div className="gauge-track">
        <div className="gauge-fill" style={{ width: `${pct}%`, background: color }} />
        {limit !== undefined && <div className="gauge-limit" style={{ left: `${(limit / max) * 100}%` }} />}
      </div>
    </div>
  );
}

export function RiskRing({ score, level }: { score: number; level: string }) {
  const r = 52;
  const c = 2 * Math.PI * r;
  const color = levelColor[level] ?? "var(--muted)";
  return (
    <svg viewBox="0 0 128 128" className="risk-ring" role="img" aria-label={`Risk ${score} of 100, ${level}`}>
      <circle cx="64" cy="64" r={r} fill="none" stroke="var(--line)" strokeWidth="10" />
      <circle cx="64" cy="64" r={r} fill="none" stroke={color} strokeWidth="10" strokeLinecap="round"
        strokeDasharray={`${(score / 100) * c} ${c}`} transform="rotate(-90 64 64)" />
      <text x="64" y="62" textAnchor="middle" className="risk-num" fill={color}>{score}</text>
      <text x="64" y="82" textAnchor="middle" className="risk-cap">RISK · {level}</text>
    </svg>
  );
}
