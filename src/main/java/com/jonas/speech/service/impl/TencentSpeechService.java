package com.jonas.speech.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import com.tencentcloudapi.asr.v20190614.AsrClient;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionRequest;
import com.tencentcloudapi.asr.v20190614.models.SentenceRecognitionResponse;
import com.tencentcloudapi.common.Credential;
import com.tencentcloudapi.common.exception.TencentCloudSDKException;
import com.tencentcloudapi.tts.v20190823.TtsClient;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceRequest;
import com.tencentcloudapi.tts.v20190823.models.TextToVoiceResponse;
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

    @Value("${speech.tencent.keyId}")
    private String keyId;
    @Value("${speech.tencent.keySecret}")
    private String keySecret;

    // 语音转文本客户端
    private AsrClient asrClient;
    // 文本转语音客户端
    private TtsClient ttsClient;

    @PostConstruct
    public void init() {
        Credential cred = new Credential(keyId, keySecret);

        asrClient = new AsrClient(cred, "");
        ttsClient = new TtsClient(cred, "ap-shanghai");
    }


    @Override
    public String speechToText(byte[] audioData) {
        try {
            SentenceRecognitionRequest request = new SentenceRecognitionRequest();
            request.setEngSerViceType("8k_zh");
            request.setVoiceFormat("wav");
            request.setSourceType(1L);
            request.setData(Base64.getEncoder().encodeToString(audioData));
            SentenceRecognitionResponse response = asrClient.SentenceRecognition(request);
            log.info("tencent speech to text response:{}", JSONUtil.toJsonStr(response));
            return response.getResult();
        } catch (TencentCloudSDKException e) {
            log.error("tencent speech to text error", e);
        }
        return "";
    }

    @Override
    public void speechToText(byte[] audioData, SpeechToTextCallback callback) {
        throw new RuntimeException("暂不支持该操作");
    }

    @Override
    public String textToSpeech(String text) {
        try {
            TextToVoiceRequest req = new TextToVoiceRequest();
            req.setText(text);
            req.setSessionId("tts-" + IdUtil.simpleUUID());
            // 返回的resp是一个TextToVoiceResponse的实例，与请求对象对应
            TextToVoiceResponse resp = ttsClient.TextToVoice(req);
            log.info("tencent text to speech response: {}", JSONUtil.toJsonStr(resp));
            return resp.getAudio();
        } catch (TencentCloudSDKException e) {
            log.error("tencent text to speech error", e);
        }
        return "";
    }
}
