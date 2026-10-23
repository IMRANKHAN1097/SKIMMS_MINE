import { pgTable, serial, text, timestamp, integer, doublePrecision, boolean, bigint, index } from "drizzle-orm/pg-core";

export const workers = pgTable("workers", {
  workerId: text("worker_id").primaryKey(),
  helmetNumber: text("helmet_number").notNull(),
  workerName: text("worker_name").notNull(),
  workerPin: text("worker_pin").notNull(),
  deviceKey: text("device_key").notNull(),
  sector: text().notNull().default("Sector 2 - Main Drift"),
  status: text().notNull().default("ONLINE"),
  batteryPct: integer("battery_pct").notNull().default(100),
  lat: doublePrecision().notNull().default(22.5726),
  lng: doublePrecision().notNull().default(88.3639),
  lastRiskScore: integer("last_risk_score").notNull().default(0),
  lastAlertLevel: text("last_alert_level").notNull().default("SAFE"),
  lastSeenMillis: bigint("last_seen_millis", { mode: "number" }).notNull(),
  createdAt: timestamp("created_at").defaultNow(),
});

export const telemetry = pgTable(
  "telemetry",
  {
    id: serial().primaryKey(),
    workerId: text("worker_id").notNull(),
    helmetNumber: text("helmet_number").notNull().default(""),
    mq4: doublePrecision().notNull(),
    mq135: doublePrecision().notNull(),
    accel: doublePrecision().notNull(),
    ir: integer().notNull(),
    tempC: doublePrecision("temp_c").notNull().default(28.5),
    humidityPct: doublePrecision("humidity_pct").notNull().default(65),
    batteryPct: integer("battery_pct").notNull().default(100),
    lat: doublePrecision().notNull(),
    lng: doublePrecision().notNull(),
    missing: integer().notNull().default(0),
    riskScore: integer("risk_score").notNull(),
    alertLevel: text("alert_level").notNull(),
    aiReasons: text("ai_reasons").notNull().default(""),
    timestampMillis: bigint("timestamp_millis", { mode: "number" }).notNull(),
  },
  (t) => [index("telemetry_worker_ts_idx").on(t.workerId, t.timestampMillis)]
);

export const alerts = pgTable("alerts", {
  id: serial().primaryKey(),
  workerId: text("worker_id").notNull(),
  workerName: text("worker_name").notNull().default(""),
  helmetNumber: text("helmet_number").notNull().default(""),
  alertLevel: text("alert_level").notNull(),
  reason: text().notNull(),
  riskScore: integer("risk_score").notNull().default(0),
  acknowledged: boolean().notNull().default(false),
  acknowledgedBy: text("acknowledged_by").notNull().default(""),
  acknowledgedAtMillis: bigint("acknowledged_at_millis", { mode: "number" }).notNull().default(0),
  lat: doublePrecision().notNull(),
  lng: doublePrecision().notNull(),
  createdAtMillis: bigint("created_at_millis", { mode: "number" }).notNull(),
});
