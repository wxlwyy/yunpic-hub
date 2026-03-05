package com.wyy.yunpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wyy.yunpicturebackend.model.dto.space.AddSpaceRequest;
import com.wyy.yunpicturebackend.model.dto.space.EditSpaceRequest;
import com.wyy.yunpicturebackend.model.dto.space.QuerySpaceRequest;
import com.wyy.yunpicturebackend.model.dto.space.UpdateSpaceRequest;
import com.wyy.yunpicturebackend.model.entity.Space;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wyy.yunpicturebackend.model.entity.User;
import com.wyy.yunpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
* @author wxl
* @description 针对表【space(空间)】的数据库操作Service
* @createDate 2025-11-19 17:15:39
*/
public interface SpaceService extends IService<Space> {

    /**
     * 校验新增、修改空间时的参数
     * @param space
     * @param add 是否是创建空间
     */
    void validateSpaceParams(Space space, boolean add);

    /**
     * 获取空间包装类（主要是给userVO赋值）
     * @param space
     * @return
     */
    SpaceVO convertToSpaceVO(Space space, HttpServletRequest request);

    /**
     * 获取空间包装类（分页）
     * @param spacePage
     * @return
     */
    Page<SpaceVO> convertToSpaceVOPage(Page<Space> spacePage);

    /**
     * 将查询条件封装为QueryWrapper对象
     * @param querySpaceRequest
     * @return
     */
    QueryWrapper<Space> buildQueryWrapper(QuerySpaceRequest querySpaceRequest);

    /**
     * 根据空间级别自动填充空间限额数据
     * @param space
     */
    void fillSpaceBySpaceLevel(Space space);

    /**
     * 创建空间
     * @return
     */
    long addSpace(AddSpaceRequest addSpaceRequest, User currentUser);

    /**
     * 校验访问空间的权限（仅本人或管理员可访问）
     * @param currentUser
     * @param space
     */
    void checkSpaceManageAuth(User currentUser, Space space);

    boolean updateSpaceInfo(UpdateSpaceRequest updateSpaceRequest);

    void deleteSpace(Long id, User currentUser);

    boolean editSpace(EditSpaceRequest editSpaceRequest, User currentUser);
}
