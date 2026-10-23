// TypeScript port of AiSafetyBoxEngine from the Android app.
// Stateless: callers supply the recent methane history and last impact time.

export const MQ4_THRESHOLD = 2200; // Methane ADC limit
export const MQ135_THRESHOLD = 2000; // Air quality ADC limit
export const IMPACT_THRESHOLD = 24.5; // m/s²
export const REST_THRESHOLD = 10.5; // m/s² (gravity ≈ 9.8)

export type AlertLevel = "SAFE" | "CAUTION" | "DANGER" | "CRITICAL";

export interface TelemetryInput {
  mq4: number;
  mq135: number;
  accel: number;
  ir: number;
  missing?: number;
  mpuOk?: boolean;
  maxOk?: boolean;
  mq4Ok?: boolean;
  mq135Ok?: boolean;
  healthHazard?: boolean;
  envHazard?: boolean;
}

export interface SafetyContext {
  now?: number;
  /** Previous methane samples within the last 30s, oldest first. */
  mq4History?: { ts: number; mq4: number }[];
  /** Timestamp of the most recent prior high-impact reading, if any. */
  lastImpactMillis?: number | null;
}

export interface SafetyResult {
  riskScore: number;
  alertLevel: AlertLevel;
  reasons: string[];
  predictedGasSpikeIn60s: boolean;
  isManDown: boolean;
  isHardwareFault: boolean;
  isImpact: boolean;
  buzzerPeriodSec: number; // 0 = off, 10 = missing sensor, 5 = health, 2 = env
  ledState: "GREEN_SOLID" | "YELLOW_SOLID" | "YELLOW_BLINK" | "RED_SOLID";
}

export function evaluate(t: TelemetryInput, ctx: SafetyContext = {}): SafetyResult {
  const now = ctx.now ?? Date.now();
  const reasons: string[] = [];
  const missing = t.missing ?? 0;

  const sensorFault =
    missing > 0 || t.mpuOk === false || t.maxOk === false || t.mq4Ok === false || t.mq135Ok === false;
  if (sensorFault) reasons.push(`Hardware alert: ${missing} sensor(s) offline/unreachable.`);

  const mq4Hazard = t.mq4 >= MQ4_THRESHOLD;
  const mq135Hazard = t.mq135 >= MQ135_THRESHOLD;
  const envHazard = mq4Hazard || mq135Hazard || !!t.envHazard;
  if (mq4Hazard) reasons.push(`MQ-4 Methane reading (${Math.round(t.mq4)}) exceeded safety limit (${MQ4_THRESHOLD}).`);
  if (mq135Hazard)
    reasons.push(`MQ-135 Toxic Air reading (${Math.round(t.mq135)}) exceeded safety limit (${MQ135_THRESHOLD}).`);

  // Gas trend prediction: will the slope cross the limit within 60s?
  const history = [...(ctx.mq4History ?? []).filter((h) => now - h.ts <= 30_000), { ts: now, mq4: t.mq4 }];
  let predictiveGasWarning = false;
  if (history.length >= 3) {
    const first = history[0];
    const last = history[history.length - 1];
    const dtSec = (last.ts - first.ts) / 1000;
    const dGas = last.mq4 - first.mq4;
    if (dtSec > 2 && dGas > 50) {
      const rate = dGas / dtSec;
      const projected = last.mq4 + rate * 60;
      if (projected >= MQ4_THRESHOLD && last.mq4 < MQ4_THRESHOLD) {
        predictiveGasWarning = true;
        reasons.push(
          `AI Trend Warning: Methane rising at +${Math.round(rate)} ADC/s, projected to breach hazard limit in < 60s!`
        );
      }
    }
  }

  const highImpact = t.accel >= IMPACT_THRESHOLD;
  if (highImpact) reasons.push(`High acceleration impact recorded (${t.accel.toFixed(1)} m/s²).`);

  let isManDown = false;
  if (!highImpact && ctx.lastImpactMillis) {
    const since = Math.floor((now - ctx.lastImpactMillis) / 1000);
    if (since >= 15 && since <= 90 && t.accel < REST_THRESHOLD) {
      isManDown = true;
      reasons.push(`MAN-DOWN DETECTED: Violent impact followed by immobility for ${since}s.`);
    }
  }

  const weakPulse = t.ir >= 1000 && t.ir <= 20000;
  if (weakPulse)
    reasons.push(`MAX30105 Optical sensor: Weak pulse/physiological distress signal (IR count: ${t.ir}).`);

  const healthHazard = highImpact || isManDown || weakPulse || !!t.healthHazard;

  const gasComponent = Math.floor(Math.max(Math.min(1, t.mq4 / MQ4_THRESHOLD), Math.min(1, t.mq135 / MQ135_THRESHOLD)) * 60);
  const impactComponent = highImpact || isManDown ? 35 : t.accel > 15 ? 15 : 0;
  const healthComponent = weakPulse ? 20 : 0;
  const faultComponent = sensorFault ? 10 : 0;

  let risk = Math.min(100, gasComponent + impactComponent + healthComponent + faultComponent);
  if (envHazard && healthHazard) risk = 100;
  if (isManDown) risk = Math.max(risk, 95);
  if (predictiveGasWarning) risk = Math.max(risk, 65);

  let alertLevel: AlertLevel;
  let ledState: SafetyResult["ledState"];
  let buzzer: number;
  if ((envHazard && healthHazard) || isManDown) {
    [alertLevel, ledState, buzzer] = ["CRITICAL", "RED_SOLID", 2];
  } else if (envHazard) {
    [alertLevel, ledState, buzzer] = ["DANGER", "RED_SOLID", 2];
  } else if (healthHazard) {
    [alertLevel, ledState, buzzer] = ["DANGER", sensorFault ? "YELLOW_BLINK" : "YELLOW_SOLID", 5];
  } else if (predictiveGasWarning) {
    [alertLevel, ledState, buzzer] = ["CAUTION", "YELLOW_SOLID", 5];
  } else if (sensorFault) {
    [alertLevel, ledState, buzzer] = ["CAUTION", "YELLOW_BLINK", 10];
  } else {
    [alertLevel, ledState, buzzer] = ["SAFE", "GREEN_SOLID", 0];
    if (reasons.length === 0) reasons.push("Atmospheric gases and vital signs are within safe operating limits.");
  }

  return {
    riskScore: risk,
    alertLevel,
    reasons,
    predictedGasSpikeIn60s: predictiveGasWarning,
    isManDown,
    isHardwareFault: sensorFault,
    isImpact: highImpact,
    buzzerPeriodSec: buzzer,
    ledState,
  };
}

/** Signal is considered lost after 15s without a packet. */
export const SIGNAL_LOST_MS = 15_000;
