package com.hhk.picturebacked.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hhk.picturebacked.model.entity.Picture;
import com.hhk.picturebacked.model.enums.AssetTypeEnum;
import com.hhk.picturebacked.model.enums.PlatformSpecEnum;
import com.hhk.picturebacked.model.vo.PlatformSpecVO;
import com.hhk.picturebacked.model.vo.SpecCheckResultVO;
import com.hhk.picturebacked.service.PlatformSpecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 平台规格服务实现
 */
@Service
@Slf4j
public class PlatformSpecServiceImpl implements PlatformSpecService {

    @Override
    public List<PlatformSpecVO> listSpec() {
        return Arrays.stream(PlatformSpecEnum.values())
                .map(PlatformSpecVO::objToVo)
                .collect(Collectors.toList());
    }

    @Override
    public PlatformSpecVO getSpec(String platform) {
        PlatformSpecEnum spec = PlatformSpecEnum.getEnumByValue(platform);
        return spec == null ? null : PlatformSpecVO.objToVo(spec);
    }

    @Override
    public SpecCheckResultVO checkPicture(Picture picture, String platform, String assetType, String productSku) {
        SpecCheckResultVO result = new SpecCheckResultVO();
        result.setPlatform(platform);

        PlatformSpecEnum spec = PlatformSpecEnum.getEnumByValue(platform);
        if (spec == null) {
            result.setPass(false);
            result.addReason("不支持的平台：" + platform);
            return result;
        }
        if (picture == null) {
            result.setPass(false);
            result.addReason("图片不存在");
            return result;
        }

        // 1. 尺寸校验：主图用主图规格，其余用详情规格
        boolean useMainSpec = AssetTypeEnum.MAIN.getValue().equals(assetType)
                || StrUtil.isBlank(assetType);
        int expectWidth = useMainSpec ? spec.getMainWidth() : spec.getDetailWidth();
        int expectHeight = useMainSpec ? spec.getMainHeight() : spec.getDetailHeight();

        Integer picWidth = picture.getPicWidth();
        Integer picHeight = picture.getPicHeight();
        if (picWidth != null && picWidth > 0 && picHeight != null && picHeight > 0) {
            // 允许非 1:1 图片通过：要求尺寸不小于规范下限，放大到规范尺寸由渲染环节完成
            if (picWidth < expectWidth || picHeight < expectHeight) {
                result.addReason(String.format("尺寸不足：当前 %dx%d，规范要求不小于 %dx%d",
                        picWidth, picHeight, expectWidth, expectHeight));
            }
            // 比例检查（主图必须是 1:1）
            if (useMainSpec && picWidth.intValue() != picHeight.intValue()) {
                result.addReason(String.format("主图必须为 1:1 正方形，当前比例 %d:%d", picWidth, picHeight));
            }
        } else {
            result.addReason("缺少图片尺寸信息，无法校验");
        }

        // 2. 体积校验
        Long picSize = picture.getPicSize();
        long maxBytes = (long) spec.getMaxSizeMb() * 1024 * 1024;
        if (picSize != null && picSize > 0 && picSize > maxBytes) {
            result.addReason(String.format("体积超限：当前 %.1fMB，上限 %dMB",
                    picSize / 1024.0 / 1024.0, spec.getMaxSizeMb()));
        }

        // 3. 格式校验
        String picFormat = picture.getPicFormat();
        if (StrUtil.isNotBlank(picFormat) && !spec.getFormat().equalsIgnoreCase(picFormat)) {
            result.addReason(String.format("格式不符：当前 %s，规范要求 %s", picFormat, spec.getFormat()));
        }

        // 4. 拼图校验：平台禁止拼图时无法自动判定，交给人工审核
        if (spec.isForbidCollage() && AssetTypeEnum.MAIN.getValue().equals(assetType)) {
            result.addReason("平台禁止拼图，需人工确认主图为单品图");
        }

        // 5. 白底校验：无像素数据可判定时给出提示
        if (spec.isWhiteBackgroundRequired()) {
            result.addReason("平台要求纯白底，需人工确认背景为 RGB(255,255,255)");
        }

        // 6. TikTok 竖版封面安全区提示
        if (spec.hasCover() && AssetTypeEnum.SCENE.getValue().equals(assetType)) {
            result.addReason(String.format("竖版封面 %dx%d，顶部约 %d%% 为标题安全区，商品主体需避开",
                    spec.getCoverWidth(), spec.getCoverHeight(), spec.getTitleSafeAreaPercent()));
        }

        result.setPass(result.getReasons().isEmpty());
        result.setSuggestFileName(buildFileName(productSku, platform, assetType, 1));
        return result;
    }

    @Override
    public String buildFileName(String productSku, String platform, String assetType, int sequence) {
        StringBuilder sb = new StringBuilder();
        sb.append(StrUtil.isBlank(productSku) ? "UNKNOWN" : productSku).append("_");
        sb.append(StrUtil.isBlank(platform) ? "COMMON" : platform).append("_");
        sb.append(StrUtil.isBlank(assetType) ? "MAIN" : assetType).append("_");
        sb.append(sequence < 1 ? 1 : sequence);
        PlatformSpecEnum spec = PlatformSpecEnum.getEnumByValue(platform);
        sb.append(".").append(spec == null ? "jpg" : spec.getFormat().toLowerCase());
        return sb.toString();
    }
}
