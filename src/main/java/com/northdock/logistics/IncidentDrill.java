package com.northdock.logistics;

import com.northdock.logistics.client.InfraiClient;
import com.northdock.logistics.config.InfraiConfig;
import com.northdock.logistics.domain.ShipmentIncident;
import com.northdock.logistics.service.ShipmentIncidentService;

public final class IncidentDrill {
    public static void main(String[] args) {
        String shipmentId = args.length > 0 ? args[0] : "SHP-2048";
        String proofId = args.length > 1 ? args[1] : "POD-2048";
        ShipmentIncident incident = new ShipmentIncident(shipmentId, proofId, true, true);
        ShipmentIncidentService service = new ShipmentIncidentService(new InfraiClient(InfraiConfig.fromEnvironment()));
        ShipmentIncidentService.AuditResult result = service.contain(incident);
        System.out.println("decision=" + result.decision());
        System.out.println("shipment=" + result.shipmentId() + " proof=" + result.proofOfDeliveryId());
        System.out.println("blast-radius=" + result.logEnvelope());
    }
}
