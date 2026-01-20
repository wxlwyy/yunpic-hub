package com.wyy.yunpicturebackend.service.impl;

import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicturebackend.exception.BusinessException;
import com.wyy.yunpicturebackend.exception.ErrorCode;
import com.wyy.yunpicturebackend.exception.ThrowUtils;
import com.wyy.yunpicturebackend.mapper.SpaceMapper;
import com.wyy.yunpicturebackend.model.dto.space.analyze.*;
import com.wyy.yunpicturebackend.model.entity.Picture;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.analyze.*;
import com.wyy.yunpicturebackend.service.PictureService;
import com.wyy.yunpicturebackend.service.SpaceAnalyzeService;
import com.wyy.yunpicturebackend.service.SpaceService;
import com.wyy.yunpicturebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
* @author wxl
*/
@Service
public class SpaceAnalyzeServiceImpl extends ServiceImpl<SpaceMapper, Space>
    implements SpaceAnalyzeService{

    @Resource
    private UserService userService;

    @Resource
    private SpaceService spaceService;

    @Resource
    private PictureService pictureService;

    /**
     * 校验空间分析权限
     * @param spaceAnalyzeRequest
     * @param loginUser
     */
    private void checkSpaceAnalyzeAuth(SpaceAnalyzeRequest spaceAnalyzeRequest, User loginUser) {
        // 分析全空间或公共空间
        if (spaceAnalyzeRequest.isQueryAll() || spaceAnalyzeRequest.isQueryPublic()) {
            // 权限校验，仅管理员可操作
            ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        } else { // 分析单个空间
            Long spaceId = spaceAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null || spaceId < 0, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 权限校验，仅本人或管理员可操作
            spaceService.checkSpaceAuth(loginUser, space);
        }
    }

    /**
     * 补充空间分析的查询条件
     * @param spaceAnalyzeRequest
     * @param queryWrapper
     */
    private void fillSpaceAnalyzeQueryWrapper(SpaceAnalyzeRequest spaceAnalyzeRequest, QueryWrapper<Picture> queryWrapper) {
        if (spaceAnalyzeRequest.isQueryAll()) {
            return;
        }
        if (spaceAnalyzeRequest.isQueryPublic()) {
            queryWrapper.isNull("spaceId");
            return;
        }
        Long spaceId = spaceAnalyzeRequest.getSpaceId();
        if (spaceId != null) {
            queryWrapper.eq("spaceId", spaceId);
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "未指定查询范围");
    }

    /**
     * 获取空间使用率分析结果
     * @param spaceUsageAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceUsageAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 查询公共空间或全空间
        if (spaceUsageAnalyzeRequest.isQueryPublic() || spaceUsageAnalyzeRequest.isQueryAll()) {
            // 校验空间分析权限
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
            // 补充查询条件的范围
            fillSpaceAnalyzeQueryWrapper(spaceUsageAnalyzeRequest, pictureQueryWrapper);
            // 查询条件设置查询该范围内的所有图片大小
            pictureQueryWrapper.select("picSize");
            // 查询
            List<Object> pictureObjList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper);
            // 已上传图片数量
            long usedCount = pictureObjList.size();
            // 已上传图片大小
            long usedSize = pictureObjList.stream().mapToLong(obj -> (Long) obj).sum();

            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            spaceUsageAnalyzeResponse.setUsedSize(usedSize);
            spaceUsageAnalyzeResponse.setMaxSize(null);
            spaceUsageAnalyzeResponse.setSizeUsage(null);
            spaceUsageAnalyzeResponse.setUsedCount(usedCount);
            spaceUsageAnalyzeResponse.setMaxCount(null);
            spaceUsageAnalyzeResponse.setCountUsage(null);
            return spaceUsageAnalyzeResponse;
        } else {
            // 查询某个空间
            // 校验参数
            Long spaceId = spaceUsageAnalyzeRequest.getSpaceId();
            ThrowUtils.throwIf(spaceId == null, ErrorCode.PARAMS_ERROR);
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
            // 校验分析空间权限
            checkSpaceAnalyzeAuth(spaceUsageAnalyzeRequest, loginUser);
            SpaceUsageAnalyzeResponse spaceUsageAnalyzeResponse = new SpaceUsageAnalyzeResponse();
            Long totalSize = space.getTotalSize();
            Long maxSize = space.getMaxSize();
            Long totalCount = space.getTotalCount();
            Long maxCount = space.getMaxCount();
            spaceUsageAnalyzeResponse.setUsedSize(totalSize);
            spaceUsageAnalyzeResponse.setMaxSize(maxSize);
            spaceUsageAnalyzeResponse.setUsedCount(totalCount);
            spaceUsageAnalyzeResponse.setMaxCount(maxCount);
            Double sizeUsage = NumberUtil.round(totalSize * 100.0 / maxSize, 2).doubleValue();
            Double countUsage = NumberUtil.round(totalCount * 100.0 / maxCount, 2).doubleValue();
            spaceUsageAnalyzeResponse.setSizeUsage(sizeUsage);
            spaceUsageAnalyzeResponse.setCountUsage(countUsage);
            return spaceUsageAnalyzeResponse;
        }
    }

    /**
     * 获取空间图片分类分析
     * @param spaceCategoryAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceCategoryAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限（仅管理员或本人可操作私人空间）
        checkSpaceAnalyzeAuth(spaceCategoryAnalyzeRequest, loginUser);
        // 填充查询范围
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        fillSpaceAnalyzeQueryWrapper(spaceCategoryAnalyzeRequest, pictureQueryWrapper);
        // 查询分类名、总数量、总大小
        pictureQueryWrapper.select("category", "count(*) as totalCount", "sum(picSize) as totalSize")
                .groupBy("category");
        List<Map<String, Object>> maps = pictureService.getBaseMapper().selectMaps(pictureQueryWrapper);
        // 将集合中的map对象转为空间分类分析响应对象
        List<SpaceCategoryAnalyzeResponse> spaceCategoryAnalyzeResponseList = maps.stream()
                .map(map -> {
                    String category = (String) map.get("category");
                    Long totalCount = (Long) map.get("totalCount");
                    Long totalSize = (Long) map.get("totalSize");
                    SpaceCategoryAnalyzeResponse spaceCategoryAnalyzeResponse = new SpaceCategoryAnalyzeResponse(category, totalCount, totalSize);
                    return spaceCategoryAnalyzeResponse;
                })
                .collect(Collectors.toList());
        return spaceCategoryAnalyzeResponseList;
    }

    /**
     * 获取空间标签分析结果
     * @param spaceTagAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceTagAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限（仅管理员或本人可操作）
        checkSpaceAnalyzeAuth(spaceTagAnalyzeRequest, loginUser);
        // 补充查询条件范围
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        fillSpaceAnalyzeQueryWrapper(spaceTagAnalyzeRequest, pictureQueryWrapper);
        // 查询标签
        pictureQueryWrapper.select("tags");
        List<Object> tagObjList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper);
        // 将结果转为JSON字符串的集合
        List<String> tagList = tagObjList.stream()
                .filter(ObjUtil::isNotNull)
                .map(tagObj -> (String) tagObj)
                .collect(Collectors.toList());
        // 将集合里所有JSON字符串转为普通字符串平铺
        Map<String, Long> tagCountMap = tagList.stream()
                .flatMap(tag -> JSONUtil.toList(tag, String.class).stream())
                .collect(Collectors.groupingBy(tag -> tag, Collectors.counting()));
        // 将map中的数据转到空间标签分析响应中
        List<SpaceTagAnalyzeResponse> spaceTagAnalyzeResponses = tagCountMap.entrySet().stream()
                .sorted((tagMap2, tagMap1) -> Long.compare(tagMap2.getValue(), tagMap1.getValue()))
                .map(tagMap -> new SpaceTagAnalyzeResponse(tagMap.getKey(), tagMap.getValue()))
                .collect(Collectors.toList());
        return spaceTagAnalyzeResponses;
    }

    /**
     * 获取空间图片大小范围分析结果
     * @param spaceSizeAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceSizeAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限（仅管理员或本人可操作）
        checkSpaceAnalyzeAuth(spaceSizeAnalyzeRequest, loginUser);
        // 补充查询条件
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        fillSpaceAnalyzeQueryWrapper(spaceSizeAnalyzeRequest, pictureQueryWrapper);
        // 查询图片大小
        pictureQueryWrapper.select("picSize");
        List<Object> picSizeObjList = pictureService.getBaseMapper().selectObjs(pictureQueryWrapper);
        List<Long> picSizeList = picSizeObjList.stream()
                .map(picSizeObj -> ((Number) picSizeObj).longValue())
                .collect(Collectors.toList());
        // 定义范围，用有序map
        LinkedHashMap<String, Long> picSizeRangeMap = new LinkedHashMap<>();
        picSizeRangeMap.put("<100KB", picSizeList.stream().filter(picSize -> picSize < 100 * 1024).count());
        picSizeRangeMap.put("100KB-500KB", picSizeList.stream().filter(size -> size >= 100 * 1024 && size < 500 * 1024).count());
        picSizeRangeMap.put("500KB-1MB", picSizeList.stream().filter(size -> size >= 500 * 1024 && size < 1024 * 1024).count());
        picSizeRangeMap.put(">1MB", picSizeList.stream().filter(size -> size >= 1024 * 1024).count());
        // 将map中的数据放入空间图片大小分析响应集合中
        List<SpaceSizeAnalyzeResponse> spaceSizeAnalyzeResponses = picSizeRangeMap.entrySet().stream()
                .map(picSizeRange -> new SpaceSizeAnalyzeResponse(picSizeRange.getKey(), picSizeRange.getValue()))
                .collect(Collectors.toList());
        return spaceSizeAnalyzeResponses;
    }

    /**
     * 获取空间用户上传图片行为分析结果
     * @param spaceUserAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceUserAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        String timeDimension = spaceUserAnalyzeRequest.getTimeDimension();
        ThrowUtils.throwIf(StrUtil.isBlank(timeDimension), ErrorCode.PARAMS_ERROR, "时间维度不能为空");
        // 校验权限（仅管理员或本人可操作）
        checkSpaceAnalyzeAuth(spaceUserAnalyzeRequest, loginUser);
        // 补充查询范围
        QueryWrapper<Picture> pictureQueryWrapper = new QueryWrapper<>();
        fillSpaceAnalyzeQueryWrapper(spaceUserAnalyzeRequest, pictureQueryWrapper);
        // 按时间维度查询
        switch (timeDimension) {
            case "day":
                pictureQueryWrapper.select("DATE_FORMAT(create_time, '%Y_%m_%d) as period", "COUNT(*) as count");
                break;
            case "week":
                pictureQueryWrapper.select("YEARWEEK(create_time) as period", "COUNT(*) as count");
                break;
            case "month":
                pictureQueryWrapper.select("DATE_FORMAT(create_time, '%Y_%m) as period", "COUNT(*) as count");
                break;
            default:
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持该时间维度");
        }
        // 分组、排序
        pictureQueryWrapper.groupBy("period").orderByAsc("period");
        // 将map中的数据转到空间用户分析响应中
        List<Map<String, Object>> periodMaps = pictureService.getBaseMapper().selectMaps(pictureQueryWrapper);
        List<SpaceUserAnalyzeResponse> spaceUserAnalyzeResponses = periodMaps.stream()
                .map(periodMap -> {
                    String period = (String) periodMap.get("period");
                    Long count = (Long) periodMap.get("count");
                    return new SpaceUserAnalyzeResponse(period, count);
                })
                .collect(Collectors.toList());
        return spaceUserAnalyzeResponses;
    }

    /**
     * 获取空间使用排行分析结果
     * @param spaceRankAnalyzeRequest
     * @param loginUser
     * @return
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        // 校验参数
        ThrowUtils.throwIf(spaceRankAnalyzeRequest == null, ErrorCode.PARAMS_ERROR);
        // 校验权限（仅管理员可操作）
        ThrowUtils.throwIf(!userService.isAdmin(loginUser), ErrorCode.NO_AUTH_ERROR);
        // 查询前N个空间
        QueryWrapper<Space> spaceQueryWrapper = new QueryWrapper<>();
        spaceQueryWrapper.select("id", "totalSize")
                .orderByDesc("totalSize")
                .last("limit" + spaceRankAnalyzeRequest.getTopN());
        List<Space> spaceList = spaceService.list(spaceQueryWrapper);
        return spaceList;
    }
}




