package com.bookmall.aftersale.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_rag_chunk")
public class RagChunk {

    private Long id;
    private Long documentId;
    private Integer chunkNo;
    private String content;
    private String keywords;
    private String vectorRef;
    private Integer tokenCount;
    private LocalDateTime createTime;
}
