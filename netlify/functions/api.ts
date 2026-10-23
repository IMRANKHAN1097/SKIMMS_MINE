import type { Config } from "@netlify/functions";
import { and, desc, eq, gte } from "drizzle-orm";
import { db } from "../../db/index.js";
import { alerts, telemetry, workers } from "../../db/schema.js";
import { evaluate, IMPACT_THRESHOLD } from "../../shared/safety.js";

type Worker = typeof workers.$inferSelect;

const json = (data: unknown, status = 200) => Response.json(data, { status });
const bad = (message: string, status = 400) => json({ error: message }, status);

// PINs and device keys never leave the server after registration.
const publicWorker = ({ workerPin: _pin, deviceKey: _key, createdAt: _c, ...rest }: Worker) => rest;

function newDeviceKey() {
  const bytes = crypto.getRandomValues(new Uint8Array(16));
  return "kavach_" + Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
}

async function seedIfEmpty() {
  const existing = await db.select({ id: workers.workerId }).from(workers).limit(1);
  if (existing.length) return;
  const now = Date.now();
  const inserted = await db.insert(workers).values([
    {
      workerId: "ESP32_NODE_01", helmetNumber: "HLM-042", workerName: "Ramesh Kumar", workerPin: "1234",
      deviceKey: newDeviceKey(), status: "ONLINE", batteryPct: 94, sector: "Sector 3 - Stope B (Active Face)",
      lat: 22.5735, lng: 88.3645, lastRiskScore: 15, lastAlertLevel: "SAFE", lastSeenMillis: now,
    },
    {
      workerId: "ESP32_NODE_02", helmetNumber: "HLM-019", workerName: "Subhash Bose", workerPin: "5678",
      deviceKey: newDeviceKey(), status: "ONLINE", batteryPct: 88, sector: "Sector 1 - Haulage Incline",
      lat: 22.5718, lng: 88.3628, lastRiskScore: 22, lastAlertLevel: "SAFE", lastSeenMillis: now - 4000,
    },
    {
      workerId: "ESP32_NODE_03", helmetNumber: "HLM-033", workerName: "Priya Das", workerPin: "9999",
      deviceKey: newDeviceKey(), status: "STANDBY", batteryPct: 78, sector: "Sector 4 - Return Airway Shaft",
      lat: 22.5742, lng: 88.3658, lastRiskScore: 45, lastAlertLevel: "CAUTION", lastSeenMillis: now - 12000,
    },
  ]).onConflictDoNothing().returning({ id: workers.workerId });
  // Only the request that actually seeded workers seeds the sample alert.
  if (inserted.length) await db.insert(alerts).values({
    workerId: "ESP32_NODE_03", workerName: "Priya Das", helmetNumber: "HLM-033", alertLevel: "CAUTION",
    reason: "Ventilation fluctuation detected in Sector 4 Return Airway.", riskScore: 45,
    lat: 22.5742, lng: 88.3658, createdAtMillis: now - 150000,
  });
}

const num = (v: unknown, fallback: number) => (typeof v === "number" && Number.isFinite(v) ? v : fallback);

/** Accepts either the flat simulator shape or the nested ESP32 firmware JSON. */
function parsePacket(body: any) {
  const gps = body.gps ?? {};
  const s = body.sensors ?? body;
  const st = body.status ?? body;
  return {
    workerId: String(body.board_id ?? body.workerId ?? ""),
    mq4: num(s.mq4, 380),
    mq135: num(s.mq135, 261),
    accel: num(s.accel, 9.8),
    ir: Math.round(num(s.ir, 49107)),
    tempC: num(s.temp ?? s.tempC, 28.5),
    humidityPct: num(s.humidity ?? s.humidityPct, 65),
    batteryPct: Math.round(num(body.battery ?? body.batteryPct, 92)),
    lat: num(gps.lat ?? body.lat, NaN),
    lng: num(gps.lng ?? body.lng, NaN),
    missing: Math.round(num(st.missing, 0)),
    mpuOk: st.mpu_ok ?? st.mpuOk ?? true,
    maxOk: st.max_ok ?? st.maxOk ?? true,
    mq4Ok: st.mq4_ok ?? st.mq4Ok ?? true,
    mq135Ok: st.mq135_ok ?? st.mq135Ok ?? true,
    healthHazard: st.health_hazard ?? st.healthHazard ?? false,
    envHazard: st.env_hazard ?? st.envHazard ?? false,
  };
}

async function ingest(worker: Worker, body: any) {
  const p = parsePacket(body);
  const now = Date.now();

  const recent = await db
    .select({ ts: telemetry.timestampMillis, mq4: telemetry.mq4, accel: telemetry.accel })
    .from(telemetry)
    .where(and(eq(telemetry.workerId, worker.workerId), gte(telemetry.timestampMillis, now - 90_000)))
    .orderBy(telemetry.timestampMillis);
  const lastImpact = [...recent].reverse().find((r) => r.accel >= IMPACT_THRESHOLD);

  const result = evaluate(p, {
    now,
    mq4History: recent.map((r) => ({ ts: r.ts, mq4: r.mq4 })),
    lastImpactMillis: lastImpact?.ts ?? null,
  });

  const lat = Number.isFinite(p.lat) ? p.lat : worker.lat;
  const lng = Number.isFinite(p.lng) ? p.lng : worker.lng;

  await db.insert(telemetry).values({
    workerId: worker.workerId, helmetNumber: worker.helmetNumber, mq4: p.mq4, mq135: p.mq135, accel: p.accel,
    ir: p.ir, tempC: p.tempC, humidityPct: p.humidityPct, batteryPct: p.batteryPct, lat, lng, missing: p.missing,
    riskScore: result.riskScore, alertLevel: result.alertLevel, aiReasons: result.reasons.join(" | "),
    timestampMillis: now,
  });

  await db.update(workers).set({
    lastSeenMillis: now, batteryPct: p.batteryPct, lat, lng, status: "ONLINE",
    lastRiskScore: result.riskScore, lastAlertLevel: result.alertLevel,
  }).where(eq(workers.workerId, worker.workerId));

  // Raise an alert when the level escalates or stays non-safe for over a minute without one.
  let alertRaised = false;
  if (result.alertLevel !== "SAFE") {
    const [lastAlert] = await db.select().from(alerts)
      .where(eq(alerts.workerId, worker.workerId))
      .orderBy(desc(alerts.createdAtMillis)).limit(1);
    const escalated = worker.lastAlertLevel !== result.alertLevel;
    const stale = !lastAlert || now - lastAlert.createdAtMillis > 60_000;
    if (escalated || stale) {
      await db.insert(alerts).values({
        workerId: worker.workerId, workerName: worker.workerName, helmetNumber: worker.helmetNumber,
        alertLevel: result.alertLevel, reason: result.reasons.join(" "), riskScore: result.riskScore,
        lat, lng, createdAtMillis: now,
      });
      alertRaised = true;
    }
  }

  return { ...result, alertRaised };
}

async function findWorker(id: string) {
  const [w] = await db.select().from(workers).where(eq(workers.workerId, id)).limit(1);
  return w;
}

export default async (req: Request) => {
  const url = new URL(req.url);
  const parts = url.pathname.replace(/^\/api\/?/, "").split("/").filter(Boolean);
  const [resource, id, action] = parts;
  const method = req.method;
  const body = method === "POST" ? await req.json().catch(() => ({})) : {};

  try {
    if (resource === "workers") {
      if (method === "GET" && !id) {
        await seedIfEmpty();
        const list = await db.select().from(workers).orderBy(workers.workerId);
        return json(list.map(publicWorker));
      }
      if (method === "POST" && !id) {
        const workerId = String(body.workerId ?? "").trim();
        const helmetNumber = String(body.helmetNumber ?? "").trim();
        const workerName = String(body.workerName ?? "").trim();
        const workerPin = String(body.workerPin ?? "").trim();
        const sector = String(body.sector ?? "").trim() || "Sector 2 - Main Drift";
        if (!workerId || !helmetNumber || !workerName) return bad("Board ID, helmet number and name are required.");
        if (!/^\d{4}$/.test(workerPin)) return bad("PIN must be exactly 4 digits.");
        if (await findWorker(workerId)) return bad(`Board ${workerId} is already registered.`, 409);
        const deviceKey = newDeviceKey();
        const [w] = await db.insert(workers).values({
          workerId, helmetNumber, workerName, workerPin, deviceKey, sector, batteryPct: 96,
          lat: 22.5726 + (Math.random() - 0.5) * 0.005, lng: 88.3639 + (Math.random() - 0.5) * 0.005,
          lastRiskScore: 10, lastSeenMillis: Date.now(),
        }).returning();
        return json({ worker: publicWorker(w), deviceKey }, 201);
      }
      if (method === "DELETE" && id) {
        await db.delete(workers).where(eq(workers.workerId, id));
        return json({ ok: true });
      }
    }

    if (resource === "login" && method === "POST") {
      const w = await findWorker(String(body.workerId ?? ""));
      if (!w || w.workerPin !== String(body.pin ?? "")) return bad("Invalid board ID or PIN.", 401);
      return json(publicWorker(w));
    }

    if (resource === "telemetry") {
      if (method === "GET") {
        const workerId = url.searchParams.get("workerId");
        if (!workerId) return bad("workerId is required.");
        const rows = await db.select().from(telemetry).where(eq(telemetry.workerId, workerId))
          .orderBy(desc(telemetry.timestampMillis)).limit(30);
        return json(rows);
      }
      if (method === "POST") {
        // Real helmets authenticate with the device key issued at registration.
        const key = req.headers.get("x-device-key");
        if (!key) return bad("Missing x-device-key header.", 401);
        const w = await findWorker(parsePacket(body).workerId);
        if (!w || w.deviceKey !== key) return bad("Unknown board or invalid device key.", 401);
        return json(await ingest(w, body));
      }
    }

    if (resource === "simulate" && method === "POST") {
      // Test bench: inject packets for a registered helmet from the browser.
      const w = await findWorker(String(body.workerId ?? ""));
      if (!w) return bad("Unknown board ID.", 404);
      return json(await ingest(w, body));
    }

    if (resource === "sos" && method === "POST") {
      const w = await findWorker(String(body.workerId ?? ""));
      if (!w || w.workerPin !== String(body.pin ?? "")) return bad("Invalid board ID or PIN.", 401);
      const now = Date.now();
      await db.insert(alerts).values({
        workerId: w.workerId, workerName: w.workerName, helmetNumber: w.helmetNumber, alertLevel: "SOS",
        reason: String(body.reason ?? "").trim() || "Manual SOS triggered by miner.", riskScore: 100,
        lat: w.lat, lng: w.lng, createdAtMillis: now,
      });
      await db.update(workers).set({ lastAlertLevel: "CRITICAL", lastRiskScore: 100, lastSeenMillis: now })
        .where(eq(workers.workerId, w.workerId));
      return json({ ok: true }, 201);
    }

    if (resource === "alerts") {
      if (method === "GET") {
        const workerId = url.searchParams.get("workerId");
        const q = db.select().from(alerts);
        const rows = await (workerId ? q.where(eq(alerts.workerId, workerId)) : q)
          .orderBy(desc(alerts.createdAtMillis)).limit(50);
        return json(rows);
      }
      if (method === "POST" && id && action === "ack") {
        await db.update(alerts).set({
          acknowledged: true,
          acknowledgedBy: String(body.by ?? "").trim() || "Control Room",
          acknowledgedAtMillis: Date.now(),
        }).where(eq(alerts.id, Number(id)));
        return json({ ok: true });
      }
    }

    return bad("Not found.", 404);
  } catch (err) {
    console.error("API error", err);
    return bad("Internal server error.", 500);
  }
};

export const config: Config = {
  path: ["/api/workers", "/api/workers/*", "/api/login", "/api/telemetry", "/api/simulate", "/api/sos", "/api/alerts", "/api/alerts/*"],
};
