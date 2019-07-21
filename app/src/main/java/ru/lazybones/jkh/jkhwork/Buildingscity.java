package ru.lazybones.jkh.jkhwork;

import java.util.HashMap;
import java.util.Map;

class Buildingscity {
    private String address;
    private String id;
    private String idmaster;
    private String idterritory;
    private String yearstart;
    private double lat;
    private double lng;

    public Buildingscity() {}

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIdmaster() {
        return idmaster;
    }

    public void setIdmaster(String idmaster) {
        this.idmaster = idmaster;
    }

    public String getIdterritory() {
        return idterritory;
    }

    public void setIdterritory(String idterritory) {
        this.idterritory = idterritory;
    }

    public String getYearstart() {
        return yearstart;
    }

    public void setYearstart(String yearstart) {
        this.yearstart = yearstart;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLng() {
        return lng;
    }

    public void setLng(double lng) {
        this.lng = lng;
    }


     public Map<String, Object> toMap() {
             HashMap<String, Object> result = new HashMap<>();
             result.put("address", address);
             result.put("id", id);
             result.put("idmaster", idmaster);
             result.put("idterritory", idterritory);
             result.put("yearstart", yearstart);
             result.put("lat", lat);
             result.put("lng", lng);

         return result;
     }



 }
