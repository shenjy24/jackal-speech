package com.jonas.speech.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 语音业务服务类
 *
 * @author shenjy
 * @time 2024/4/23 15:36
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechBusinessService {

    private final SpeechServiceFactory speechServiceFactory;

    /**
     * 识别语音，转成文本
     * 同步方法
     *
     * @param audioData 音频二进制数据
     * @return 文本
     */
    public String speechToText(byte[] audioData) {
        SpeechService speechService = speechServiceFactory.getService();
        return speechService.speechToText(audioData);
    }

    /**
     * 识别文本，转成语音
     *
     * @param text 文本
     * @return 音频二进制数据
     */
    public byte[] textToSpeech(String text) {
        SpeechService speechService = speechServiceFactory.getService();
        return speechService.textToSpeech(text);
    }
}
