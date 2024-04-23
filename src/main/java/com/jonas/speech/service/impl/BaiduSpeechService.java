package com.jonas.speech.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONUtil;
import com.baidu.aip.speech.AipSpeech;
import com.baidu.aip.speech.TtsResponse;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;

/**
 * <a href="http://api.fanyi.baidu.com/product/113">百度翻译</a>
 *
 * @author shenjy
 * @time 2023/8/30 9:53
 */
@Slf4j
@Service(SpeechType.BAIDU)
public class BaiduSpeechService extends SpeechService {

    @Value("${speech.baidu.appId}")
    private String appId;
    @Value("${speech.baidu.apiKey}")
    private String apiKey;
    @Value("${speech.baidu.secretKey}")
    private String secretKey;

    private AipSpeech aipSpeech;

    @PostConstruct
    public void initAipSpeech() {
        aipSpeech = new AipSpeech(appId, apiKey, secretKey);
        aipSpeech.setConnectionTimeoutInMillis(2000);
        aipSpeech.setSocketTimeoutInMillis(30000);
    }

    @Override
    public String speechToText(byte[] audioData) {
        JSONObject res = aipSpeech.asr(audioData, "wav", 16000, null);
        log.info("baidu speech recognize, res: {}", res);
        JSONArray texts = res.getJSONArray("result");
        if (texts != null && texts.length() > 0) {
            return texts.getString(0);
        }
        return "";
    }

    @Override
    public void speechToText(byte[] audioData, SpeechToTextCallback callback) {
        throw new RuntimeException("暂不支持该操作");
    }

    @Override
    public String textToSpeech(String text) {
        HashMap<String, Object> options = new HashMap<>();
        options.put("aue", 6);
        TtsResponse response = aipSpeech.synthesis(text, "zh", 1, options);
        log.info("baidu speech synthesis, res: {}", JSONUtil.toJsonStr(response));
        return Base64.encode(response.getData());
    }
}
