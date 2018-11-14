package com.bloomberglp.blpapi.examples.model;

import com.alibaba.fastjson.annotation.JSONField;

public class HistoryInfo {
    public String date;

    @JSONField(name="open")
    public String PX_OPEN;
    @JSONField(name="high")
    public String PX_HIGH;
    @JSONField(name="low")
    public String PX_LOW;
    @JSONField(name="close")
    public String PX_LAST;
    @JSONField(name="volume")
    public String PX_VOLUME;

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    @JSONField(name="open")
    public String getPX_OPEN() {
        return PX_OPEN;
    }

    public void setPX_OPEN(String PX_OPEN) {
        this.PX_OPEN = PX_OPEN;
    }
    @JSONField(name="high")
    public String getPX_HIGH() {
        return PX_HIGH;
    }

    public void setPX_HIGH(String PX_HIGH) {
        this.PX_HIGH = PX_HIGH;
    }
    @JSONField(name="low")
    public String getPX_LOW() {
        return PX_LOW;
    }

    public void setPX_LOW(String PX_LOW) {
        this.PX_LOW = PX_LOW;
    }
    @JSONField(name="close")
    public String getPX_LAST() {
        return PX_LAST;
    }

    public void setPX_LAST(String PX_LAST) {
        this.PX_LAST = PX_LAST;
    }
    @JSONField(name="volume")
    public String getPX_VOLUME() {
        return PX_VOLUME;
    }

    public void setPX_VOLUME(String PX_VOLUME) {
        this.PX_VOLUME = PX_VOLUME;
    }
}
