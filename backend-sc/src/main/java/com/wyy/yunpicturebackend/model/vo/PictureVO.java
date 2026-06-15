package com.wyy.yunpicturebackend.model.vo;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wyy.yunpicturebackend.model.entity.Picture;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 返回给前端的图片封装类
 */
@Data
public class PictureVO implements Serializable {

    private static final long serialVersionUID = -2944061688425101226L;

    /**
     * 主键
     */
    private Long id;

    /**
     * 图片的url
     */
    private String url;

    /**
     * 缩略图 url
     */
    private String thumbnailUrl;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 图片简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（json数组）
     */
    private List<String> tags;

    /**
     * 图片体积
     */
    private Long picSize;

    /**
     * 图片宽度
     */
    private Integer picWidth;

    /**
     * 图片高度
     */
    private Integer picHeight;

    /**
     * 图片宽高比例
     */
    private Double picScale;

    /**
     * 图片格式
     */
    private String picFormat;

    /**
     * 图片主色调
     */
    private String picColor;

    /**
     * 创建的用户id
     */
    private Long userId;

    /**
     * 空间 id
     */
    private Long spaceId;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 更新时间
     */
    private Date updateTime;


    /**
     * 上传该图片的用户信息
     */
    private UserVO userVO;

    /**
     * 权限列表
     */
    private List<String> permissionList = new ArrayList<>();

    /**
     * 将封装类转为对象
     * @param pictureVO
     * @return
     */
    public static Picture voToObj(PictureVO pictureVO){
        if (pictureVO == null) {
            return null;
        }
        Picture picture = new Picture();
        BeanUtil.copyProperties(pictureVO, picture);
        picture.setTags(JSONUtil.toJsonStr(pictureVO.getTags()));
        return picture;
    }

    /**
     * 将对象转为封装类
     * @param picture
     * @return
     */
    public static PictureVO objToVo(Picture picture){
        if (picture == null) {
            return null;
        }
        PictureVO pictureVO = new PictureVO();
        BeanUtil.copyProperties(picture, pictureVO);
        // 从旧 JSON 字段解析标签，null 时返回空列表避免前端 v-for 报错
        List<String> tagList = JSONUtil.toList(picture.getTags(), String.class);
        pictureVO.setTags(tagList != null ? tagList : new ArrayList<>());
        return pictureVO;
    }
}