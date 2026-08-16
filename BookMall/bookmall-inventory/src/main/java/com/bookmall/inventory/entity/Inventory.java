package com.bookmall.inventory.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_inventory")
public class Inventory {

    private Long id;
    private Long bookId;
    private Integer availableStock;
    private Integer lockedStock;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}