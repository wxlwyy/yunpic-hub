package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.dto.space.analyze.*;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.analyze.*;

import java.util.List;

/**
 * @author wxl
 */
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 获取空间使用率分析结果
     * @param spaceUsageAnalyzeRequest
     * @param currentUser
     * @return
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User currentUser);

    /**
     * 获取空间图片分类分析
     * @param spaceCategoryAnalyzeRequest
     * @param currentUser
     * @return
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User currentUser);

    /**
     * 获取空间标签分析结果
     * @param spaceTagAnalyzeRequest
     * @param currentUser
     * @return
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User currentUser);

    /**
     * 获取空间图片大小范围分析结果
     * @param spaceSizeAnalyzeRequest
     * @param currentUser
     * @return
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User currentUser);

    /**
     * 获取空间用户上传图片行为分析结果
     * @param spaceUserAnalyzeRequest
     * @param currentUser
     * @return
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User currentUser);

    /**
     * 获取空间使用排行分析结果
     * @param spaceRankAnalyzeRequest
     * @param currentUser
     * @return
     */
    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User currentUser);
}
