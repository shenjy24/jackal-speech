package com.jonas.speech.service;

/**
 * 语音接口
 *
 * @author shenjy
 * @time 2023/8/30 9:19
 */
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
     * 识别文本，转成语音
     *
     * @param text 文本
     * @return 音频二进制数据
     */
    public abstract String textToSpeech(String text);
}
