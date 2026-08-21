# BookMall Book 模块说明文档

## 1. 项目概述

`bookmall-book` 是 BookMall 项目中的图书服务模块，负责图书和分类相关能力。

当前这个模块已经完成了第一版可用闭环，包含：

- 图书列表
- 图书详情
- 图书搜索
- 图书分页
- 图书新增
- 图书修改
- 图书删除
- 图书上下架
- 分类列表
- 分类树
- 热点图书 Redis 缓存
- 图书软删除

该文档用于记录当前 `book` 模块的实现状态、配置、接口、数据库映射和验证方式。

## 2. 当前项目结构

当前根工程为 Maven 聚合工程，模块如下：

- `bookmall-common`
- `bookmall-auth`
- `bookmall-book`

当前项目目录中的关键部分：

- [pom.xml](D:/workspace_idea/BookMall/BookMall/pom.xml)
- [bookmall-common](D:/workspace_idea/BookMall/BookMall/bookmall-common)
- [bookmall-auth](D:/workspace_idea/BookMall/BookMall/bookmall-auth)
- [bookmall-book](D:/workspace_idea/BookMall/BookMall/bookmall-book)

## 3. 父工程配置说明

父工程用于统一管理子模块和依赖版本，不直接编写业务代码。

当前父工程配置要点：

- `packaging` 为 `pom`
- Java 版本为 `17`
- 统一管理 Spring Boot、Spring Cloud、Spring Cloud Alibaba 版本
- 当前模块包括：
  - `bookmall-common`
  - `bookmall-auth`
  - `bookmall-book`

当前使用的版本：

- Spring Boot `3.2.5`
- Spring Cloud `2023.0.2`
- Spring Cloud Alibaba `2023.0.1.0`

## 4. bookmall-common 模块说明

`bookmall-common` 是公共基础模块，用于存放所有服务都能复用的通用类。

当前包含的类：

- `Result<T>`：统一接口返回体
- `ErrorCode`：错误码枚举
- `BusinessException`：业务异常类

当前包结构：

- `com.bookmall.common.result`
- `com.bookmall.common.constant`
- `com.bookmall.common.exception`

### 4.1 Result<T>

文件路径：

- [Result.java](D:/workspace_idea/BookMall/BookMall/bookmall-common/src/main/java/com/bookmall/common/result/Result.java)

用途：

- 统一接口返回格式
- 所有服务尽量都使用这个结构返回数据

常用方法：

- `Result.success(data)`
- `Result.success()`
- `Result.fail(code, message)`
- `Result.fail(message)`

### 4.2 ErrorCode

文件路径：

- [ErrorCode.java](D:/workspace_idea/BookMall/BookMall/bookmall-common/src/main/java/com/bookmall/common/constant/ErrorCode.java)

当前错误码：

- `SUCCESS(200, "success")`
- `PARAM_ERROR(400, "参数错误")`
- `UNAUTHORIZED(401, "未授权")`
- `FORBIDDEN(403, "无权限")`
- `NOT_FOUND(404, "资源不存在")`
- `SYSTEM_ERROR(500, "系统异常")`

### 4.3 BusinessException

文件路径：

- [BusinessException.java](D:/workspace_idea/BookMall/BookMall/bookmall-common/src/main/java/com/bookmall/common/exception/BusinessException.java)

用途：

- 业务层主动抛出异常
- 例如：用户名已存在、密码错误、token 为空等

## 5. bookmall-book 模块说明

`bookmall-book` 是图书服务模块，负责图书和分类相关能力。

当前职责包括：

- 图书列表查询
- 图书详情查询
- 图书搜索
- 图书分页
- 图书新增
- 图书修改
- 图书删除
- 图书上下架
- 分类列表查询
- 分类树查询
- 热点数据缓存

### 5.1 启动类

文件路径：

- [BookApplication.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/BookApplication.java)

当前注解：

- `@SpringBootApplication`
- `@MapperScan("com.bookmall.book.mapper")`
- `@EnableConfigurationProperties(CacheProperties.class)`

作用：

- 启动 Spring Boot 服务
- 扫描 MyBatis-Plus Mapper 接口
- 让缓存参数配置类生效

### 5.2 配置文件

文件路径：

- [application.yml](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/resources/application.yml)

当前配置包含：

- 服务端口：`8070`
- 服务名：`book`
- 数据源：连接本地 MySQL 的 `bookmall` 数据库
- Redis 连接配置
- 缓存 TTL 配置
- MyBatis-Plus 配置

当前 Redis 连接信息：

- Host: `localhost`
- Port: `6379`
- Database: `0`

## 6. 依赖说明

文件路径：

- [bookmall-book/pom.xml](D:/workspace_idea/BookMall/BookMall/bookmall-book/pom.xml)

当前依赖包括：

- `bookmall-common`
- `spring-boot-starter-web`
- `spring-boot-starter-validation`
- `spring-boot-starter-data-redis`
- `mybatis-plus-spring-boot3-starter`
- `mysql-connector-j`
- `lombok`

## 7. 当前接口说明

当前 `book` 模块的接口前缀是：

- `/books`

### 7.1 GET /books/hello

用途：

- 服务健康检查
- 验证服务是否启动成功

当前返回：

- `bookmall-book is running`

### 7.2 GET /books

用途：

- 查询图书列表

功能说明：

- 只返回状态为上架的图书
- 返回图书基础信息
- 优先读取 Redis 缓存

### 7.3 GET /books/{id}

用途：

- 查询图书详情

功能说明：

- 根据图书 id 查询
- 如果不存在，返回 `404 图书不存在`
- 优先读取 Redis 缓存

### 7.4 GET /books/search

用途：

- 图书搜索

支持参数：

- `keyword`：标题关键字
- `categoryId`：分类 ID

功能说明：

- 支持标题模糊搜索
- 支持分类筛选
- 选中父分类时，会自动递归包含全部子分类

### 7.5 GET /books/page

用途：

- 图书分页查询

支持参数：

- `pageNum`
- `pageSize`
- `keyword`
- `categoryId`

功能说明：

- 支持分页
- 支持关键字过滤
- 支持分类树筛选

### 7.6 POST /books

用途：

- 新增图书

请求字段：

- `title`
- `author`
- `price`
- `categoryId`
- `coverUrl`
- `description`

功能说明：

- 新增图书
- 默认状态为上架
- 自动写入创建时间和更新时间
- 写入后清理相关缓存

### 7.7 PUT /books/{id}

用途：

- 修改图书信息

请求字段：

- `title`
- `author`
- `price`
- `categoryId`
- `coverUrl`
- `description`
- `status`

功能说明：

- 根据 id 更新图书
- 同时更新状态和更新时间
- 更新后清理列表、详情和分类树缓存

### 7.8 DELETE /books/{id}

用途：

- 删除图书

功能说明：

- 当前使用软删除
- 实际上会更新 `deleted` 字段
- 删除后清理相关缓存

### 7.9 PUT /books/{id}/status

用途：

- 图书上下架

支持参数：

- `status=1`：上架
- `status=0`：下架

### 7.10 GET /books/categories

用途：

- 查询分类列表

功能说明：

- 返回平铺分类数据
- 只查询启用状态分类

### 7.11 GET /books/categories/tree

用途：

- 查询分类树

功能说明：

- 返回一级分类及其子分类
- 优先读取 Redis 缓存
- 适合前端做导航展示

## 8. Redis 缓存实现说明

### 8.1 缓存键

文件路径：

- [CacheKeys.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/constant/CacheKeys.java)

当前缓存键：

- `book:list`
- `book:detail:{id}`
- `book:category:tree`

### 8.2 缓存配置类

文件路径：

- [CacheProperties.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/config/CacheProperties.java)

作用：

- 管理图书列表、详情、分类树和空值缓存的过期时间

### 8.3 RedisConfig

文件路径：

- [RedisConfig.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/config/RedisConfig.java)

作用：

- 配置 `RedisTemplate<String, Object>`
- 使用 JSON 序列化 Java 对象

### 8.4 CacheSupport

文件路径：

- [CacheSupport.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/support/CacheSupport.java)

作用：

- 封装缓存读取、写入、空值缓存和缓存清理逻辑
- 为缓存 TTL 增加随机抖动，减少同一时间大量失效

## 9. 业务类结构说明

### 9.1 Entity

#### Book

文件路径：

- [Book.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/entity/Book.java)

映射表：

- `t_book`

字段：

- `id`
- `title`
- `author`
- `price`
- `categoryId`
- `coverUrl`
- `description`
- `status`
- `deleted`
- `createTime`
- `updateTime`

说明：

- `deleted` 字段用于软删除
- `status` 字段用于上架/下架

#### Category

文件路径：

- [Category.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/entity/Category.java)

映射表：

- `t_category`

字段：

- `id`
- `name`
- `parentId`
- `sort`
- `status`
- `createTime`
- `updateTime`

### 9.2 Mapper

#### BookMapper

文件路径：

- [BookMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/mapper/BookMapper.java)

说明：

- 继承 `BaseMapper<Book>`
- 使用 MyBatis-Plus 提供的基础 CRUD 能力

#### CategoryMapper

文件路径：

- [CategoryMapper.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/mapper/CategoryMapper.java)

说明：

- 继承 `BaseMapper<Category>`
- 使用 MyBatis-Plus 提供的基础 CRUD 能力

### 9.3 DTO

#### BookCreateRequest

文件路径：

- [BookCreateRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/dto/BookCreateRequest.java)

#### BookUpdateRequest

文件路径：

- [BookUpdateRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/dto/BookUpdateRequest.java)

#### BookSearchRequest

文件路径：

- [BookSearchRequest.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/dto/BookSearchRequest.java)

### 9.4 VO

#### BookVO

文件路径：

- [BookVO.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/vo/BookVO.java)

当前额外字段：

- `categoryId`

作用：

- 让前端在分类筛选场景下更方便拿到图书所属分类

#### BookDetailVO

文件路径：

- [BookDetailVO.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/vo/BookDetailVO.java)

#### CategoryVO

文件路径：

- [CategoryVO.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/vo/CategoryVO.java)

#### CategoryTreeVO

文件路径：

- [CategoryTreeVO.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/vo/CategoryTreeVO.java)

### 9.5 Service

#### BookService

文件路径：

- [BookService.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/BookService.java)

当前方法：

- `listBooks()`
- `getBookById(Long id)`
- `searchBooks(BookSearchRequest request)`
- `pageBooks(Integer pageNum, Integer pageSize, String keyword, Long categoryId)`
- `createBook(BookCreateRequest request)`
- `updateBook(Long id, BookUpdateRequest request)`
- `deleteBook(Long id)`
- `updateBookStatus(Long id, Integer status)`

#### CategoryService

文件路径：

- [CategoryService.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/CategoryService.java)

当前方法：

- `listCategories()`
- `listCategoryTree()`

### 9.6 Service 实现

#### BookServiceImpl

文件路径：

- [BookServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/impl/BookServiceImpl.java)

职责：

- 图书列表查询
- 图书详情查询
- 图书搜索
- 图书分页
- 图书新增
- 图书修改
- 图书删除
- 图书上下架
- 图书列表 / 图书详情 Redis 缓存
- 分类筛选递归展开

#### CategoryServiceImpl

文件路径：

- [CategoryServiceImpl.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/service/impl/CategoryServiceImpl.java)

职责：

- 分类平铺查询
- 分类树组装
- 分类树 Redis 缓存

### 9.7 Controller

文件路径：

- [BookController.java](D:/workspace_idea/BookMall/BookMall/bookmall-book/src/main/java/com/bookmall/book/controller/BookController.java)

职责：

- 暴露图书和分类相关 HTTP 接口

## 10. 数据库说明

当前数据库：

- `bookmall`

当前已经创建的表：

- `t_user`
- `t_category`
- `t_book`
- `t_inventory`
- `t_cart`
- `t_order`
- `t_order_item`
- `t_address`

其中 `book` 模块当前主要使用：

- `t_book`
- `t_category`

### 10.1 图书表软删除

`book` 模块对 `t_book` 使用了软删除。

当前做法：

- `t_book` 添加 `deleted` 字段
- 实体类中使用 `@TableLogic`
- 删除接口调用 `deleteById`
- MyBatis-Plus 会自动把删除转换成更新 `deleted = 1`

## 11. 已验证功能

当前已经验证通过的功能有：

- `GET /books/hello` 可访问
- `GET /books` 可查询图书列表
- `GET /books/{id}` 可查询图书详情
- `GET /books/search` 可按关键字和分类搜索
- `GET /books/page` 可分页查询
- `GET /books/categories` 可查询分类列表
- `GET /books/categories/tree` 可查询分类树
- `POST /books` 可新增图书
- `PUT /books/{id}` 可修改图书
- `DELETE /books/{id}` 可软删除图书
- `PUT /books/{id}/status` 可上下架图书
- Redis 缓存相关代码可通过编译

## 12. 当前测试方式

当前功能主要通过以下方式验证：

- 浏览器访问 `GET /books/hello`
- Postman / Apifox 测试 `GET`、`POST`、`PUT`、`DELETE`
- IDEA HTTP Client 测试接口
- MySQL 中查看 `t_book`、`t_category` 表数据
- Redis 中查看缓存键是否写入

### 12.1 图书新增测试

请求示例：

```json
{
  "title": "MyBatis-Plus实战",
  "author": "张三",
  "price": 68.00,
  "categoryId": 2,
  "coverUrl": null,
  "description": "一本讲解 MyBatis-Plus 的实战图书。"
}
```

### 12.2 图书修改测试

请求示例：

```json
{
  "title": "Java核心技术（第12版）",
  "author": "Cay S. Horstmann",
  "price": 109.00,
  "categoryId": 2,
  "coverUrl": null,
  "description": "更新后的经典Java基础图书。",
  "status": 1
}
```

### 12.3 图书上下架测试

请求示例：

```text
PUT /books/1/status?status=1
PUT /books/1/status?status=0
```

### 12.4 Redis 缓存测试

建议测试顺序：

1. 请求 `GET /books`
2. 请求 `GET /books/1`
3. 请求 `GET /books/categories/tree`
4. 到 Redis 中查看是否出现以下 key：
   - `book:list`
   - `book:detail:1`
   - `book:category:tree`
5. 再执行图书修改或上下架操作
6. 检查对应 key 是否被清理

## 13. 当前模块状态总结

`bookmall-book` 已经完成了图书服务的第一版可用闭环，并增加了第一项增强能力：Redis 热点图书缓存。

现在它已经具备：

- 可运行
- 可查询
- 可分页
- 可搜索
- 可分类
- 可新增
- 可修改
- 可删除
- 可上下架
- 支持软删除
- 支持热点数据缓存

## 14. 后续开发计划

下一步建议开发：

- `Sentinel`：保护图书热点接口
- `日志和链路追踪`
- `RabbitMQ`：订单异步通知
- `Seata`：订单链路分布式事务

## 15. 维护规则

以后每完成一个模块或增强项，都要同步输出说明文档，并记录：

- 模块职责
- 配置说明
- 文件结构
- 接口列表
- 数据模型
- 测试方式
- 当前状态
- 下一步计划
- 新增代码实现了什么功能
