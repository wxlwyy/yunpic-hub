package com.wyy.yunpicturebackend.api.imagesearch;

import com.wyy.yunpicturebackend.api.imagesearch.model.ImageSearchResult;
import com.wyy.yunpicturebackend.api.imagesearch.sub.GetImageFirstUrlApi;
import com.wyy.yunpicturebackend.api.imagesearch.sub.GetImageListApi;
import com.wyy.yunpicturebackend.api.imagesearch.sub.GetImagePageUrlApi;

import java.util.List;

public class ImageSearchApiFacade {

    /**
     * 以图搜图
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        String imageUrl = "https://wyy-1364223088.cos.ap-beijing.myqcloud.com/yun-picture/public/1944740582778175490/2025-10-19_7wVowaNNVZkWRzCA.jpg";
        System.out.println("图片列表为：" + ImageSearchApiFacade.searchImage(imageUrl));
    }
}
