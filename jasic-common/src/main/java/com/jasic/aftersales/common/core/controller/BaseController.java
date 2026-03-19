package com.jasic.aftersales.common.core.controller;

import com.jasic.aftersales.common.core.domain.Result;

/**
 * Controller 基类
 *
 * @author Zoro
 * @date 2026/03/18
 */
public class BaseController {

    /**
     * 根据操作结果返回响应
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected Result<Void> toResult(int rows) {
        return rows > 0 ? Result.ok() : Result.fail("操作失败");
    }

    /**
     * 根据布尔结果返回响应
     *
     * @param result 操作结果
     * @return 响应
     */
    protected Result<Void> toResult(boolean result) {
        return result ? Result.ok() : Result.fail("操作失败");
    }
}
