package com.wyy.yunpicture.infrastructure.api.imagesearch.sub;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.wyy.yunpicture.infrastructure.api.imagesearch.model.ImageSearchResult;
import com.wyy.yunpicture.infrastructure.exception.BusinessException;
import com.wyy.yunpicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class GetImageListApi {

    /**
     * 根据firstUrl获取图片列表
     * @param imageFirstUrl
     * @return
     */
    public static List<ImageSearchResult> getImageList(String imageFirstUrl) {
        try {
            HttpResponse response = HttpRequest.get(imageFirstUrl).execute();
            int status = response.getStatus();
            if (status != HttpStatus.HTTP_OK) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败");
            }
            String responseBody = response.body();
            JSONObject jsonObject = new JSONObject(responseBody);
            if (!jsonObject.containsKey("data")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
            }
            JSONObject data = jsonObject.getJSONObject("data");
            if (!data.containsKey("list")) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未获取到图片列表");
            }
            JSONArray list = data.getJSONArray("list");
            List<ImageSearchResult> imageList = JSONUtil.toList(list, ImageSearchResult.class);
            return imageList;
        } catch (Exception e) {
            log.error("图片列表获取失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "图片列表获取失败");
        }
    }

    public static void main(String[] args) {
        String imageFirstUrl = "https://graph.baidu.com/ajax/pcsimi?carousel=503&entrance=GENERAL&extUiData%5BisLogoShow%5D=1&inspire=general_pc&limit=30&next=2&render_type=card&session_id=9702745121019538950&sign=12183784b7760d46782a201765843358&tk=7ddcd&tpl_from=pc";
        System.out.println("图片列表为：" + GetImageListApi.getImageList(imageFirstUrl));
    }
}
