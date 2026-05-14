package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class MockSaRequest implements SaRequest {

    @Override
    public Object getSource() {
        return this;
    }

    @Override
    public String getParam(String name) {
        return null;
    }

    @Override
    public List<String> getParamNames() {
        return new ArrayList<>();
    }

    @Override
    public Map<String, String> getParamMap() {
        return new LinkedHashMap<>();
    }

    @Override
    public String getHeader(String name) {
        return null;
    }

    @Override
    public String getCookieValue(String name) {
        return null;
    }

    @Override
    public String getRequestPath() {
        return "/";
    }

    @Override
    public String getUrl() {
        return "http://localhost/";
    }

    @Override
    public String getMethod() {
        return "GET";
    }

    @Override
    public String forward(String path) {
        return path;
    }
}

class MockSaResponse implements SaResponse {

    @Override
    public Object getSource() {
        return this;
    }

    @Override
    public SaResponse setStatus(int sc) {
        return this;
    }

    @Override
    public SaResponse setHeader(String name, String value) {
        return this;
    }

    @Override
    public SaResponse addHeader(String name, String value) {
        return this;
    }

    @Override
    public Object redirect(String url) {
        return url;
    }
}

class MockSaStorage implements SaStorage {

    private final Map<String, Object> storage = new LinkedHashMap<>();

    @Override
    public Object getSource() {
        return this;
    }

    @Override
    public Object get(String key) {
        return storage.get(key);
    }

    @Override
    public SaStorage set(String key, Object value) {
        storage.put(key, value);
        return this;
    }

    @Override
    public SaStorage delete(String key) {
        storage.remove(key);
        return this;
    }
}
