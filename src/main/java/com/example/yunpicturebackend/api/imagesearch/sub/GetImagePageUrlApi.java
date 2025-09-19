package com.example.yunpicturebackend.api.imagesearch.sub;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.*;
import cn.hutool.json.JSONUtil;
import com.example.yunpicturebackend.exception.BusinessException;
import com.example.yunpicturebackend.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class GetImagePageUrlApi {
    public static String getImagePageUrl(String imageUrl) {
        //准备请求参数
        Map<String, Object> formData = new HashMap<>();
        formData.put("image", imageUrl);
        formData.put("tn", "pc");
        formData.put("from", "pc");
        formData.put("image_source", "PC_UPLOAD_URL");
        formData.put("sdkParams", "{\"data\":\"abf74eb921ad0b4437abfb912734cdc19607886669fa92bc2d89fb772b35b5006491c7ce505ed470a20436097d0b8c4606a54539b1923fb1e85778cc896797702827b36d6394e873cf806f2027043ee1d1dfa8e96f15769115589a12a177a5a1\",\"key_id\":\"23\",\"sign\":\"feca9e89\"}");
        //获取时间戳
        long uptime = System.currentTimeMillis();
        //请求地址
        String url = "https://graph.baidu.com/upload?uptime=" + uptime;
        try {
            //发送请求
            HttpResponse httpResponse = HttpRequest.post(url)
                    .form(formData)
                    .header("Acs-Token", "1754799642797_1754831612191_rxCTecR3RJ33iP2GPg0oif7e5NkF5I+ktZd+rp5zb3cbfD2M3Y2kaGK8bZ1VwiG581gACXbNkkznl2nb8EPVxBA7/5ZMtx2sWmsaZwiufjfxKz697PuGCNaRHH2KA9+hzLvE/EcD4ZXsS7+dWjfoCtLUc1MRzq5RBVhl5E2naTw6M0nHk/UgZ8z0FmmFn0lo0fhQ85vcastnhvHioOIYQQ3IcBD9J8zqvTijiwmnjrzFGNQdqrQu/iJWaacGGVtycRhAtFRYrm/3sFNvJfqZY0yPVxwDl9JiOKqhY2wIltANIHMaliQBN7Co5FSmhTSrlJ6Vzk+mN/gHcVUcPY9/xiMRisiLEjGLTqyzCwrwNvzeygXJbSJ3JrJr2Hqy0VDAa0tlq2APK3mcuKh124dhD5NDxVHfRFW0KD4Q2+PfnSpKMlapKkkeamo9oNmNNis9bCdYQ2mJ1xVPVf6yRBDHXvim7Xc00Hg/xCPbjyk+l30=")
                    .timeout(5000)
                    .execute();
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                log.error("调用百度以图搜图接口失败，状态码: {}", httpResponse.getStatus());
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败，状态码: " + httpResponse.getStatus());
            }
            //解析响应
            String body = httpResponse.body();
            Map<String, Object> result = JSONUtil.toBean(body, Map.class);

            //处理响应结果
            if (result == null || !Integer.valueOf(0).equals(result.get("status"))) {
                log.error("调用百度以图搜图接口失败，响应内容: {}", body);
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "接口调用失败，响应内容: " + body);
            }
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            String rawUrl = (String) data.get("url");
            String searchResultUrl = URLUtil.decode(rawUrl, StandardCharsets.UTF_8);
            //如果url为空
            if (StrUtil.isBlank(searchResultUrl)) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "未返回有效的结果地址");
            }
            return searchResultUrl;
        } catch (Exception e) {
            log.error("调用百度以图搜图接口失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "搜索失败: " + e.getMessage());
        }

    }

    public static void main(String[] args) {
        String imageUrl = "https://yun-picture-1-1372675861.cos.ap-guangzhou.myqcloud.com/public/1951831759600001025/2025-08-07 zQks1JPxBm6751Mz png";
        String searchResultUrl = getImagePageUrl(imageUrl);
        System.out.println("搜索成功，结果URL：" + searchResultUrl);
    }
}


