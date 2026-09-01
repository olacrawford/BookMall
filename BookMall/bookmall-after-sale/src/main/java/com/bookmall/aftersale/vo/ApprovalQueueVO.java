package com.bookmall.aftersale.vo;

import lombok.Data;

import java.util.List;

@Data
public class ApprovalQueueVO {

    private List<ApprovalTaskVO> items;

    public ApprovalQueueVO(List<ApprovalTaskVO> items) {
        this.items = items;
    }
}
