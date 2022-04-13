package edu.asu.mobile_offloading_master;

public class SecondaryDataModel {

    String identifier;
    int remainingPower;
    String secondaryId;
    double longitudeCoord;
    double latitudeCoord;

    public SecondaryDataModel() {

    }

    public SecondaryDataModel(String identifier, int remainingPower, String secondaryId, double longitudeCoord, double latitudeCoord) {
        this.identifier = identifier;
        this.remainingPower = remainingPower;
        this.secondaryId = secondaryId;
        this.longitudeCoord = longitudeCoord;
        this.latitudeCoord = latitudeCoord;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public int getRemainingPower() {
        return remainingPower;
    }

    public void setRemainingPower(int remainingPower) {
        this.remainingPower = remainingPower;
    }

    public String getSecondaryId() {
        return secondaryId;
    }

    public void setSecondaryId(String secondaryId) {
        this.secondaryId = secondaryId;
    }

    public double getLongitudeCoord() {
        return longitudeCoord;
    }

    public void setLongitudeCoord(double longitudeCoord) {
        this.longitudeCoord = longitudeCoord;
    }

    public double getLatitudeCoord() {
        return latitudeCoord;
    }

    public void setLatitudeCoord(double latitudeCoord) {
        this.latitudeCoord = latitudeCoord;
    }
}
