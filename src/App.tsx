import { useEffect, useState } from "react";
import ControlRoom from "./views/ControlRoom";
import WorkerView from "./views/WorkerView";
import Simulator from "./views/Simulator";

const VIEWS = [
  { id: "control", label: "Control Room", hint: "Admin fleet & alerts" },
  { id: "worker", label: "Miner Kavach", hint: "Helmet wearer view" },
  { id: "simulator", label: "Test Bench", hint: "ESP32 fault injector" },
] as const;
type ViewId = (typeof VIEWS)[number]["id"];

const fromHash = (): ViewId => {
  const h = window.location.hash.slice(1);
  return (VIEWS.find((v) => v.id === h)?.id ?? "control") as ViewId;
};

export default function App() {
  const [view, setView] = useState<ViewId>(fromHash);

  useEffect(() => {
    const onHash = () => setView(fromHash());
    window.addEventListener("hashchange", onHash);
    return () => window.removeEventListener("hashchange", onHash);
  }, []);

  return (
    <div className="shell">
      <header className="topbar">
        <div className="brand">
          <svg viewBox="0 0 32 32" className="brand-mark" aria-hidden>
            <path d="M4 22a12 12 0 0 1 24 0v3H4z" fill="var(--amber)" />
            <rect x="14" y="6" width="4" height="8" rx="1" fill="var(--bg)" />
            <circle cx="16" cy="17" r="2.2" fill="var(--cyan)" />
          </svg>
          <div>
            <div className="brand-name">SKIMMS</div>
            <div className="brand-sub">Smart Kavach · Intelligent Mine Monitoring</div>
          </div>
        </div>
        <nav className="tabs" aria-label="Views">
          {VIEWS.map((v) => (
            <a key={v.id} href={`#${v.id}`} className={`tab ${view === v.id ? "active" : ""}`}>
              <span>{v.label}</span>
              <small>{v.hint}</small>
            </a>
          ))}
        </nav>
      </header>
      <main className="content">
        {view === "control" && <ControlRoom />}
        {view === "worker" && <WorkerView />}
        {view === "simulator" && <Simulator />}
      </main>
      <footer className="footer">
        Team HEXACREW · University of Calcutta · Elevation −180 m sub-surface grid
      </footer>
    </div>
  );
}
