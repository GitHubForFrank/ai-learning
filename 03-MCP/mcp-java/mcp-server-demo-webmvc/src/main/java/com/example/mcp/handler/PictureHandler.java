package com.example.mcp.handler;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.io.FileUtil;
import com.alibaba.fastjson2.JSONObject;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * @author FrankKang
 * @since 2026-01-09 21:31
 */
@Service
public class PictureHandler {

    private static final String BASE_URL = "https://894q5dgyrz.coze.site/run";

    private final RestClient restClient;

    public PictureHandler() {
        String token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjI1NDgzMTMwLWQxYzAtNGZlNS05ZjJlLWRmNjU3OTFkMDJlNSJ9.eyJpc3MiOiJodHRwczovL2FwaS5jb3plLmNuIiwiYXVkIjpbInkwZThhNERnZUpMS0xEUzZ6THVQSGdRTGwwMHlFRjIyIl0sImV4cCI6ODIxMDI2Njg3Njc5OSwiaWF0IjoxNzY3OTYzMzQyLCJzdWIiOiJzcGlmZmU6Ly9hcGkuY296ZS5jbi93b3JrbG9hZF9pZGVudGl0eS9pZDo3NTkzMzQyMjc3MjEzMjI1MDA2Iiwic3JjIjoiaW5ib3VuZF9hdXRoX2FjY2Vzc190b2tlbl9pZDo3NTkzMzQ0NzM4MzQyNzMxODM5In0.GXWHEyakhAVchbYC2-gknM_7EeYbPcn8TzuYTfU9bV9FZL70P2ZUExF2t1NyA3JiGhK9cgGP_QujF3SQ4qCvKm6ieszQGeS4xj4qzo3cRyZDAfqOXOW-mJf1461CDAVykir_nUroo5c1E8e8HhjnSvpfipQhL8lXWKMFzVkk0-qWvoQJ-q1YkAiMz3LLLl5tAPDdhguf_eNy3p-1zAClJJvV_Y9RS2eI7nxRopYxm14Y_c200NoyZdC5yXMKR4ebXnaj7XGcrXQBK17X-ILRBCgQ9gQznRBuzQfh0x2UGWxsE-hXAp8YPdzUsAyWuwrthS_FKH3jAZVsW1jpVnsuqg";
        this.restClient = RestClient.builder()
                                    .baseUrl(BASE_URL)
                                    .defaultHeader("Content-Type", "application/json")
                                    .defaultHeader("Authorization", "Bearer " + token)
                                    .build();
    }

    public static void main(String[] args) {
        PictureHandler client = new PictureHandler();
        String filePath = "C:\\Users\\11260\\Desktop\\temp\\0 (1).jpg";
        System.out.println(client.getPicInfoByLocation(filePath));
    }

    @Tool(description = "获取图片信息，根据本地磁盘的图片路径")
    public String getPicInfoByLocation(String localPicPath) {
        String base64 = Base64.encode(FileUtil.readBytes(localPicPath));
        JSONObject jsonObj = new JSONObject();
        jsonObj.put("image_base64", base64);
        try {
            ResponseEntity<JSONObject> response = restClient.post()
                                                            .body(jsonObj)
                                                            .retrieve()
                                                            .toEntity(JSONObject.class);

            if (response.getStatusCode()
                        .value() == 200) {
                JSONObject body = response.getBody();
                if (body != null) {
                    return body.getString("extracted_text");
                }
                return null;
            } else {
                if (response.getBody() != null) {
                    return response.getBody()
                                   .toString();
                }
            }

        } catch (Exception e) {
            return ExceptionUtil.getSimpleMessage(e);
        }
        return "";
    }
}
