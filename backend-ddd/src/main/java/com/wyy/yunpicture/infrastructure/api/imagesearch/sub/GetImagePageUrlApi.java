package com.wyy.yunpicture.infrastructure.api.imagesearch.sub;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.json.JSONObject;
import com.wyy.yunpicture.infrastructure.exception.BusinessException;
import com.wyy.yunpicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {

    /**
     * 通过图片Url调用以图搜图接口
     * @param imageUrl
     * @return
     */
    public static String getImagePageUrl(String imageUrl){
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");

        //调用百度的以图搜图的上传图片的接口
        long upTime = System.currentTimeMillis();
        String url = "https://graph.baidu.com/upload?uptime=" + upTime;
        try {
            // 1. 第一步：先访问百度识图的首页 (GET 请求)
            // 目的：让服务器给我们发一个全新的 Cookie (BAIDUID)
            HttpResponse homeResponse = HttpRequest.get("https://graph.baidu.com/pcpage/index?tpl_from=pc")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36") // 必须带 UA
                    .execute();

            // 2. 第二步：从响应里拿到 Cookie
            // Hutool 的 HttpResponse 自带这个功能
            String cookies = homeResponse.getCookieStr();

            HttpResponse response = HttpRequest.post(url)
                    .header("acs-token", RandomUtil.randomString(1))
                    // 伪装成 Chrome 浏览器
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36")
                    // 告诉百度，我是从百度识图首页来的
                    .header("Referer", "https://graph.baidu.com/pcpage/index?tpl_from=pc")
                    .header("Cookie", cookies)
                    .form(formData)
                    .timeout(5000)
                    .execute();
            int httpStatus = response.getStatus();
            if (HttpStatus.HTTP_OK != httpStatus) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败（HTTP状态码：" + httpStatus + "）");
            }
            String responseBody = response.body();
            JSONObject result = new JSONObject(responseBody);
            int businessStatus = result.getInt("status");
            if (0 != businessStatus) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败（业务状态码：" + businessStatus + "，响应体：" + responseBody + "）");
            }
            JSONObject data = result.getJSONObject("data");
            String rawUrl = data.getStr("url");
            if (rawUrl == null) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效结果");
            }
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            return searchResultUrl;
        } catch (Exception e) {
            log.error("以图搜图上传图片接口调用失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        //测试调用以图搜图上传图片接口
        String imageUrl = "https://wyy-1364223088.cos.ap-beijing.myqcloud.com/yun-picture/public/1944740582778175490/2025-10-19_7wVowaNNVZkWRzCA.jpg";
        System.out.println("调用以图搜图上传图片接口返回的Url为：" + GetImagePageUrlApi.getImagePageUrl(imageUrl));
    }
}
