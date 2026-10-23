# Web User Guide Rewrite Design

- **Status:** Approved (Trevor, 2026-10-23)
- **Risk tier:** Low - documentation-only; no application behavior, data, permissions, or deployment changes
- **Workstream:** User-guide web rewrite | **Plan:** [implementation plan](../plans/2026-10-23-user-guide-web.md)
- **Constraints consulted:** `PROJECT_STATE.md` § How to Resume → Upcoming (rolling guide rewrite); C76 (web re-platform); C81 (three independent components); C85 (Render/Vercel/Neon hosting); C86–C87 (Firebase roles and registration codes); C98 (guide approval gate)
- **Reviewed by lenses:** Implementer, Tester (round 1; findings addressed in this draft)

## 1. Problem

`docs/UserGuide.md` currently describes the retired user experience as a JavaFX desktop
application: Java 25 and Gradle prerequisites, a local SQLite file, packaged JAR startup,
mocked email-only authentication, and single-user limitations. The current implementation is a
Next.js browser frontend backed by Spring Boot, Firebase authentication, and PostgreSQL, with
Guest, Host, and Support Agent routes plus role-specific AI assistants.

The guide must become an accurate browser-oriented guide for the web app. Screenshots are not
part of this work; existing image references may remain as temporary placeholders until the
operator replaces them.

## 2. Goals

**Functional**

- F1 Replace desktop-only quick-start instructions with browser access, the verified deployed frontend URL, optional local development access, account setup, role routing, and current environment notes.
- F2 Document Firebase-backed sign-in, registration, password reset, administrator-provided privileged registration codes, and suspended-account behavior without claiming that authentication is mocked.
- F3 Document the current Guest, Host, and Support Agent journeys, including web route names, API-backed behavior, and the implemented Host and Guest AI assistants.
- F4 Update FAQ, known limitations, troubleshooting, glossary, and navigation so they contain no desktop-only claims or SQLite/JAR instructions.

**Non-functional**

- N1 Every factual claim must be checked against the current frontend, backend, runbook, or `PROJECT_STATE.md`; unresolved visual details remain marked for later screenshot/manual confirmation. The post-approval plan's Traceability section retains a source-to-section checklist with the columns `Guide section`, `required claim or route`, `source path`, and `verification`, with one row for every required section and feature listed in F1–F4.
- N2 Keep the guide usable as a Jekyll Markdown page and preserve valid internal links and image references.

## 3. Non-goals

- Screenshot replacement or new screenshot capture - the operator will update screenshots later.
- Changes to frontend, backend, Firebase, deployment, or role behavior - the guide records existing behavior only.
- Documentation of the unfinished Agent AI feature - `PROJECT_STATE.md` lists W-AI-AGENT as remaining work.
- Removal of the desktop implementation - the desktop app remains in `desktop/` while the re-platforming continues (C76).

## 4. Components

- [Conventions](2026-10-23-user-guide-web-design/conventions.md) - terminology, source-of-truth rules, and screenshot placeholder treatment.
- [Edge cases](2026-10-23-user-guide-web-design/edge-cases.md) - stale-claim and link-validation checks.
- [Implementation order](2026-10-23-user-guide-web-design/implementation-order.md) - documentation rewrite sequence.
- No data model or API changes: this work changes only the end-user Markdown guide.

## 5. Open questions

| ID | Question | Blocks | Owner |
|---|---|---|---|

## 6. Decisions made in this spec

| ID | Decision | Why / rejected alternative | Asked by |
|---|---|---|---|
| SD1 | Rewrite `docs/UserGuide.md` as a browser-app guide while retaining image paths as placeholders | The operator asked for documentation work and explicitly deferred screenshots; replacing screenshots now would expand scope | Operator |
| SD2 | Describe local development as an optional developer path and deployed browser access as the primary user path | The current system is a web app hosted through Vercel/Render/Neon; retaining JavaFX startup would mislead end users | Spec author, based on C76/C81/C85 |
| SD3 | Do not print production Host or Agent registration codes; tell users to obtain them from the deployment administrator | The runbook marks those values as secrets; publishing them would be unsafe. Local demo values may be documented only if verified against the active local configuration | Implementer review; repository runbook |
| SD4 | Use the verified frontend URL from the runbook as the current deployed entry point, while noting that deployments may change it | The URL is known in repository evidence, but environment URLs are operational configuration rather than a permanent product contract | Implementer review; repository runbook |
| SD5 | Any retained desktop screenshot gets a replacement marker in its caption or surrounding prose | The operator will replace screenshots later, but users must not mistake old visuals for the current web UI | Operator |
