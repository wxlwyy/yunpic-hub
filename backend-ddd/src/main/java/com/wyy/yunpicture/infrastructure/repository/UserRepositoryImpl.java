package com.wyy.yunpicture.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wyy.yunpicture.domain.user.entity.User;
import com.wyy.yunpicture.domain.user.repository.UserRepository;
import com.wyy.yunpicture.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}