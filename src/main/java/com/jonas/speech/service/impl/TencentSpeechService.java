package com.jonas.speech.service.impl;

import cn.hutool.json.JSON;
import cn.hutool.json.JSONUtil;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.common.profile.ClientProfile;
import com.tencentcloudapi.common.profile.HttpProfile;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Base64;

/**
 * <a href="https://cloud.tencent.com/document/api/551/15619">腾讯翻译</a>
 *
 * @author shenjy
 * @time 2023/8/30 19:56
 */
@Slf4j
@Service(SpeechType.TENCENT)
public class TencentSpeechService extends SpeechService {

    @Value("${speech.tencent.appId}")
    private String appId;
    @Value("${speech.tencent.keyId}")
    private String keyId;
    @Value("${speech.tencent.keySecret}")
    private String keySecret;
    @Value("${speech.tencent.region}")
    private String region;

    private AsrClient client;

    @PostConstruct
    public void init() {
        HttpProfile httpProfile = new HttpProfile();
        httpProfile.setEndpoint("asr.tencentcloudapi.com");
        // 实例化一个client选项，可选的，没有特殊需求可以跳过
        ClientProfile clientProfile = new ClientProfile();
        clientProfile.setHttpProfile(httpProfile);

        Credential cred = new Credential(keyId, keySecret);
        client = new AsrClient(cred, "", clientProfile);
    }


    @Override
    public String speechToText(byte[] audioData) {
        try {
            SentenceRecognitionRequest request = new SentenceRecognitionRequest();
            request.setEngSerViceType("8k_zh");
            request.setVoiceFormat("wav");
            request.setSourceType(1L);
            request.setData(Base64.getEncoder().encodeToString(audioData));
            SentenceRecognitionResponse response = client.SentenceRecognition(request);
            log.info("tencent speech to text response:{}", JSONUtil.toJsonStr(response));
            return response.getResult();
        } catch (TencentCloudSDKException e) {
            log.error("tencent speech to text error", e);
        }
        return "";
    }

    @Override
    public byte[] textToSpeech(String text) {
        return new byte[0];
    }
}
