package com.jonas.speech.service.impl;

import com.alibaba.fastjson.JSONPath;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import com.jonas.speech.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * <a href="http://api.fanyi.baidu.com/product/113">百度翻译</a>
 *
 * @author shenjy
 * @time 2023/8/30 9:53
 */
@Slf4j
@Service(SpeechType.ALIYUN)
public class AliyunSpeechService extends SpeechService {

    @Value("${speech.aliyun.appKey}")
    private String appKey;
    @Value("${speech.aliyun.accessKeyId}")
    private String accessKeyId;
    @Value("${speech.aliyun.accessKeySecret}")
    private String accessKeySecret;
    @Value("${speech.aliyun.url}")
    private String url;


    @Override
    public String speechToText(byte[] audioData) {
        /*
         * 设置HTTPS RESTful POST请求：
         * 1.使用HTTPS协议。
         * 2.语音识别服务域名：nls-gateway-cn-shanghai.aliyuncs.com。
         * 3.语音识别接口请求路径：/stream/v1/asr。
         * 4.设置必选请求参数：appkey、format、sample_rate。
         * 5.设置可选请求参数：enable_punctuation_prediction、enable_inverse_text_normalization、enable_voice_detection。
         */
        String request = url;
        request = request + "?appkey=" + appKey;
        request = request + "&format=" + "wav";
        request = request + "&sample_rate=" + 16000;
        request = request + "&enable_punctuation_prediction=" + true;
        request = request + "&enable_inverse_text_normalization=" + true;
        request = request + "&enable_voice_detection=" + true;
        log.info("Request: {}", request);

        /*
         * 设置HTTPS头部字段：
         * 1.鉴权参数。
         * 2.Content-Type：application/octet-stream。
         */
        Map<String, String> headers = new HashMap<>();
        headers.put("X-NLS-Token", "35a8a6b6da6e4e0cbc3044bffdab3a57");
        headers.put("Content-Type", "application/octet-stream");

        /*
         * 发送HTTPS POST请求，返回服务端的响应。
         */
        String response = HttpUtil.sendPostData(request, headers, audioData);
        if (response != null) {
            log.info("Response: {}", response);
            return JSONPath.read(response, "result").toString();
        }
        return "";
    }

    @Override
    public void speechToText(byte[] audioData, SpeechToTextCallback callback) {
        throw new RuntimeException("暂不支持该操作");
    }

    @Override
    public String textToSpeech(String text) {
        throw new RuntimeException("暂不支持该操作");
    }
}
