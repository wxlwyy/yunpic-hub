package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.wyy.yunpicturebackend.model.dto.spaceuser.SpaceUserQueryRequest;
import com.wyy.yunpicturebackend.model.entity.SpaceUser;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author wxl
* @description 针对表【space_user(空间成员表)】的数据库操作Service
* @createDate 2026-01-21 21:50:09
*/
public interface SpaceUserService extends IService<SpaceUser> {

    /**
     * 添加成员进入团队空间或编辑成员的角色时检验参数
     * @param spaceUser
     * @param add
     */
    void validateSpaceUserParams(SpaceUser spaceUser, boolean add);

    /**
     * 添加用户进入团队空间
     * @param spaceUserAddRequest
     * @return
     */
    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    /**
     * 获取查询包装类
     * @param spaceUserQueryRequest
     * @return
     */
    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    /**
     * 将空间成员列表脱敏
     * @param spaceUserList
     * @return
     */
    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);
}
