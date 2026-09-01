package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_rag_document")
public class RagDocument {

    private Long id;
    private String documentCode;
    private String title;
    private String category;
    private String policyVersion;
    private String permissionScope;
    private String status;
    private LocalDateTime createTime;
}
