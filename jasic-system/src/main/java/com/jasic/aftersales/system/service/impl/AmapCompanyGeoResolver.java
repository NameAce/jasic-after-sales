package com.jasic.aftersales.system.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jasic.aftersales.common.exception.ServiceException;
import com.jasic.aftersales.system.service.ICompanyGeoResolver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 基于高德 WebService 的公司地址解析实现
 *
 * @author Codex
 * @date 2026/04/02
 */
@Slf4j
@Service
public class AmapCompanyGeoResolver implements ICompanyGeoResolver {

    @Value("${jasic.amap.geocode.enabled:false}")
    private boolean enabled;

    @Value("${jasic.amap.geocode.key:}")
    private String key;

    @Value("${jasic.amap.geocode.url:https://restapi.amap.com/v3/geocode/geo}")
    private String url;

    @Value("${jasic.amap.geocode.timeout-ms:3000}")
    private int timeoutMs;

    /**
     * 根据地址解析经纬度
     *
     * @param address 公司地址
     * @return 经纬度结果
     */
    @Override
    public GeoLocation resolve(String address) {
        String normalizedAddress = StrUtil.trim(address);
        if (StrUtil.isBlank(normalizedAddress)) {
            throw new ServiceException("公司地址不能为空");
        }
        if (!enabled || StrUtil.isBlank(key)) {
            throw new ServiceException("未配置地址解析服务，请联系管理员");
        }

        Map<String, Object> params = new LinkedHashMap<>();
        params.put("key", key);
        params.put("address", normalizedAddress);

        String responseBody;
        try {
            responseBody = HttpUtil.get(url, params, timeoutMs);
        } catch (Exception ex) {
            log.error("调用高德地址解析失败，address={}", normalizedAddress, ex);
            throw new ServiceException("地址解析服务调用失败，请稍后重试");
        }

        try {
            JSONObject response = JSONUtil.parseObj(responseBody);
            if (!"1".equals(response.getStr("status"))) {
                String info = StrUtil.blankToDefault(response.getStr("info"), "地址解析失败");
                throw new ServiceException(info);
            }
            JSONArray geocodes = response.getJSONArray("geocodes");
            if (geocodes == null || geocodes.isEmpty()) {
                throw new ServiceException("未找到该地址对应的经纬度");
            }
            String location = geocodes.getJSONObject(0).getStr("location");
            if (StrUtil.isBlank(location) || !location.contains(",")) {
                throw new ServiceException("地址解析结果异常，请检查地址是否完整");
            }
            String[] coordinates = location.split(",");
            return new GeoLocation(new BigDecimal(coordinates[0]), new BigDecimal(coordinates[1]));
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("解析高德地址响应失败，address={}, response={}", normalizedAddress, responseBody, ex);
            throw new ServiceException("地址解析结果异常，请稍后重试");
        }
    }
}
