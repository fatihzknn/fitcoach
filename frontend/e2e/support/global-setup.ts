import { BACKEND_URL } from "./env";

const MAX_WAIT_MS = 90_000;
const POLL_INTERVAL_MS = 2_000;

export default async function globalSetup() {
  const deadline = Date.now() + MAX_WAIT_MS;

  while (Date.now() < deadline) {
    try {
      const res = await fetch(`${BACKEND_URL}/api/health`);
      if (res.ok) return;
    } catch {
      // backend not up yet, keep polling
    }
    await new Promise((resolve) => setTimeout(resolve, POLL_INTERVAL_MS));
  }

  throw new Error(
    `\n\nBackend is not reachable at ${BACKEND_URL}/api/health.\n` +
      "E2E tests need the full stack running:\n" +
      "  1) docker compose up -d db\n" +
      "  2) cd backend && mvn spring-boot:run\n" +
      "  3) then re-run the E2E suite (the frontend dev server starts itself).\n",
  );
}
