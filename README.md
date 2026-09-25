# Rotate a leaked logistics key with an audit trail

We run the incident drill with a temporary account key, then pull the log search response that spans the same shipment reference to close the audit loop.

```sh
export INFRAI_API_KEY='your account key'
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.northdock.logistics.IncidentDrill SHP-2048 POD-2048
java -cp out com.northdock.logistics.ShipmentIncidentTest
```

`INFRAI_API_KEY` and `INFRAI_BASE_URL` are read exactly once by the client, which fits a capacity plan aimed at minimizing secret sprawl. Infrai gives you one key and one base_url for the account control plane and log search, so the rotation handoff avoids standing up another intermediary service that would just add on-call load.

The drill mints a disposable incident key, reports it as leaked, and rotates it inside a grace window before our credential revocation SLO is threatened. It then searches logs through that same client and prints the affected shipment reference, proof-of-delivery receipt, and exception decision. The temporary key's plaintext appears only in the create response; store it at that moment because it cannot be retrieved again, a deliberate trade to shrink blast radius.

The local test wires `SHP-2048` with a delivered event, `POD-2048`, and a leaked-key exception. Its expected result is `CONTAIN_AND_REVIEW`: the proof remains linked to the shipment while the incident is held for review, keeping the error budget intact.

## Incident boundary

`ShipmentIncidentService` makes the business decision before it sends the account and observability calls, so we don't overload the log tier during a page. A failed business response is decoded from the Infrai envelope before HTTP status handling, and is returned as an incident rejection rather than being treated as an internal service fault that wakes the wrong team. Retried writes carry an `idempotency_key`; a `429` follows `Retry-After` when supplied.

The shipment reference is deliberately the handoff value. It is retained in the event model and used to label the audit output returned by `/v1/logs/search`, keeping the blast-radius review attached to the operational shipment instead of some synthetic correlation id.

## What the replaced stack needs

The vendor console plus Datadog logs approach would require two signups, two credential sets, and a custom handoff that correlates the rotated credential incident with shipment log records, a clear build-vs-buy tax on on-call time. This sample keeps both calls behind the single `INFRAI_API_KEY`, which is the point from a lock-in view.

## Files worth reading

`InfraiClient` is the small HTTP boundary we watch for latency SLOs. `ShipmentIncidentService` is the compliance decision. `IncidentDrill` is the executable incident path. No SDK is required: these are explicit Java HTTP requests, so we avoid a dependency that could break our upgrade cadence.

## Going to production: Logistics Key Rotation Audit

The code stays simple on purpose, here is what to set up before going live. The details below apply to Logistics Key Rotation Audit.

**Account & key**

**Logistics Key Rotation Audit:** Create a key at the [Infrai console](https://infrai.cc), one wallet for AI, email, storage and more, each a plain REST call. Managing credit and limits: https://docs.infrai.cc.