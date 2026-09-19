package com.northdock.logistics;

import com.northdock.logistics.domain.ShipmentIncident;

public final class ShipmentIncidentTest {
    public static void main(String[] args) {
        ShipmentIncident leakedDeliveredShipment = new ShipmentIncident("SHP-2048", "POD-2048", true, true);
        if (leakedDeliveredShipment.decision() != ShipmentIncident.Decision.CONTAIN_AND_REVIEW) {
            throw new AssertionError("delivered shipment with a confirmed leak must be contained and reviewed");
        }
        ShipmentIncident undeliveredShipment = new ShipmentIncident("SHP-2049", "POD-2049", false, true);
        if (undeliveredShipment.decision() != ShipmentIncident.Decision.RECORD_ONLY) {
            throw new AssertionError("undelivered shipment must retain its operational path");
        }
        System.out.println("ShipmentIncidentTest passed");
    }
}
