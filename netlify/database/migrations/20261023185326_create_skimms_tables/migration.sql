CREATE TABLE "alerts" (
	"id" serial PRIMARY KEY,
	"worker_id" text NOT NULL,
	"worker_name" text DEFAULT '' NOT NULL,
	"helmet_number" text DEFAULT '' NOT NULL,
	"alert_level" text NOT NULL,
	"reason" text NOT NULL,
	"risk_score" integer DEFAULT 0 NOT NULL,
	"acknowledged" boolean DEFAULT false NOT NULL,
	"acknowledged_by" text DEFAULT '' NOT NULL,
	"acknowledged_at_millis" bigint DEFAULT 0 NOT NULL,
	"lat" double precision NOT NULL,
	"lng" double precision NOT NULL,
	"created_at_millis" bigint NOT NULL
);
--> statement-breakpoint
CREATE TABLE "telemetry" (
	"id" serial PRIMARY KEY,
	"worker_id" text NOT NULL,
	"helmet_number" text DEFAULT '' NOT NULL,
	"mq4" double precision NOT NULL,
	"mq135" double precision NOT NULL,
	"accel" double precision NOT NULL,
	"ir" integer NOT NULL,
	"temp_c" double precision DEFAULT 28.5 NOT NULL,
	"humidity_pct" double precision DEFAULT 65 NOT NULL,
	"battery_pct" integer DEFAULT 100 NOT NULL,
	"lat" double precision NOT NULL,
	"lng" double precision NOT NULL,
	"missing" integer DEFAULT 0 NOT NULL,
	"risk_score" integer NOT NULL,
	"alert_level" text NOT NULL,
	"ai_reasons" text DEFAULT '' NOT NULL,
	"timestamp_millis" bigint NOT NULL
);
--> statement-breakpoint
CREATE TABLE "workers" (
	"worker_id" text PRIMARY KEY,
	"helmet_number" text NOT NULL,
	"worker_name" text NOT NULL,
	"worker_pin" text NOT NULL,
	"device_key" text NOT NULL,
	"sector" text DEFAULT 'Sector 2 - Main Drift' NOT NULL,
	"status" text DEFAULT 'ONLINE' NOT NULL,
	"battery_pct" integer DEFAULT 100 NOT NULL,
	"lat" double precision DEFAULT 22.5726 NOT NULL,
	"lng" double precision DEFAULT 88.3639 NOT NULL,
	"last_risk_score" integer DEFAULT 0 NOT NULL,
	"last_alert_level" text DEFAULT 'SAFE' NOT NULL,
	"last_seen_millis" bigint NOT NULL,
	"created_at" timestamp DEFAULT now()
);
--> statement-breakpoint
CREATE INDEX "telemetry_worker_ts_idx" ON "telemetry" ("worker_id","timestamp_millis");