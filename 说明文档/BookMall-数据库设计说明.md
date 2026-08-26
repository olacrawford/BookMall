# BookMall 数据库设计说明

## 当前基础表

`sql/sql.txt` 保存初始建库脚本，目前包含：

- `t_user`：用户
- `t_category`：图书分类
- `t_book`：图书
- `t_order`：订单主表
- `t_order_item`：订单明细

## 增量脚本规范

数据库增量变更统一放在 `sql/updates/`，不直接修改已经初始化过的旧脚本。

## 第一阶段新增

脚本：`sql/updates/001_cart_address_stock.sql`

- `t_user_address`：用户收货地址，支持多个地址和默认地址。
- `t_cart_item`：购物车条目，`(user_id, book_id)` 唯一，由 `bookmall-cart` 服务使用，同一本书重复加入时更新数量。
- `t_book_stock`：图书库存表。

执行方式：

```bash
mysql -h127.0.0.1 -uroot -p < sql/updates/001_cart_address_stock.sql
```
