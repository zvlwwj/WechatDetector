package com.zou.wechatdetector.bean;

/**
 * Created by zou on 2018/5/4.
 */

public class GsonAddDeviceBean {
    private Integer code;
    private String user_name;
    private int deviceId;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public String getUser_name() {
        return user_name;
    }

    public void setUser_name(String user_name) {
        this.user_name = user_name;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(int deviceId) {
        this.deviceId = deviceId;
    }
}
