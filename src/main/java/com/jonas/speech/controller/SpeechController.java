package com.jonas.speech.controller;

import com.jonas.speech.service.SpeechBusinessService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * SpeechController
 *
 * @author shenjy
 * @time 2024/4/23 15:47
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/speech")
public class SpeechController {
    private final SpeechBusinessService speechService;

    /**
     * 识别语音，转成文本
     * 同步方法
     *
     * @param audioData 音频二进制数据
     */
    @SneakyThrows
    @PostMapping("/speechToText")
    public String speechToText(@RequestParam MultipartFile audioData) {
        return speechService.speechToText(audioData.getBytes());
    }

    /**
     * 识别文本，转成语音 //为了测试效果，直接进行播放，晚点将改为音频文件输出
     *
     * @param text 文本
     */
    @SneakyThrows
    @PostMapping("/textToSpeech")
    public String textToSpeech(@RequestParam String text) {
        return speechService.textToSpeech(text);
    }

    /**
     * 识别文本，转成语音 //为了测试效果，直接进行播放，晚点将改为音频文件输出
     *
     * @param text 文本
     */
    @SneakyThrows
    @PostMapping("/textToSpeechStream")
    public void textToSpeechStream(@RequestParam String text, @RequestParam Long clientId) {
        speechService.textToSpeechStream(text, clientId);
    }
}
