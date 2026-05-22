package com.jasic.aftersales.system.service.impl;

import cn.dev33.satoken.context.model.SaRequest;
import cn.dev33.satoken.context.model.SaResponse;
import cn.dev33.satoken.context.model.SaStorage;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**MockSaRequest 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
class MockSaRequest implements SaRequest {

    /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @Override
    public Object getSource() {
        return this;
    }

    /**getParam 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
    @Override
    public String getParam(String name) {
        return null;
    }

    /**getParamNames 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
    @Override
    public List<String> getParamNames() {
        return new ArrayList<>();
    }

    /**getParamMap 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或组装后的业务数据集合。*/
    @Override
    public Map<String, String> getParamMap() {
        return new LinkedHashMap<>();
    }

    /**getHeader 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
    @Override
    public String getHeader(String name) {
        return null;
    }

    /**getCookieValue 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@return 查询或解析得到的业务对象。*/
    @Override
    public String getCookieValue(String name) {
        return null;
    }

    /**getRequestPath 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @Override
    public String getRequestPath() {
        return "/";
    }

    /**getUrl 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @Override
    public String getUrl() {
        return "http://localhost/";
    }

    /**getMethod 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @Override
    public String getMethod() {
        return "GET";
    }

    /**forward 处理逻辑，服务于当前类的业务编排和数据转换。
@param path path 字段参数。
@return 处理后的业务结果。*/
    @Override
    public String forward(String path) {
        return path;
    }
}

/**MockSaResponse 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
class MockSaResponse implements SaResponse {

    /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @Override
    public Object getSource() {
        return this;
    }

    /**setStatus 处理逻辑，服务于当前类的业务编排和数据转换。
@param sc sc 字段参数。
@return 处理后的业务结果。*/
    @Override
    public SaResponse setStatus(int sc) {
        return this;
    }

    /**setHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
    @Override
    public SaResponse setHeader(String name, String value) {
        return this;
    }

    /**addHeader 处理逻辑，服务于当前类的业务编排和数据转换。
@param name 名称文本，用于展示、匹配或保存业务对象名称。
@param value value 字段参数。
@return 处理后的业务结果。*/
    @Override
    public SaResponse addHeader(String name, String value) {
        return this;
    }

    /**redirect 处理逻辑，服务于当前类的业务编排和数据转换。
@param url url 字段参数。
@return 处理后的业务结果。*/
    @Override
    public Object redirect(String url) {
        return url;
    }
}

/**MockSaStorage 测试类，用于验证对应业务规则、边界条件和回归场景。

@author Zoro*/
class MockSaStorage implements SaStorage {

    /**storage 字段，用于当前类内部业务处理。*/
    private final Map<String, Object> storage = new LinkedHashMap<>();

    /**getSource 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@return 查询或解析得到的业务对象。*/
    @Override
    public Object getSource() {
        return this;
    }

    /**get 业务对象，缺失或不满足条件时按调用语义返回空值或抛出业务异常。
@param key key 字段参数。
@return 查询或解析得到的业务对象。*/
    @Override
    public Object get(String key) {
        return storage.get(key);
    }

    /**set 处理逻辑，服务于当前类的业务编排和数据转换。
@param key key 字段参数。
@param value value 字段参数。
@return 处理后的业务结果。*/
    @Override
    public SaStorage set(String key, Object value) {
        storage.put(key, value);
        return this;
    }

    /**delete 业务动作，完成必要校验后同步更新主表、明细表和流程记录。
@param key key 字段参数。
@return 处理后的业务结果。*/
    @Override
    public SaStorage delete(String key) {
        storage.remove(key);
        return this;
    }
}
