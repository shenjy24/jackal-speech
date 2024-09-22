package com.jonas.speech.service;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * 语音接口
 *
 * @author shenjy
 * @time 2023/8/30 9:19
 */
@Slf4j
public abstract class SpeechService {

    /**
     * 识别语音，转成文本
     * 同步方法
     *
     * @param audioData 音频二进制数据
     * @return 文本
     */
    public abstract String speechToText(byte[] audioData);

    /**
     * 识别语音，转成文本
     * 异步方法
     *
     * @param audioData 音频二进制数据
     */
    public abstract void speechToText(byte[] audioData, SpeechToTextCallback callback);

    /**
     * 识别文本，转成语音
     *
     * @param text 文本
     * @return 音频二进制数据
     */
    public abstract String textToSpeech(String text);

    /**
     * 识别文本，转成语音
     * 流式接口
     *
     * @param text 文本
     */
    public abstract void textToSpeechStream(String text, Long clientId);

    public void writeResponse(HttpServletResponse response, Object data) {
        try {
//            response.setCharacterEncoding("UTF-8");
//            response.setContentType("application/json");
            response.getWriter().println(data);
        } catch (Exception e) {
            log.error("write response error", e);
        }
    }
}
