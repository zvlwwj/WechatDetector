package com.zou.wechatdetector.bean;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by zou on 2018/5/4.
 */

public class GsonGetDeviceListBean {
    private Integer code;
    private ArrayList<Device> deviceList = null;

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public ArrayList<Device> getDeviceList() {
        return deviceList;
    }

    public void setDeviceList(ArrayList<Device> deviceList) {
        this.deviceList = deviceList;
    }
}
