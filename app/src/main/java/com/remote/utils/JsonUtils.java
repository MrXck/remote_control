package com.remote.utils;

import com.google.gson.Gson;
import com.remote.pojo.Message;

public class JsonUtils {

    public static String toJson(Object o) {
        Gson gson = new Gson();
        return gson.toJson(o);
    }

    public static Message fromJson(String json) {
        return new Gson().fromJson(json, Message.class);
    }
}
