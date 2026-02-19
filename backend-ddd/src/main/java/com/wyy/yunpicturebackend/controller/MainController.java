package com.wyy.yunpicturebackend.controller;

import com.wyy.yunpicture.infrastructure.common.BaseResponse;
import com.wyy.yunpicture.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {

    @GetMapping("/health")
    public BaseResponse<String> health(){
        return ResultUtils.success("healthy!");
    }
}
