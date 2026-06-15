package com.wyy.yunpicturebackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wyy.yunpicturebackend.model.entity.PictureTag;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
* @description 针对表【picture_tag(图片标签表)】的数据库操作Mapper
* @Entity com.wyy.yunpicturebackend.model.entity.PictureTag
*/
public interface PictureTagMapper extends BaseMapper<PictureTag> {

    /**
     * 批量插入标签，已存在的忽略（依赖 uk_name 唯一键）
     */
    int insertIgnoreBatch(@Param("names") List<String> names);

    /**
     * 根据名称列表批量查标签
     */
    List<PictureTag> selectByNames(@Param("names") List<String> names);
}
