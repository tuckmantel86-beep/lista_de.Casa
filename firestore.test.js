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

test("Unauthenticated user: cannot read shopping_lists or shopping_items", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shopping_lists").get());
  await assertFails(unauthDb.collection("shopping_items").get());
});

test("Authenticated user: can create a house and a shopping list", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("houses").doc("house_1").set({
      name: "Casa da Alice",
      code: "#CASA-1234",
      ownerId: ALICE_UID,
      memberUids: [ALICE_UID],
    })
  );

  await assertSucceeds(
    aliceDb.collection("shopping_lists").doc("list_1").set({
      houseId: "house_1",
      name: "Feira Semanal",
      icon: "🛒",
      colorHex: "#16A34A",
      isArchived: false,
      memberUids: [ALICE_UID],
    })
  );
});

test("Unauthorized user: Bob cannot read Alice's shopping items", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("shopping_items").doc("item_1").set({
      listId: "list_1",
      houseId: "house_1",
      name: "Arroz 5kg",
      category: "Mercearia",
      itemCount: 1,
      isCompleted: false,
      memberUids: [ALICE_UID],
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("shopping_items").doc("item_1").get());
});

test("Collaborative user: Bob can read and toggle item if he is in memberUids", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("shopping_items").doc("item_2").set({
      listId: "list_1",
      houseId: "house_1",
      name: "Café Torrado",
      category: "Matinais",
      itemCount: 2,
      isCompleted: false,
      memberUids: [ALICE_UID, BOB_UID],
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(bobDb.collection("shopping_items").doc("item_2").get());

  await assertSucceeds(
    bobDb.collection("shopping_items").doc("item_2").update({
      isCompleted: true,
    })
  );
});
