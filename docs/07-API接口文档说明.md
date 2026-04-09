# API 接口文档说明

## 1. 文档入口

- 启动后端服务后，默认访问地址：`http://localhost:8080/api/doc.html`
- OpenAPI JSON 地址：`http://localhost:8080/api/v2/api-docs`

## 2. 本次补充范围

- 已为系统端与客户端 Controller 补充接口级中文说明。
- 已为 DTO、VO、Query 以及通用分页/返回对象补充字段级中文说明。
- 已接入到 Knife4j 页面，可直接查看接口用途、请求字段、响应字段和分页结构。

## 3. 文档阅读约定

- 接口统一返回结构为 `Result<T>`
- 分页接口数据结构为 `Result<PageResult<T>>`
- `Result` 字段说明：
  - `code`：状态码
  - `msg`：提示信息
  - `data`：业务数据
- `PageResult` 字段说明：
  - `records`：当前页数据列表
  - `total`：总记录数
  - `pageNum`：当前页码
  - `pageSize`：每页数量

## 4. 维护方式

- 后续新增接口时，默认同步补充 Controller 的接口说明。
- 后续新增 DTO、VO、Query 时，默认同步补充字段说明。
- 返回码、业务枚举、字段取值的详细对照，统一维护在 `docs/08-API返回码、业务枚举与字段取值说明.md`。
- 如果字段语义调整，优先同步更新代码内 Swagger 注解，避免在线文档与实际接口不一致。
