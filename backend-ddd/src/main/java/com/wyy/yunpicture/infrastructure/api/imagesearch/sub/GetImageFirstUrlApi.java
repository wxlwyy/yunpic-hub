package com.wyy.yunpicture.infrastructure.api.imagesearch.sub;

import com.wyy.yunpicture.infrastructure.exception.BusinessException;
import com.wyy.yunpicture.infrastructure.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class GetImageFirstUrlApi {

    /**
     * 通过以图搜图界面的前端代码获取到firstUrl
     * @param imagePageUrl
     * @return
     */
    public static String getImageFirstUrl(String imagePageUrl) {
        try {
            //使用Jsoup获取稳定对象
            Document document = Jsoup.connect(imagePageUrl)
                    .timeout(5000)
                    .get();
            //获取到所有script标签,找出所有指定标签名的元素
            Elements ScriptElements = document.getElementsByTag("script");
            for (Element scriptElement : ScriptElements) {
                String scriptElementContent = scriptElement.html();
                if (scriptElementContent.contains("\"firstUrl\"")) {
                    Pattern pattern = Pattern.compile("\"firstUrl\"\\s*:\\s*\"(.*?)\"");
                    Matcher matcher = pattern.matcher(scriptElementContent);
                    if (matcher.find()) {
                        String firstUrl = matcher.group(1);
                        firstUrl = firstUrl.replace("\\/", "/");
                        return firstUrl;
                    }
                }
            }
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未找到url");
        } catch (Exception e) {
            log.error("获取firstUrl失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败");
        }
    }

    public static void main(String[] args) {
        // 请求目标 URL
        String url = "https://graph.baidu.com/s?card_key=&entrance=GENERAL&extUiData[isLogoShow]=1&f=all&isLogoShow=1&session_id=9702745121019538950&sign=12183784b7760d46782a201765843358&tpl_from=pc";
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(url);
        System.out.println("搜索成功，结果 URL：" + imageFirstUrl);
    }
}
