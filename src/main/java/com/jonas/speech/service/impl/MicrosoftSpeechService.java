package com.jonas.speech.service.impl;

import cn.hutool.core.codec.Base64;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微软语言服务
 *
 * @author shenjy
 * @time 2024/4/15 11:12
 */
@Slf4j
@RequiredArgsConstructor
@Service(SpeechType.MICROSOFT)
public class MicrosoftSpeechService extends SpeechService {

    @Value("${speech.microsoft.key}")
    private String speechKey;
    @Value("${speech.microsoft.region}")
    private String speechRegion;

    private final Map<String, String> recognizeMap = new ConcurrentHashMap<>();

    @Override
    public String speechToText(byte[] audioData) {
        throw new RuntimeException("暂不支持该操作");
    }

    @Override
    public void speechToText(byte[] audioData, SpeechToTextCallback callback) {

        try (SpeechConfig speechConfig = SpeechConfig.fromSubscription(speechKey, speechRegion)) {
            speechConfig.setSpeechRecognitionLanguage("zh-CN");
            // 转成 AudioInputStream
            PushAudioInputStream audioInputStream = AudioInputStream.createPushStream();
            audioInputStream.write(audioData);
            try (AudioConfig audioConfig = AudioConfig.fromStreamInput(audioInputStream)) {
                // 告诉Azure数据流已结束
                audioInputStream.close();

                SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);
                {
                    // Subscribes to events.
                    speechRecognizer.recognizing.addEventListener((s, e) -> {
                        log.info("RECOGNIZING: Session={}, Text={}", e.getSessionId(), e.getResult().getText());
                    });

                    speechRecognizer.recognized.addEventListener((s, e) -> {
                        if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                            log.info("RECOGNIZED: Session={}, Text={}", e.getSessionId(), e.getResult().getText());
                            String recognizedText = recognizeMap.getOrDefault(e.getSessionId(), "");
                            recognizeMap.put(e.getSessionId(), recognizedText + e.getResult().getText());
                        } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                            log.info("NOMATCH: Session={}, Speech could not be recognized.", e.getSessionId());
                        }
                    });

                    speechRecognizer.canceled.addEventListener((s, e) -> {
                        log.info("CANCELED: Session={}, Reason={}", e.getSessionId(), e.getReason());

                        if (e.getReason() == CancellationReason.Error) {
                            log.error("CANCELED: Session={}, ErrorCode={}", e.getSessionId(), e.getErrorCode());
                            log.error("CANCELED: Session={}, ErrorDetails={}", e.getSessionId(), e.getErrorDetails());
                        }

                        if (e.getReason() == CancellationReason.EndOfStream) {
                            // Stops recognition.
                            speechRecognizer.stopContinuousRecognitionAsync();
                            callback.onComplete(recognizeMap.remove(e.getSessionId()));
                        }
                    });

                    speechRecognizer.sessionStarted.addEventListener((s, e) -> {
                        log.info("Session {} started event.", e.getSessionId());
                    });

                    speechRecognizer.sessionStopped.addEventListener((s, e) -> {
                        log.info("Session {} stopped event.", e.getSessionId());
                    });

                    // Starts continuous recognition. Uses StopContinuousRecognitionAsync() to stop recognition.
                    speechRecognizer.startContinuousRecognitionAsync().get();
                }
            }
        } catch (Exception e) {
            log.error("microsoft recognize speech error", e);
        }
    }

    @Override
    public String textToSpeech(String text) {
        try {
            // 从订阅信息创建语音配置
            SpeechConfig config = SpeechConfig.fromSubscription(speechKey, speechRegion);

            // 设置语音合成器的语音名称
            config.setSpeechSynthesisVoiceName("zh-CN-XiaoxiaoNeural");

            // 创建音频配置，使用默认扬声器输出
            AudioConfig audioConfig = AudioConfig.fromDefaultSpeakerOutput();

            try (SpeechSynthesizer synthesizer = new SpeechSynthesizer(config, audioConfig)) {

                // 将文本合成为语音
                SpeechSynthesisResult result = synthesizer.SpeakTextAsync(text).get();

                // 检查结果
                if (result.getReason() == ResultReason.Canceled) {
                    SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
                    log.error("CANCELED: Reason=" + cancellation.getReason());
                    if (cancellation.getReason() == CancellationReason.Error) {
                        log.error("CANCELED: ErrorCode=" + cancellation.getErrorCode());
                        log.error("CANCELED: ErrorDetails=" + cancellation.getErrorDetails());
                        log.error("CANCELED: Did you set the speech resource key and region values?");
                    }
                    return null;
                }

                // 成功
                if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                    log.info("Speech synthesized for text: [" + text + "]");
                    return Base64.encode(result.getAudioData());
                }
            }
        } catch (Exception ex) {
            log.error("textToSpeech Exception: ", ex);
        }
        return null;
    }
}
