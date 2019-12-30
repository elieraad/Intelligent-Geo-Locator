package com.elieraad.seproject;

public class Result {

    private final String label;
    private final double proba;
    private String uri;

    public Result(String uri, String label, double proba) {
        this.uri = uri;
        this.label = label;
        this.proba = proba;
    }

    public String getLabel() {
        return label;
    }

    public double getProba() {
        return proba;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }
}
