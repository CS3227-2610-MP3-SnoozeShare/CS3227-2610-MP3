# Conventions

- Call the product a **web app** or **browser app**, never a desktop app in user-facing setup text.
- Use the implemented role labels **Guest**, **Host**, and **Support Agent**. Use **Agent** only when matching an implemented route or control label.
- Treat Firebase authentication as real authentication: users sign in with email/password, and password reset is available. Do not describe email-only login or fixed mock authentication.
- Describe the public frontend and backend as separate deployment concerns: Vercel serves the frontend, Render serves the backend, and Neon provides PostgreSQL. Use `https://cs3227-snoozeshare.vercel.app` as the current deployed frontend URL because it is recorded in `docs/runbooks/dev-environment.md`; do not invent a different URL.
- Keep account and financial wording accurate to the implementation: wallet operations are application behavior backed by the web service, while no external payment provider is connected.
- Preserve existing image references during this rewrite. Where an image depicts the old desktop UI, its caption or immediately following prose must say `Screenshot placeholder — replace with the current web UI screenshot.`
- Do not publish production Host or Agent registration codes. Explain that privileged codes come from the deployment administrator; only document local demo codes when the active local configuration verifies them.
- Check route names and visible labels against `frontend/src/app/`, role navigation components, and the relevant backend controllers before writing them.
