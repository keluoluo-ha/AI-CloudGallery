package com.hhk.picturebacked.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 平台规格合规校验结果
 */
@Data
public class SpecCheckResultVO implements Serializable {

    /**
     * 是否合规
     */
    private Boolean pass;

    /**
     * 平台枚举值
     */
    private String platform;

    /**
     * 不合规原因列表，为空表示全部通过
     */
    private List<String> reasons = new ArrayList<>();

    /**
     * 建议的合规文件命名
     */
    private String suggestFileName;

    /**
     * 追加不合规原因
     */
    public void addReason(String reason) {
        if (reason != null && !reason.isEmpty()) {
            this.reasons.add(reason);
        }
    }

    private static final long serialVersionUID = 1L;
}
