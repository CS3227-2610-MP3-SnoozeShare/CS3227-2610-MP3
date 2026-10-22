# W-AI-GATEWAY Implementation Plan

- **Status:** Approved (Xiao-Congchen, 2026-10-22)

> **For agents:** tick `- [ ]` -> `- [x]` as you finish each step, in this file, in the same commit as the work. Keep the branch state's Progress cell current (`AGENTS.md` § 2). Spec: [W-AI-GATEWAY design](../specs/2026-10-22-w-ai-gateway-design.md).

## Scope

**Goal:** Add the shared `com.snoozeshare.ai` gateway through which every role's LLM feature calls NUS SoCLaaS, with global rate and concurrency control, guardrails, caps, audit, mock mode and fallbacks.

**In scope:** F1 to F14 and N1 to N6 of the spec; the 60 edge cases E1 to E60; the backend only.

**Non-goals:** streaming; response caching; embeddings, `/v1/responses`, audio and tool calling; the portal budget endpoint; any role feature, endpoint, frontend, AI draft field or `AI` author role; limiter state across instances or restarts (Known Gap "Rate limiting is process-local", C69); an ops statistics endpoint (the health body stays `{"status":"UP"}`); a repair retry for bad JSON; a content-safety classifier; a role cross-check query (SD18).

**Risk tier:** High - every task ships its own tests, the Attacker threats (E41 to E47, E55 to E60) are verified by tests, and the branch is reviewed before any push (`AGENTS.md` § 8).

## Task order

`Post-merge` marks a task a human does after the merge (T8). Commands: Gradle from `backend/` in PowerShell; run Checkstyle and the Testcontainers tests as separate invocations on a low-memory machine (`PROJECT_STATE.md` § Conventions).

| Task | Depends on | Can run concurrently with | Wave | Status |
|---|---|---|---|---|
| T1 Core types and configuration | - | - | 1 | Done |
| T2 Audit actions and writer | T1 | T3, T4, T5 | 2 | Done |
| T3 Guardrails | T1 | T2, T4, T5 | 2 | Done |
| T4 Limits and resilience | T1 | T2, T3, T5 | 2 | Done |
| T5 SoCLaaS client | T1 | T2, T3, T4 | 2 | Done |
| T6 Gateway facade | T2, T3, T4, T5 | - | 3 | Done |
| T7 Integration tests, boundary test and docs | T6 | - | 4 | Done |
| T8 Live verification | T7 | - | 5 | Post-merge |

## Traceability

Test classes are under `backend/src/test/java/com/snoozeshare/ai/` unless a path is given.

| Requirement | Task(s) | Verified by |
|---|---|---|
| F1 | T1, T6 | `LlmCallTest`, `LlmGatewayTest.neverThrowsForExpectedFailures` |
| F2 | T6 | `LlmGatewayTest.roleDenied` |
| F3 | T3 | `InputSanitizerTest`, `PromptBuilderTest` |
| F4 | T3 | `InjectionDetectorTest` |
| F5 | T3 | `PromptBuilderTest.layout` |
| F6 | T4 | `SchedulerCoreTest`, `LlmSchedulerTest` |
| F7 | T2, T4 | `AiAuditWriterTest.capsUnderParallelBurst`, `UserThrottleTest` |
| F8 | T5, T6 | `SoclaasClientTest.requestShape`, `ModelCheckTest` |
| F9 | T4, T5, T6 | `CircuitBreakerTest`, `BudgetBlockTest`, `BackoffTest`, `LlmGatewayTest.retryMatrix` |
| F10 | T3 | `OutputValidatorTest` |
| F11 | T2, T6 | `AiAuditWriterTest`, `LlmGatewayIntegrationTest.auditRows` |
| F12 | T6 | `LlmGatewayTest.disabled`, `LlmGatewayTest.mockMode` |
| F13 | T1 | `LlmPropertiesTest`, `ProdEnvironmentGuardAiTest`, `ProdProfileTest` |
| F14 | T6 | `LlmGatewayTest.pipelineOrder` |
| N1 | T1, T5, T7 | `LlmPropertiesTest.toStringHidesKey`, `SoclaasClientTest`, `AiLogLeakTest` |
| N2 | T6 | `LlmGatewayTest.deadline` |
| N3 | T4 | `SchedulerCoreTest.ceilings` |
| N4 | T6, T7 | `AiHealthTest`, `AiDatasourceFreeContextTest` |
| N5 | T7 | `SecretScanTest`, build with no `SOCLAAS_*` variables |
| N6 | T2, T6 | `AiAuditWriterTest.noPromptText`, `LlmGatewayIntegrationTest.noMarkersInRows` |
| E1 | T3 | `InputSanitizerTest.finalCapAfterRedaction` |
| E2 | T3 | `InputSanitizerTest.tagVariants` |
| E3 | T3 | `PromptBuilderTest.keepsLastTurnsAndStartsWithUser` |
| E4 | T1 | `LlmCallTest.shapeErrors` |
| E5 | T3 | `PromptBuilderTest.blankLastUserTurn` |
| E6 | T3 | `PiiRedactorTest`, `InputSanitizerTest.pathologicalInput` |
| E7 | T3 | `InjectionDetectorTest.flagsAndBenign` |
| E8 | T3 | `PromptBuilderTest.layout` |
| E9 | T6 | `LlmGatewayTest.roleDenied` |
| E10 | T1 | `AiFeatureRegistryTest.rejectsBadFeatures` |
| E11 | T1 | `AiFeatureRegistryTest.duplicateAndEmpty` |
| E12 | T4 | `UserThrottleTest.rollingWindow` |
| E13 | T2 | `AiAuditWriterTest.capsUnderParallelBurst` |
| E14 | T4 | `SchedulerCoreTest.ceilingsAndPriority` |
| E15 | T4, T6 | `SchedulerCoreTest.queueFull`, `LlmGatewayIntegrationTest.rejectedRows` |
| E16 | T4, T6 | `SchedulerCoreTest.queueTimeout`, `LlmGatewayIntegrationTest.rejectedRows` |
| E17 | T4 | `SchedulerCoreTest.retriesKeepArrival` |
| E18 | T5, T6 | `BackoffTest`, `LlmGatewayTest.retryMatrix` |
| E19 | T4, T6 | `BudgetBlockTest`, `LlmGatewayTest.budgetBlock` |
| E20 | T4, T6 | `CircuitBreakerTest`, `LlmGatewayTest.breakerOpens` |
| E21 | T5 | `SoclaasClientTest.statusClasses` |
| E22 | T5, T6 | `SoclaasClientTest.timeout`, `LlmGatewayTest.deadline` |
| E23 | T5 | `SoclaasClientTest.connectionRefused` |
| E24 | T3 | `OutputValidatorTest.unusableBodies` |
| E25 | T3 | `OutputValidatorTest.leakAndLinks` |
| E26 | T3 | `OutputValidatorTest.refusal` |
| E27 | T8 | recorded live check in the branch state |
| E28 | T6 | `LlmGatewayIntegrationTest.auditRows` |
| E29 | T6 | `LlmGatewayIntegrationTest.noMarkersInRows` |
| E30 | T2, T6 | `AiAuditWriterTest.writerFailures`, `LlmGatewayTest.auditFailed` |
| E31 | T2, T6 | `AiAuditWriterTest.coalescing`, `LlmGatewayIntegrationTest.unauditedCodes` |
| E32 | T6 | `LlmGatewayTest.disabled` |
| E33 | T6 | `LlmGatewayTest.mockMode` |
| E34 | T1 | `LlmPropertiesTest`, `ProdEnvironmentGuardAiTest`, `ProdProfileTest` |
| E35 | T1, T7 | `LlmPropertiesTest.toStringHidesKey`, `AiLogLeakTest` |
| E36 | T6 | `LlmGatewayIntegrationTest.refusesInsideTransaction` |
| E37 | T5 | `SoclaasClientTest.requestShape` |
| E38 | T6 | `ModelCheckTest` |
| E39 | T7 | `AiHealthTest`, `AiDatasourceFreeContextTest` |
| E40 | T7 | `SecretScanTest` |
| E41 | T3, T6 | `OutputValidatorTest.leakAndLinks`, `LlmGatewayTest.obedientModel` |
| E42 | T3 | `InjectionDetectorTest`, `PromptBuilderTest.untrustedContextTags` |
| E43 | T6 | `LlmGatewayTest.floodDoesNotStarveOthers` |
| E44 | T6, T7 | `LlmGatewayTest.keyNeverInRequestBody`, `AiLogLeakTest` |
| E45 | T7 | runbook section for role authors; each role spec's own test |
| E46 | T5 | `SoclaasClientTest.errorBodyNeverLogged` |
| E47 | T2, T4 | `AiAuditWriterTest.utcDay`, `BudgetBlockTest.utcBoundary` |
| E48 | T3 | `PromptBuilderTest.oversizedContexts` |
| E49 | T6 | `LlmGatewayTest.pipelineOrder` |
| E50 | T6 | `LlmGatewayTest.refusalAfterSendKeepsLastCode` |
| E51 | T4, T6 | `LlmSchedulerTest.interrupt`, `LlmGatewayTest.deadlineDuringBackoff` |
| E52 | T3 | `InputSanitizerTest.assistantTurns` |
| E53 | T4, T6 | `UserThrottleTest.inFlight`, `LlmGatewayTest.inFlightLimit` |
| E54 | T6 | `LlmGatewayTest.throwingFallbackPropagates` |
| E55 | T4 | `SchedulerCoreTest.priorityZeroReserve`, `AiAuditWriterTest.globalReserve` |
| E56 | T2 | `AiAuditWriterTest.countFailureFailsClosed` |
| E57 | T4, T6 | `CircuitBreakerTest.probes`, `LlmGatewayTest.retryRechecksBreaker` |
| E58 | T3 | `InputSanitizerTest.unclosedTagAndCutCard` |
| E59 | T2 | `AiAuditWriterTest.failedAuditStaysCounted` |
| E60 | T5 | `SoclaasClientTest.bodySizeCaps` |

## Verification gates

- Per task: `cd backend; .\gradlew test --tests "com.snoozeshare.ai.*"` for the touched classes, then `cd backend; .\gradlew checkstyleMain checkstyleTest` as a separate run.
- Before finishing: `cd backend; .\gradlew build` green with Docker running and no `SOCLAAS_*` variable set; each spec F and N acceptance criterion has a passing test; `lint.mjs plan` passes.

---

### T1: Core types and configuration
**Delivers:** the `ai` types, validated `LlmProperties`, the feature registry, the prod guard rules, the deployment variable | **Satisfies:** F13, N1 | **Depends on:** - | **Wave:** 1

**Files**
| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/ai/ModelTier.java` | enum `FAST`, `STANDARD`, `STRONG` |
| Create | `backend/src/main/java/com/snoozeshare/ai/Turn.java` | record `Turn(TurnRole role, String text)`; `TurnRole.java` holds enum `USER`, `ASSISTANT` |
| Create | `backend/src/main/java/com/snoozeshare/ai/TurnRole.java` | the enum |
| Create | `backend/src/main/java/com/snoozeshare/ai/AuditContext.java` | record with `ticketId`, `bookingId`, `subjectUserId` and constant `NONE` |
| Create | `backend/src/main/java/com/snoozeshare/ai/LlmCall.java` | record per api.md Inputs; `checkShape()` throws `IllegalArgumentException` for every shape error of api.md step 1 |
| Create | `backend/src/main/java/com/snoozeshare/ai/LlmErrorCode.java` | the 19 codes of api.md § Error codes |
| Create | `backend/src/main/java/com/snoozeshare/ai/LlmResult.java` | `LlmResult<T>(ok, value, code, flagged, truncated, callId)` with `ok(...)` and `degraded(...)` factories |
| Create | `backend/src/main/java/com/snoozeshare/ai/OutputSpec.java` | `text(maxOutputChars, allowLinks)` and `json(Class<T>, allowLinks)` |
| Create | `backend/src/main/java/com/snoozeshare/ai/AiFeature.java` | record per api.md § AiFeature with `validate(LlmProperties)` |
| Create | `backend/src/main/java/com/snoozeshare/ai/AiFeatureRegistry.java` | a `@Component` with no database dependency (so it exists in datasource-free contexts); built from `ObjectProvider<AiFeature<?>>`, validates each feature, rejects duplicate ids, accepts zero features |
| Create | `backend/src/main/java/com/snoozeshare/ai/config/LlmProperties.java` | validated `@ConfigurationProperties("snoozeshare.ai")` record; every component carries `@DefaultValue` with the default of api.md § Configuration, so contexts that set only some keys still bind; ranges checked in the compact constructor |
| Create | `backend/src/main/java/com/snoozeshare/ai/config/AiConfig.java` | `@Configuration` with `@EnableConfigurationProperties(LlmProperties.class)` |
| Modify | `backend/src/main/java/com/snoozeshare/ProdEnvironmentGuard.java` (`REQUIRED_KEYS`, `postProcessBeanFactory`) | add the AI rules below |
| Modify | `backend/src/main/resources/application-dev.yml` | `snoozeshare.ai.enabled: ${SNOOZESHARE_AI_ENABLED:false}`, `mock: ${SNOOZESHARE_AI_MOCK:false}`, `base-url: ${SOCLAAS_BASE_URL:}`, `api-key: ${SOCLAAS_API_KEY:}`, and the three model keys with their default model names |
| Modify | `backend/src/main/resources/application-prod.yml` | `enabled: ${SNOOZESHARE_AI_ENABLED}` with no default; `mock: ${SNOOZESHARE_AI_MOCK:false}`; `base-url: ${SOCLAAS_BASE_URL:}`; `api-key: ${SOCLAAS_API_KEY:}`; model variables with their defaults |
| Modify | `render.yaml` (`envVars`) | `SNOOZESHARE_AI_ENABLED` value `"false"`; `SOCLAAS_BASE_URL` and `SOCLAAS_API_KEY` as `sync: false` |
| Modify | `backend/src/test/java/com/snoozeshare/ProdProfileTest.java` (both argument lists) | add `--SNOOZESHARE_AI_ENABLED=false`; add `missingAiEnabled` asserting the message contains `SNOOZESHARE_AI_ENABLED` |
| Test | `backend/src/test/java/com/snoozeshare/ai/LlmCallTest.java` | E4 |
| Test | `backend/src/test/java/com/snoozeshare/ai/config/LlmPropertiesTest.java` | E34 range and URL cases, E35 `toString` |
| Test | `backend/src/test/java/com/snoozeshare/ai/AiFeatureRegistryTest.java` | E10, E11 with `ApplicationContextRunner` |
| Test | `backend/src/test/java/com/snoozeshare/ai/config/ProdEnvironmentGuardAiTest.java` | E34 prod cases through `MockEnvironment` |

**Checklist**
- [x] 1. Write failing `LlmCallTest.shapeErrors` (null feature, `userId`, `role`, `turns`, null element or text, empty list, last turn ASSISTANT each throw `IllegalArgumentException`) and `LlmPropertiesTest` (each invalid value of E34 names its key; `toString` prints `***` for the key; a value containing `${` counts as blank). Run `.\gradlew test --tests "com.snoozeshare.ai.*"`; expect FAIL because the classes do not exist.
- [x] 2. Create the types listed above; `LlmCall.checkShape()` implements api.md step 1.
- [x] 3. Create `LlmProperties`; do range and cross-field checks in the compact constructor with a helper that throws `IllegalArgumentException("snoozeshare.ai.<key> ...")`; override `toString`.
- [x] 4. Create `AiFeature.validate` and `AiFeatureRegistry`; write `AiFeatureRegistryTest` first: SYSTEM or empty role set, bad id, `maxTokens` 0, non-record JSON class, `maxInputChars x (maxTurns + 1) + 15000` above `max-prompt-chars`, duplicate id (all fail startup naming the feature id and the field), no beans (succeeds).
- [x] 5. Add the guard rules and YAML below; write `ProdEnvironmentGuardAiTest`: unset enabled names `SNOOZESHARE_AI_ENABLED`; enabled with blank key or base URL names the key; `mock=true` fails with enabled true and false; disabled with no key passes; prod base URL must start `https://`.
- [x] 6. Update `ProdProfileTest`, `render.yaml`; run `.\gradlew test --tests "com.snoozeshare.ai.*"` and, as a second invocation, `.\gradlew test --tests "com.snoozeshare.ProdProfileTest"`; expect PASS.
- [x] 7. Run `.\gradlew checkstyleMain checkstyleTest` as its own invocation; expect no violations.
- [x] 8. Run `.\gradlew build`; expect green.
- [x] 9. Update the branch state: Progress `Task 1/8 done`, a Changes line.
- [x] 10. Commit: `feat: add AI gateway types, validated configuration and prod guard rules (W-AI-GATEWAY)`.

Intended shape of the guard additions (the implementer keeps them test-driven):
```java
// REQUIRED_KEYS gains "snoozeshare.ai.enabled"
boolean aiEnabled = Boolean.parseBoolean(environment.getRequiredProperty("snoozeshare.ai.enabled"));
if (Boolean.parseBoolean(environment.getProperty("snoozeshare.ai.mock", "false"))) {
    throw new IllegalStateException("snoozeshare.ai.mock must be false in the prod profile");
}
if (aiEnabled) {
    requireNotBlank("snoozeshare.ai.base-url");   // blank, or contains "${", is missing
    requireNotBlank("snoozeshare.ai.api-key");
    if (!environment.getProperty("snoozeshare.ai.base-url").startsWith("https://")) {
        throw new IllegalStateException("snoozeshare.ai.base-url must be https in the prod profile");
    }
}
```

### T2: Audit actions and writer
**Delivers:** the two audit actions and `AiAuditWriter` | **Satisfies:** F7, F11, N6 | **Depends on:** T1 | **Wave:** 2

**Files**
| Action | Path | What changes |
|---|---|---|
| Modify | `backend/src/main/java/com/snoozeshare/domain/enums/AuditAction.java` | add `AI_CALL` and `AI_CALL_REJECTED` |
| Modify | `backend/src/main/java/com/snoozeshare/repository/AuditLogRepository.java` | add `long countByActorUserIdAndActionTypeAndTimestampGreaterThanEqual(UUID, String, Instant)` and `long countByActionTypeAndTimestampGreaterThanEqual(String, Instant)` |
| Create | `backend/src/main/java/com/snoozeshare/ai/audit/AiAuditWriter.java` | row writing, caps with reservations, R-row coalescing (data-model.md) |
| Create | `backend/src/test/java/com/snoozeshare/ai/testsupport/AiTestUsers.java` | helper that saves and commits real `User` rows (the audit table has foreign keys to `users`) and deletes them and their audit rows afterwards |
| Test | `backend/src/test/java/com/snoozeshare/ai/audit/AiAuditWriterTest.java` | E13, E30, E31, E47, E55 (global reserve), E56, E59 |

**Checklist**
- [x] 1. Write failing `AiAuditWriterTest` on a Postgres Testcontainer. Reuse the container setup of `AuditLogRepositoryTest` but mark the class `@Transactional(propagation = Propagation.NOT_SUPPORTED)` so seed rows are committed and visible to worker threads, create every actor with `AiTestUsers`, and delete the rows and users in `@AfterEach` (the global count spans all users). Build `AiAuditWriter` with a real `AuditServiceImpl(entries, users, clock)`; for the failure cases wrap `AuditService` or the repository in a Mockito spy that throws: `capsUnderParallelBurst` (195 `AI_CALL` rows today, 50 yesterday, 30 R rows, 10 threads released together: exactly 5 reserve), `utcDay` (Singapore-zone clock at 23:59:59 UTC and 00:00:01 UTC), `globalReserve` (priority 2 stops at 80 percent of the global cap, priority 0 runs to 100), `countFailureFailsClosed`. Expect FAIL.
- [x] 2. Add the enum constants and the two count methods; run `AuditLogRepositoryTest` to confirm nothing else breaks.
- [x] 3. Implement `reserve(UUID userId, int priority)` per the snippet and `release(UUID userId, boolean keepCounted)`.
- [x] 4. Implement `write(...)`: build the metadata line of data-model.md, call `AuditServiceImpl`-backed `AuditService.record` through `AuditRecord.builder(...).status(null, metadata).reason(code).subject(...).booking(...).ticket(...).build()`; return false when it throws.
- [x] 5. Implement R-row coalescing (one row per user and code per 10 minutes, kept in a map cleaned on each call).
- [x] 6. Write and pass `writerFailures` (writer throws, a nonexistent ticket id), `coalescing` (20 DAILY_CAP refusals give one row), `noPromptText` (marker strings absent from every column) and `failedAuditStaysCounted`.
- [x] 7. Run the test command, then Checkstyle separately; expect PASS and no violations.
- [x] 8. Run `.\gradlew build`; expect green.
- [x] 9. Update the branch state: Progress, a Changes line.
- [x] 10. Commit: `feat: add AI audit actions, cap counts and audit writer (W-AI-GATEWAY)`.

```java
// AiAuditWriter.reserve - one lock across the reads and the reservation (spec api.md step 8)
public CapResult reserve(UUID userId, int priority) {
    synchronized (lock) {
        int reservedUser = reservedByUser.getOrDefault(userId, 0) + pinnedByUser.getOrDefault(userId, 0);
        int reservedAll = reservedGlobal + pinnedGlobal;          // read reservations first
        try {
            Instant dayStart = clock.instant().atZone(ZoneOffset.UTC).toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant();
            long userRows = entries.countByActorUserIdAndActionTypeAndTimestampGreaterThanEqual(userId, "AI_CALL", dayStart);
            long allRows = entries.countByActionTypeAndTimestampGreaterThanEqual("AI_CALL", dayStart);
            long globalLimit = priority == 0 ? props.globalDailyCap()
                    : props.globalDailyCap() * (100 - props.priorityZeroReservePercent()) / 100;
            if (userRows + reservedUser >= props.dailyCapPerUser() || allRows + reservedAll >= globalLimit) {
                return CapResult.DAILY_CAP;
            }
        } catch (RuntimeException e) {
            return CapResult.COUNT_FAILED;                          // caller returns AUDIT_FAILED
        }
        reservedByUser.merge(userId, 1, Integer::sum);
        reservedGlobal++;
        return CapResult.RESERVED;
    }
}
```

### T3: Guardrails
**Delivers:** sanitizing, PII redaction, injection flag, prompt assembly and output validation as pure classes | **Satisfies:** F3, F4, F5, F10 | **Depends on:** T1 | **Wave:** 2

**Files**
| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/ai/guard/InputSanitizer.java` | the seven ordered steps of conventions.md § Sanitizing |
| Create | `backend/src/main/java/com/snoozeshare/ai/guard/PiiRedactor.java` | card, NRIC, phone, email in that order |
| Create | `backend/src/main/java/com/snoozeshare/ai/guard/InjectionDetector.java` | the flag patterns, dot-all |
| Create | `backend/src/main/java/com/snoozeshare/ai/guard/PromptBuilder.java` | layout of conventions.md § Prompt layout; turn dropping rules of api.md step 7; `promptHash` |
| Create | `backend/src/main/java/com/snoozeshare/ai/guard/OutputValidator.java` | the five ordered steps of conventions.md § Validating model output |
| Test | `backend/src/test/java/com/snoozeshare/ai/guard/InputSanitizerTest.java` | E1, E2, E6 (pathological), E52, E58 |
| Test | `backend/src/test/java/com/snoozeshare/ai/guard/PiiRedactorTest.java` | E6 |
| Test | `backend/src/test/java/com/snoozeshare/ai/guard/InjectionDetectorTest.java` | E7, E42 |
| Test | `backend/src/test/java/com/snoozeshare/ai/guard/PromptBuilderTest.java` | E3, E5, E8, E42 (untrusted context tags), E48 |
| Test | `backend/src/test/java/com/snoozeshare/ai/guard/OutputValidatorTest.java` | E24, E25, E26, E41 (validator side) |

**Checklist**
- [x] 1. Write failing `InputSanitizerTest`, `PiiRedactorTest`, `InjectionDetectorTest`: every scenario of E1, E2 (all six tag variants), E6 (including `6011 1111 1111 1111` and 100,000 characters of `a@a@a@` under 200 ms), E7 (including the benign "you are now confirmed"), E52, E58. Expect FAIL.
- [x] 2. Implement `InputSanitizer`, `PiiRedactor`, `InjectionDetector`; run those tests; expect PASS.
- [x] 3. Write failing `PromptBuilderTest`: `layout` (exact message list), `keepsLastTurnsAndStartsWithUser`, `blankLastUserTurn`, `untrustedContextTags`, `oversizedContexts`, and a golden `promptHash` for a fixed prompt.
- [x] 4. Implement `PromptBuilder`; expect PASS.
- [x] 5. Write failing `OutputValidatorTest`: `unusableBodies`, `leakAndLinks` (including the `\u`-escaped link, tag and key inside a JSON string and the fullwidth link), `refusal` for text and JSON features.
- [x] 6. Implement `OutputValidator`; expect PASS.
- [x] 7. Run Checkstyle separately; expect no violations.
- [x] 8. Run `.\gradlew build`; expect green.
- [x] 9. Update the branch state: Progress, a Changes line.
- [x] 10. Commit: `feat: add AI input and output guardrails (W-AI-GATEWAY)`.

```java
// InputSanitizer.sanitize - order is the contract (conventions.md)
String t = Normalizer.normalize(raw, Normalizer.Form.NFKC);
t = CONTROL_AND_FORMAT.matcher(t).replaceAll("");                 // keeps \t and \n
boolean cut = false;
int pre = limit * 4;
if (t.codePointCount(0, t.length()) > pre) { t = t.substring(0, t.offsetByCodePoints(0, pre)); cut = true;
    t = TRAILING_DIGITS_SPACES_HYPHENS.matcher(t).replaceFirst(""); }
if (redactPii) t = PiiRedactor.redact(t);                         // card, NRIC, phone, email
String prev; do { prev = t; t = DELIMITER_TAG.matcher(t).replaceAll(""); } while (!t.equals(prev));
t = t.strip();
if (t.codePointCount(0, t.length()) > limit) { t = t.substring(0, t.offsetByCodePoints(0, limit)); cut = true; }
return new Sanitized(t, cut);
```

### T4: Limits and resilience
**Delivers:** the scheduler, throttle, breaker, budget block and backoff | **Satisfies:** F6, F7, F9, N3 | **Depends on:** T1 | **Wave:** 2

**Files**
| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/SchedulerCore.java` | pure state machine: offer, poll, release, displacement, rolling window, priority-0 reserve |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/LlmScheduler.java` | blocking wrapper (lock, condition) over `SchedulerCore`; `acquire` returns a `Permit` or throws `SchedulerRejection` |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/UserThrottle.java` | per user and feature rolling minute window; per user in-flight count |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/CircuitBreaker.java` | one streak (count and user set), half-open single probe |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/BudgetBlock.java` | block for `budget-block-ms` capped at the next 00:00 UTC, single probe |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/Backoff.java` | `min(8000, 500 x 2^n)` times a clamped jitter factor; `Retry-After` handling |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/Sleeper.java` | seam with a system implementation |
| Create | `backend/src/main/java/com/snoozeshare/ai/limit/Jitter.java` | seam returning a factor clamped to [0.5, 1.0] |
| Test | `backend/src/test/java/com/snoozeshare/ai/limit/SchedulerCoreTest.java` | E14, E15, E16, E17, E55 |
| Test | `backend/src/test/java/com/snoozeshare/ai/limit/LlmSchedulerTest.java` | blocking wait, wake-up, E51 interrupt |
| Test | `backend/src/test/java/com/snoozeshare/ai/limit/UserThrottleTest.java` | E12, E53 |
| Test | `backend/src/test/java/com/snoozeshare/ai/limit/CircuitBreakerTest.java` | E20, E57 |
| Test | `backend/src/test/java/com/snoozeshare/ai/limit/BudgetBlockTest.java` | E19, E47 |
| Test | `backend/src/test/java/com/snoozeshare/ai/limit/BackoffTest.java` | E18 arithmetic |

**Checklist**
- [x] 1. Write failing `SchedulerCoreTest` on a fake clock: `ceilingsAndPriority` (200 jobs, `max-rpm` 55, `max-concurrent` 4; windows of (t-60 s, t]; priority 0 first; priority 1 and 2 use at most 3 slots), `queueFull`, `queueTimeout`, `retriesKeepArrival`, `priorityZeroReserve` (a full queue and a priority 0 job displaces the youngest priority 2 job). Expect FAIL.
- [x] 2. Implement `SchedulerCore` per the snippet; expect PASS.
- [x] 3. Write failing `UserThrottleTest` (E12: 6 at t, 3 refused at t+10 s consuming nothing, refused at t+59.999 s, allowed at t+60 s; E53 in-flight limit 2 of 5 parallel and release after an exception) and `BackoffTest` (500 and 1,000 ms at jitter 1.0; `Retry-After` 30 capped at 10 s; HTTP-date ignored; jitter clamped).
- [x] 4. Implement `UserThrottle`, `Backoff`, `Sleeper`, `Jitter`; expect PASS.
- [x] 5. Write failing `CircuitBreakerTest` (one user never opens; two users keep counting; a third opens at once; one probe; probe success closes, failure re-opens; a response below 500 resets count and users; `probes`) and `BudgetBlockTest` (block, repeat on a second budget 429, probe after the window, cap at 00:00 UTC, `utcBoundary` with a Singapore-zone clock).
- [x] 6. Implement `CircuitBreaker`, `BudgetBlock`; expect PASS.
- [x] 7. Write `LlmSchedulerTest` (a waiting job wakes when a permit is released; an interrupt restores the flag and releases state), implement `LlmScheduler`; expect PASS.
- [x] 8. Run Checkstyle separately, then `.\gradlew build`; expect green.
- [x] 9. Update the branch state: Progress, a Changes line.
- [x] 10. Commit: `feat: add AI scheduler, user throttle, circuit breaker, budget block and backoff (W-AI-GATEWAY)`.

```java
// SchedulerCore.canStart - the contract the tests pin
boolean canStart(Job job, Instant now) {
    starts.removeIf(t -> !t.isAfter(now.minusSeconds(60)));       // window is (now-60s, now]
    int cap = job.priority() == 0 ? maxConcurrent : maxConcurrent - 1;
    return running < cap && starts.size() < maxRpm;
}
// offer: queue.size() >= maxQueue -> if job.priority()==0 and a waiting job has priority >= 1,
// remove the youngest such job (its caller gets QUEUE_FULL) and queue this one; else QUEUE_FULL.
```

### T5: SoCLaaS client
**Delivers:** one HTTP attempt with classified result | **Satisfies:** F8, F9, N1 | **Depends on:** T1 | **Wave:** 2

**Files**
| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/ai/client/SoclaasClient.java` | one shared `HttpClient` (HTTP/1.1, `Redirect.NEVER`), `attempt(request, timeout)`, `listModels(timeout)` |
| Create | `backend/src/main/java/com/snoozeshare/ai/client/AttemptResult.java` | `Success(content, finishReason, tokensIn, tokensOut)` or `Failure(code, retryAfterSeconds, budget)` |
| Create | `backend/src/test/java/com/snoozeshare/ai/testsupport/StubSoclaasServer.java` | in-process `com.sun.net.httpserver.HttpServer` with scripted responses and request capture |
| Test | `backend/src/test/java/com/snoozeshare/ai/client/SoclaasClientTest.java` | E21, E22 (client side), E23, E37, E46, E60, 429 classification of E18 and E19 |

**Checklist**
- [x] 1. Create `StubSoclaasServer` (scripted status, headers, body, delay; captures method, path, headers, body).
- [x] 2. Write failing `SoclaasClientTest`: `requestShape` (Bearer header, tier model, `max_tokens`, `temperature`, no `stream`, `response_format` and `reasoning_effort` only when set, key absent from the body, HTTP/1.1), `statusClasses` (400, 401, 403, 404, 409, 413, 422, 204, a 302 to a second stub that must receive nothing), `connectionRefused`, `timeout`, `bodySizeCaps` (1 MiB 429 body classified from its first 4,096 bytes; 300,000-byte 200 body is INVALID_OUTPUT), `errorBodyNeverLogged`, and the 429 rule: `{"error":"Daily quota exceeded"}` budget; "rate limit exceeded", "separate generated text", non-JSON, empty are RATE_LIMITED; `Retry-After` in seconds parsed, an HTTP-date ignored. Expect FAIL.
- [x] 3. Implement `SoclaasClient.attempt`: build the JSON body with Jackson, send with `HttpRequest.timeout`, read at most 262,144 bytes, classify per api.md § Error codes.
- [x] 4. Implement `listModels` reading `data[].id`.
- [x] 5. Run the tests; expect PASS.
- [x] 6. Run Checkstyle separately; expect no violations.
- [x] 7. Run `.\gradlew build`; expect green.
- [x] 8. Update the branch state: Progress, a Changes line.
- [x] 9. Commit: `feat: add the SoCLaaS client with response classification (W-AI-GATEWAY)`.

```java
// classification of a non-2xx response (api.md § Error codes)
return switch (status) {
    case 401 -> failure(AUTH);
    case 403 -> failure(FORBIDDEN);
    case 429 -> isBudget(first200(errorText(body))) ? budgetFailure() : rateLimited(retryAfterSeconds(headers));
    default -> status >= 500 ? failure(SERVER) : failure(BAD_REQUEST);   // 3xx and other 4xx
};
// 2xx other than 200 -> INVALID_OUTPUT
```

### T6: Gateway facade
**Delivers:** `LlmGateway.run` composing T2 to T5 | **Satisfies:** F1, F2, F8, F9, F10, F11, F12, F14, N2, N4 | **Depends on:** T2, T3, T4, T5 | **Wave:** 3

**Files**
| Action | Path | What changes |
|---|---|---|
| Create | `backend/src/main/java/com/snoozeshare/ai/LlmGateway.java` | the api.md pipeline, steps 0 to 14, with the attempt loop |
| Create | `backend/src/main/java/com/snoozeshare/ai/config/ModelCheck.java` | `ApplicationReadyEvent` listener running `GET /models` on an injectable `Executor` |
| Modify | `backend/src/main/java/com/snoozeshare/ai/config/AiConfig.java` | `@Bean` methods for the classes that need no database (guard classes, limit classes, `Sleeper`, `Jitter`, `SoclaasClient`, the `ModelCheck` executor); `LlmGateway`, `AiAuditWriter` and `ModelCheck` are `@Component` classes that each carry `@ConditionalOnBean(PropertyRepository.class)` themselves, the pattern the W-DOMAIN services use |
| Modify | `backend/src/test/java/com/snoozeshare/PostgresIntegrationTest.java` | make the class `public` so tests in `com.snoozeshare.ai` can extend it (the `POSTGRES` field is already `protected static`) |
| Test | `backend/src/test/java/com/snoozeshare/ai/LlmGatewayTest.java` | E9, E18, E19, E20, E22, E30, E32, E33, E41, E43, E44, E49, E50, E51, E53, E54, E57 with fakes and the stub server |
| Test | `backend/src/test/java/com/snoozeshare/ai/LlmGatewayIntegrationTest.java` | E15, E16 (rows), E28, E29, E31, E36 on `PostgresIntegrationTest` (not `@Transactional`; actors from `AiTestUsers`, committed and cleaned up) |
| Test | `backend/src/test/java/com/snoozeshare/ai/config/ModelCheckTest.java` | E38 |

**Checklist**
- [x] 1. Write failing `LlmGatewayTest` with fake limit classes, the stub server, a recording `Sleeper` and a fixed `Jitter`: `pipelineOrder` (the six pairs of E49), `roleDenied`, `disabled`, `mockMode` (steps 10 to 12 skipped, `model=mock`), `retryMatrix` (SERVER three times; 429 then success sets `attempts` 2 and resets the breaker), `budgetBlock`, `breakerOpens`, `retryRechecksBreaker`, `refusalAfterSendKeepsLastCode`, `deadline`, `deadlineDuringBackoff`, `floodDoesNotStarveOthers`, `inFlightLimit`, `keyNeverInRequestBody`, `obedientModel`, `auditFailed`, `throwingFallbackPropagates`. Expect FAIL.
- [x] 2. Implement `LlmGateway.run` step by step per the snippet; every checkpoint returns through one `finish(...)` that validates, audits and releases reservations in a `finally`.
- [x] 3. Implement the attempt loop (scheduler permit, breaker and budget re-check, send, classify, backoff through `Sleeper`, deadline arithmetic, interrupt handling, probes get one attempt).
- [x] 4. Run `LlmGatewayTest`; expect PASS.
- [x] 5. Write failing `LlmGatewayIntegrationTest`: `auditRows` (one row per A and R outcome with fresh user ids), `noMarkersInRows`, `unauditedCodes` (no row, non-null `callId`), `rejectedRows`, `refusesInsideTransaction` (a `TransactionTemplate` caller gets `IllegalStateException`, also when disabled). Implement `ModelCheck` with `ModelCheckTest` (missing model WARN, failing `/models` WARN, direct executor). Expect PASS after wiring `AiConfig`.
- [x] 6. Confirm `ServerPortDefaultTest` (datasource-free context) still passes: the gateway beans are absent there.
- [x] 7. Run Checkstyle separately; expect no violations.
- [x] 8. Run `.\gradlew build`; expect green.
- [x] 9. Update the branch state: Progress, a Changes line.
- [x] 10. Commit: `feat: add the AI gateway pipeline and startup model check (W-AI-GATEWAY)`.

```java
// LlmGateway.run - skeleton of the pipeline (api.md); each early return goes through finish()
public <T> LlmResult<T> run(AiFeature<T> feature, LlmCall call) {
    UUID callId = UUID.randomUUID(); Instant start = clock.instant();            // step 0
    call.checkShape(); requireFeature(feature);                                  // 1
    if (TransactionSynchronizationManager.isActualTransactionActive()) throw new IllegalStateException(...); // 2
    if (!props.enabled()) return degraded(feature, call, DISABLED, callId);      // 3
    if (!feature.allowedRoles().contains(call.role())) return degraded(..., ROLE_DENIED, callId); // 4
    if (!throttle.tryAcquire(call.userId(), feature.id(), feature.perUserPerMinute(), start)) return degraded(..., USER_THROTTLED, callId); // 5
    if (!throttle.tryAcquireInFlight(call.userId(), props.perUserMaxInFlight())) return degraded(..., USER_THROTTLED, callId);              // 6
    try {
        Prepared p = promptBuilder.build(feature, call);                        // 7 (BLOCKED_INPUT -> R row)
        CapResult cap = writer.reserve(call.userId(), feature.priority());      // 8
        ...                                                                      // 9 to 14
    } finally { throttle.releaseInFlight(call.userId()); }
}
```

### T7: Integration tests, boundary test and docs
**Delivers:** end-to-end, health, datasource-free, boundary and secret-scan tests, and the runbook section | **Satisfies:** N1, N2, N3, N4, N5, N6 | **Depends on:** T6 | **Wave:** 4

**Files**
| Action | Path | What changes |
|---|---|---|
| Test | `backend/src/test/java/com/snoozeshare/ai/AiBoundaryTest.java` | source scan: no import of `ai.config`, `ai.guard`, `ai.limit`, `ai.client` or `ai.audit` outside `com.snoozeshare.ai`; no `java.net.http` or `chat/completions` outside `ai/client` |
| Test | `backend/src/test/java/com/snoozeshare/ai/SecretScanTest.java` | E40: regex `clsk_[A-Za-z0-9]{8}_[A-Za-z0-9]{16,}` over the files from `git -C <repository root> ls-files` (Gradle's working directory is `backend/`, so the root is `..`); the test builds its sample key at run time from fragments so its own source never matches |
| Test | `backend/src/test/java/com/snoozeshare/ai/AiHealthTest.java` | E39 health body with AI disabled and with an unreachable base URL, on `PostgresIntegrationTest` |
| Test | `backend/src/test/java/com/snoozeshare/ai/AiDatasourceFreeContextTest.java` | E39 datasource-free context starts with no gateway bean (mirrors `ServerPortDefaultTest`) |
| Test | `backend/src/test/java/com/snoozeshare/ai/AiLogLeakTest.java` | E35 and E44 end to end: captured logs of every failure scenario hold no key, marker or exception message |
| Modify | `docs/runbooks/dev-environment.md` (`## Variables`) | add the AI variables and an "AI gateway" section: variables, the dev and prod key split, how to run with mock, the rules role authors must follow (E45) |

**Checklist**
- [x] 1. Write `AiBoundaryTest` (reads each file with `Files.readString`, splits on any line break) and confirm it passes on the current tree; add a deliberately violating temporary file in the test to prove it fails, then remove it.
- [x] 2. Write `SecretScanTest`; confirm it passes and that a planted key-shaped string in a temp file makes the matcher report it.
- [x] 3. Write `AiHealthTest` and `AiDatasourceFreeContextTest`; expect PASS.
- [x] 4. Write `AiLogLeakTest` with a log appender attached to the `com.snoozeshare.ai` logger across the failure scenarios; expect PASS.
- [x] 5. Update the runbook; keep the E45 rules (principal-sourced role, server-stored assistant turns, plain-text rendering, `flagged` handling, no calls inside transactions) as a numbered list.
- [x] 6. Run the whole test suite with no `SOCLAAS_*` variable set; expect PASS.
- [x] 7. Run Checkstyle separately; expect no violations.
- [x] 8. Run `.\gradlew build`; expect green.
- [x] 9. Update the branch state: Progress, a Changes line.
- [x] 10. Commit: `test: add AI gateway integration, boundary and secret-scan tests and runbook section (W-AI-GATEWAY)`.

### T8: Live verification
**Delivers:** the observed behaviour of the real SoCLaaS service recorded | **Satisfies:** E27 | **Depends on:** T7 | **Wave:** 5

**Files**
| Action | Path | What changes |
|---|---|---|
| Modify | `docs/runbooks/dev-environment.md` (`## Evidence`) | the recorded observations of E27 |

**Checklist**
- [ ] 1. GATE: in the Render dashboard set `SNOOZESHARE_AI_ENABLED`, `SOCLAAS_BASE_URL` and the development `SOCLAAS_API_KEY`; keep the production key out of every file.
- [ ] 2. GATE: from the development machine, run one chat call and `GET /models` with the key; record the model ids the key may use and whether `response_format` and `reasoning_effort` are accepted per configured model.
- [ ] 3. GATE: from the Render dev service, repeat one call and record whether SoCLaaS is reachable from there.
- [ ] 4. GATE: record whether a 429 carries `Retry-After`, the wording of a budget 429, and any concurrency limit seen; if a default must change, make a follow-up commit pinned by `LlmPropertiesTest`.
- [ ] 5. Write the observations into the runbook Evidence block and the branch state; role AI specs start only after this.

### Close-out
- [ ] GATE: before the merge, confirm the Render dev service has `SNOOZESHARE_AI_ENABLED=false` (a Blueprint re-sync or the dashboard), because the prod guard fails startup without it; record it as a Handover item in the branch state.
- [ ] Every Traceability row verified; `cd backend; .\gradlew build` green with no `SOCLAAS_*` variable set.
- [ ] Spec index status and the plan Task order reflect reality; amendments recorded in the branch state.
- [ ] Branch state: workstream `In review`, session rows removed, Changes lines complete, new Known Gaps, Conventions and Deviations written (the process-local, many-accounts and bare-domain gaps are already there), Handover items for T8 and for the W-AI-AGENT decisions, `guide out of date: the AI gateway` noted (the guide is not edited on a branch, `AGENTS.md` § 5).
- [ ] Ask the operator to review the branch (`AGENTS.md` § 8, gate 3); write `Approved` only on their yes, and never push, open a PR or merge before it.
