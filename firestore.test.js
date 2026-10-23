const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user: cannot read users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").get());
});

test("Unauthenticated user: cannot read workers", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("workers").get());
});

test("Authenticated user: can create own profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@mine.org",
      displayName: "Alice Admin",
      role: "admin",
    })
  );
});

test("Authenticated user: cannot update other user's profile", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@mine.org",
      displayName: "Bob",
      role: "worker",
    })
  );
});

test("Authenticated user: can register worker with valid schema", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("workers").doc("ESP32_NODE_01").set({
      workerId: "ESP32_NODE_01",
      helmetNumber: "HLM-042",
      workerName: "Ramesh Kumar",
      workerPin: "1234",
      deviceKey: "sec_dev_key_abc",
    })
  );
});

test("Authenticated user: cannot register worker with missing required fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("workers").doc("ESP32_NODE_01").set({
      workerId: "ESP32_NODE_01",
      workerName: "Ramesh Kumar",
    })
  );
});

test("Authenticated user: can submit valid telemetry", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("telemetry").doc("tel_01").set({
      workerId: "ESP32_NODE_01",
      mq4: 380,
      mq135: 261,
      accel: 9.68,
      ir: 49107,
      riskScore: 12,
      alertLevel: "SAFE",
    })
  );
});

test("Authenticated user: can create emergency alert", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("alerts").doc("alt_01").set({
      workerId: "ESP32_NODE_01",
      helmetNumber: "HLM-042",
      alertLevel: "CRITICAL",
      reason: "Gas hazard (MQ-4 methane exceeded 2200 threshold)",
      acknowledged: false,
      escalated: false,
    })
  );
});
