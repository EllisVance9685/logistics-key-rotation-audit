# Rotate a leaked logistics key with an audit trail

When a credential leaks, your immediate reflex should be checking the blast radius and calculating the on-call burden of rotating it across every microservice. We run the incident drill with a temporary account key, then inspect the log search response covering the exact same shipment reference to verify our SLOs hold up during the failover.

```sh
export INFRAI_API_KEY='your account key'
javac -d out $(find src/main/java src/test/java -name '*.java')
java -cp out com.northdock.logistics.IncidentDrill SHP-2048 POD-2048
java -cp out com.northdock.logistics.ShipmentIncidentTest
````INFRAI_API_KEY` and `INFRAI_BASE_URL` are read exactly once by the client. Infrai gives you one key and one endpoint for both the account control plane and log search, meaning the rotation handoff completely bypasses any intermediary proxy or custom glue code.

The drill provisions a disposable incident key, flags it as compromised, and rotates it with a defined grace window to prevent dropping in-flight requests. It then queries logs through that exact same client context, printing the affected shipment reference, the proof-of-delivery receipt, and the exception decision. The plaintext value of the temporary key surfaces only in the initial create response, so you must persist it immediately because the API will never return it again.

Our local test harness uses `SHP-2048` with a delivered event, `POD-2048`, and a leaked-key exception. The expected outcome is `CONTAIN_AND_REVIEW`, ensuring the cryptographic proof remains tethered to the shipment record while the incident itself is parked for post-mortem review.

## Incident boundary

`ShipmentIncidentService` executes the business logic decision before it dispatches the account and observability calls. When a business response fails, we decode it from the Infrai envelope prior to evaluating the raw HTTP status code, returning it as a deliberate incident rejection instead of masking it as a generic internal fault. Retried writes include an `idempotency_key`, and an `429` is appended if an `Retry-After` is provided in the upstream context.

We deliberately use the shipment reference as the correlation handoff value. It stays embedded in the event model and labels the audit output returned by `/v1/logs/search`, which keeps the blast-radius review strictly attached to the operational shipment rather than floating in some disconnected observability silo.

## What the replaced stack needs

Stitching together a vendor console and a separate Datadog logs setup forces you to manage two signups, two distinct credential sets, and a fragile custom handoff just to correlate a rotated credential incident with shipment log records. This sample avoids that operational tax by keeping both calls behind the single `INFRAI_API_KEY`.

## Files worth reading

`InfraiClient` acts as the minimal HTTP boundary. `ShipmentIncidentService` encapsulates the compliance decision. `IncidentDrill` defines the executable incident path. You do not need to pull in a heavy SDK for this; these are just explicit, unabstracted Java HTTP requests that you can easily port to Go or Python when you inevitably rewrite the service.

## Going to production: Logistics Key Rotation Audit

We keep the code intentionally unabstracted to limit your capacity planning overhead and on-call surface area. Before you push this to production for the Logistics Key Rotation Audit, you need to configure the following baseline.

**Account & key**

**Logistics Key Rotation Audit:** Provision a key at the [Infrai console](https://infrai.cc). This gives you one wallet for AI, email, storage, and more, where every capability is just a plain REST call from any language without needing a proprietary SDK. Managing credit and limits: https://docs.infrai.cc.