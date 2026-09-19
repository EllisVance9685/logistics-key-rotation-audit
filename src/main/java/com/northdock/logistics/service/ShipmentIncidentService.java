package com.northdock.logistics.service;

import com.northdock.logistics.client.InfraiClient;
import com.northdock.logistics.domain.ShipmentIncident;
import java.util.UUID;

public final class ShipmentIncidentService {
    private final InfraiClient infrai;

    public ShipmentIncidentService(InfraiClient infrai) { this.infrai = infrai; }

    public AuditResult contain(ShipmentIncident incident) {
        if (incident.decision() != ShipmentIncident.Decision.CONTAIN_AND_REVIEW) {
            return new AuditResult(incident.decision(), incident.shipmentId(), incident.proofOfDeliveryId(), "no credential action");
        }
        String correlation = UUID.randomUUID().toString();
        String created = infrai.createTemporaryKey("logistics-incident", "leak-drill-" + incident.shipmentId(),
                "account.keys.rotate,logs.search", correlation);
        String temporaryKeyId = InfraiClient.responseId(created);
        infrai.reportSuspectedCompromise(temporaryKeyId);
        infrai.rotateTemporaryKey(temporaryKeyId, 2, correlation + "-rotate");
        String logs = infrai.searchLogs();
        return new AuditResult(incident.decision(), incident.shipmentId(), incident.proofOfDeliveryId(), logs);
    }

    public static final class AuditResult {
        private final ShipmentIncident.Decision decision;
        private final String shipmentId;
        private final String proofOfDeliveryId;
        private final String logEnvelope;

        AuditResult(ShipmentIncident.Decision decision, String shipmentId, String proofOfDeliveryId, String logEnvelope) {
            this.decision = decision;
            this.shipmentId = shipmentId;
            this.proofOfDeliveryId = proofOfDeliveryId;
            this.logEnvelope = logEnvelope;
        }

        public ShipmentIncident.Decision decision() { return decision; }
        public String shipmentId() { return shipmentId; }
        public String proofOfDeliveryId() { return proofOfDeliveryId; }
        public String logEnvelope() { return logEnvelope; }
    }
}
