package com.northdock.logistics.domain;

public final class ShipmentIncident {
    public enum Decision { CONTAIN_AND_REVIEW, RECORD_ONLY }
    private final String shipmentId;
    private final String proofOfDeliveryId;
    private final boolean delivered;
    private final boolean keyLeakConfirmed;

    public ShipmentIncident(String shipmentId, String proofOfDeliveryId, boolean delivered, boolean keyLeakConfirmed) {
        this.shipmentId = shipmentId;
        this.proofOfDeliveryId = proofOfDeliveryId;
        this.delivered = delivered;
        this.keyLeakConfirmed = keyLeakConfirmed;
    }

    public String shipmentId() { return shipmentId; }
    public String proofOfDeliveryId() { return proofOfDeliveryId; }

    public Decision decision() {
        return delivered && keyLeakConfirmed ? Decision.CONTAIN_AND_REVIEW : Decision.RECORD_ONLY;
    }
}
