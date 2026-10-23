import type { Config } from "@netlify/functions";
import { GoogleGenAI } from "@google/genai";
import { desc, eq } from "drizzle-orm";
import { db } from "../../db/index.js";
import { alerts, telemetry, workers } from "../../db/schema.js";

const ai = new GoogleGenAI({});

export default async (req: Request) => {
  if (req.method !== "POST") return new Response("Method not allowed", { status: 405 });
  const { alertId } = await req.json().catch(() => ({}));

  const [alert] = await db.select().from(alerts).where(eq(alerts.id, Number(alertId))).limit(1);
  if (!alert) return Response.json({ error: "Alert not found." }, { status: 404 });
  const [worker] = await db.select().from(workers).where(eq(workers.workerId, alert.workerId)).limit(1);
  const [t] = await db.select().from(telemetry).where(eq(telemetry.workerId, alert.workerId))
    .orderBy(desc(telemetry.timestampMillis)).limit(1);

  const prompt = `You are the Chief Mine Safety Officer and AI Incident Diagnostician for SKIMMS (Team HEXACREW, University of Calcutta).
Perform a deep, step-by-step reasoning analysis of the following mine hazard incident:

Worker: ${alert.workerName}
Helmet Node ID: ${alert.workerId} (Tag: ${alert.helmetNumber})
Underground Mine Sector: ${worker?.sector ?? "Unknown"}
GPS Coordinates: Lat ${alert.lat}, Lng ${alert.lng}
Latest Telemetry Data:
${t ? `- MQ-4 Methane Raw ADC: ${t.mq4} (Hazard limit: 2200)
- MQ-135 Toxic Air Raw ADC: ${t.mq135} (Hazard limit: 2000)
- Acceleration: ${t.accel} m/s² (Earth gravity baseline: ~9.8 m/s²)
- MAX30105 Optical IR Pulse: ${t.ir} (Weak threshold: 1000-20000)
- Temperature: ${t.tempC} °C, Humidity: ${t.humidityPct}%
- Battery: ${t.batteryPct}%
- Missing Sensors: ${t.missing}` : "- No telemetry packets recorded for this helmet yet."}
- Incident Alert (${alert.alertLevel}, risk ${alert.riskScore}/100): ${alert.reason}

Please structure your response with:
1. ROOT-CAUSE HAZARD CLASSIFICATION (Combustible gas pocket, toxic fume accumulation, structural collapse/rockfall, or physiological distress).
2. SPREAD DYNAMICS & VENTILATION RISK (How air velocity and airway geometry could propagate the hazard to nearby shafts).
3. IMMEDIATE ACTION & CONTROL ROOM DIRECTIVES (Ventilation reversal, power isolation, rescue squad dispatch protocol).
4. SAFE RETREAT & EVACUATION CORRIDOR ADVISORY for the miner.

Provide concise, authoritative, professional safety engineering advice in plain text (no markdown tables).`;

  try {
    const response = await ai.models.generateContent({ model: "gemini-3-flash-preview", contents: prompt });
    const text = response.text?.trim();
    if (!text) return Response.json({ error: "Model returned an empty analysis." }, { status: 502 });
    return Response.json({ analysis: text });
  } catch (err) {
    console.error("Gemini analysis failed", err);
    return Response.json({ error: "AI analysis is unavailable right now." }, { status: 502 });
  }
};

export const config: Config = { path: "/api/analyze" };
