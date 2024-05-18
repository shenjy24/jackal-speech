package com.jonas.speech.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONUtil;
import cn.xfyun.api.TtsClient;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.security.SignatureException;

/**
 * 讯飞语音服务
 * <a href="https://github.com/iFLYTEK-OP/websdk-java?tab=readme-ov-file">讯飞语音服务</a>
 *
 * @author shenjy
 * @time 2024/5/15 23:31
 */
@Slf4j
@RequiredArgsConstructor
@Service(SpeechType.XUNFEI)
public class XunFeiSpeechService extends SpeechService {

    @Value("${speech.xunfei.appId}")
    private String appId;
    @Value("${speech.xunfei.apiKey}")
    private String apiKey;
    @Value("${speech.xunfei.apiSecret}")
    private String apiSecret;
    @Value("${speech.xunfei.vcn}")
    private String vcn;

    private TtsClient ttsClient;

    @PostConstruct
    public void init() {
        // 设置合成参数,这里的appid,apiKey,apiSecret是在开放平台控制台获得
        try {
            ttsClient = new TtsClient.Builder()
                    .signature(appId, apiKey, apiSecret)
                    .vcn(vcn)  // 发音人
                    .build();
        } catch (MalformedURLException | SignatureException e) {
            log.error("讯飞语音客户端初始化错误", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public String speechToText(byte[] audioData) {
        return null;
    }

    @Override
    public void speechToText(byte[] audioData, SpeechToTextCallback callback) {

    }

    @Override
    public String textToSpeech(String text) {
        return null;
    }

    @Override
    public void textToSpeechStream(String text, Long clientId) {
        try {
            ttsClient.send(text, new AbstractTtsWebSocketListener() {
                //返回格式为音频文件的二进制数组bytes
                @Override
                public void onSuccess(byte[] bytes) {
                    log.info("on success: " + Base64.encode(bytes));
                }

                //授权失败通过throwable.getMessage()获取对应错误信息
                @Override
                public void onFail(WebSocket webSocket, Throwable throwable, Response response) {
                    System.out.println(throwable.getMessage());
                    log.error("讯飞语音合成流式接口失败, {}", throwable.getMessage());
                }

                //业务失败通过ttsResponse获取错误码和错误信息
                @Override
                public void onBusinessFail(WebSocket webSocket, TtsResponse ttsResponse) {
                    log.error("讯飞语音合成流式接口业务失败, {}", ttsResponse);
                }
            });
        } catch (Exception e) {
            log.error("讯飞语音合成流式接口异常", e);
            throw new RuntimeException(e);
        }
    }
}
