const { initializeTestEnvironment, assertFails, assertSucceeds } = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;

before(async () => {
  testEnv = await initializeTestEnvironment({
    projectId: "demo-no-project",
    firestore: {
      rules: fs.readFileSync("firestore.rules", "utf8"),
      host: "127.0.0.1",
      port: 8085
    }
  });
});

beforeEach(async () => {
  await testEnv.clearFirestore();
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

test("unauthenticated read fails", async () => {
  const unauthedDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthedDb.collection("users").doc("alice").get());
  await assertFails(unauthedDb.collection("users").doc("alice").collection("chats").get());
});

test("authenticated owner can create and read user profile and chat", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  
  await assertSucceeds(
    aliceDb.collection("users").doc("alice").set({
      userId: "alice",
      email: "alice@example.com",
      displayName: "Alice Dev"
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc("alice").collection("chats").doc("chat-1").set({
      userId: "alice",
      title: "Architecture Planning",
      model: "gemini-3.8-flash",
      pinned: true
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc("alice").collection("workspaces").doc("ws-1").set({
      userId: "alice",
      name: "CLOUKE Core",
      language: "Kotlin",
      description: "Mobile IDE Engine"
    })
  );
});

test("cross-user access is denied", async () => {
  const aliceDb = testEnv.authenticatedContext("alice").firestore();
  await aliceDb.collection("users").doc("alice").set({
    userId: "alice",
    email: "alice@example.com"
  });

  const bobDb = testEnv.authenticatedContext("bob").firestore();
  await assertFails(bobDb.collection("users").doc("alice").get());
  await assertFails(
    bobDb.collection("users").doc("alice").collection("chats").doc("chat-1").set({
      userId: "alice",
      title: "Hacked Chat"
    })
  );
});
