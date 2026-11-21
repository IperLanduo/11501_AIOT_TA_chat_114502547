package com.example.helloworld;

import org.json.JSONException;
import org.json.JSONObject;

public class MsgObj {
    public static final String LABEL_NAME = "name";
    public static final String LABEL_MSG = "msg";

    public static final String LABEL_TYPE = "type";

    private String name;

    public MsgObj(String name) {
        this.name = name;
    }

    // 把訊息打包成一行 JSON，例如 {"name":"Server","msg":"你好"}
    public String toJson(String type, String msg) throws JSONException {
        JSONObject obj = new JSONObject();
        obj.put(LABEL_TYPE, type);

        obj.put(LABEL_NAME, name);
        obj.put(LABEL_MSG, msg);
        return obj.toString();
    }
}