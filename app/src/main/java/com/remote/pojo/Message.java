package com.remote.pojo;

import java.util.Map;

public class Message {

    private String type;

    private Map sdp;

    private Map candidate;

    private Map data;

    public Map getData() {
        return data;
    }

    public void setData(Map data) {
        this.data = data;
    }

    public Map getCandidate() {
        return candidate;
    }

    public void setCandidate(Map candidate) {
        this.candidate = candidate;
    }


    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Map getSdp() {
        return sdp;
    }

    public void setSdp(Map sdp) {
        this.sdp = sdp;
    }

    @Override
    public String toString() {
        return "Message{" +
                "type='" + type + '\'' +
                ", sdp=" + sdp +
                ", candidate=" + candidate +
                ", data=" + data +
                '}';
    }
}
