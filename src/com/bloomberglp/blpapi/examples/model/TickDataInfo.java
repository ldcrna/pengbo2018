package com.bloomberglp.blpapi.examples.model;

public class TickDataInfo {

    private  String time;
    private  String type;
    private  String value;
    private  String size;
    private  String conditionCodes;

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getConditionCodes() {
        return conditionCodes;
    }

    public void setConditionCodes(String conditionCodes) {
        this.conditionCodes = conditionCodes;
    }
}
