# Delivery Phases

## Phase status values

Use only: `not-started`, `in-progress`, `blocked`, `complete`.

## Phase 0 — Desktop lock listener

- **Objective and scope:** Windows-only TCP server on default 43298, packets 0xB3/0xB4, `LockWorkStation`, AppSettings `lockServerPort`. Linux/macOS unchanged. No Android UI yet beyond what tests need.
- **Dependencies:** Approved feature PRD, design, architecture, rules. Existing pairing store on desktop.
- **Deliverables:** Lock server bound while JarvisAuth.exe runs in the user session; unknown phone → `NOT_PAIRED`; success has no `passwordKey`. Installer firewall rule for 43298 if pairing/unlock already get one.
- **Verification steps:** Local TCP client (test) against a running desktop with a known pairing vector; Win+L-equivalent after a valid packet; 43296 still free for unlock.
- **Exit criteria:** Desktop accepts a valid lock packet and rejects a bad key without locking.
- **Current status:** `in-progress`

## Phase 1 — Android lock client

- **Objective and scope:** Persist `lastIpv4`, `LockPc` + `TcpLockClient`, Lock PC on PC detail per design. Fill IP from pair beacon, UDP broadcast, inbound TCP unlock.
- **Dependencies:** Phase 0 desktop (or a mock server for JVM tests; hardware needs Phase 0).
- **Deliverables:** Detail button, status copy, redacted logs, JVM tests. FGS unchanged.
- **Verification steps:** `test` + `ktlintCheck` + `assembleDebug`. Unit tests for missing IP, proto mismatch, no `passwordKey` in success parse.
- **Exit criteria:** FR-L1–L4, L8–L10 in code. No re-pair schema break (additive `lastIpv4`).
- **Current status:** `complete`

## Phase 2 — Hardware

- **Objective and scope:** Real phone + real Windows session. Existing pairing, one LAN unlock to learn IP, tap Lock PC.
- **Dependencies:** Phase 0 and 1.
- **Deliverables:** Manual sign-off listed in memory. No new surface area.
- **Verification steps:** AC-L1–L5. Unlock UDP or TCP still works. Quit desktop → reachability error.
- **Exit criteria:** Feature PRD success metrics 1–4 true on hardware.
- **Current status:** `not-started`

## Phase structure

Every phase above includes: objective and scope, dependencies, deliverables, verification steps, exit criteria, current status.

Do not start Phase N+1 until Phase N is `complete`, except JVM Android tests in Phase 1 may use a loopback fake server before desktop hardware exists. Phase 2 is still required before the feature is `complete`.
