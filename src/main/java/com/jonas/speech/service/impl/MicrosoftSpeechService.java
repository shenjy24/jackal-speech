package com.jonas.speech.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import com.jonas.speech.service.SseService;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioInputStream;
import com.microsoft.cognitiveservices.speech.audio.PushAudioInputStream;
import com.vdurmont.emoji.EmojiParser;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.AsyncEvent;
import jakarta.servlet.AsyncListener;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import okio.Buffer;
import okio.BufferedSource;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 微软语言服务
 * <a href="https://learn.microsoft.com/zh-cn/azure/ai-services/speech-service/language-support?tabs=tts">语音列表</a>
 *
 * @author shenjy
 * @time 2024/4/15 11:12
 */
@Slf4j
@RequiredArgsConstructor
@Service(SpeechType.MICROSOFT)
public class MicrosoftSpeechService extends SpeechService {

    private final SseService sseService;

    @Value("${speech.microsoft.key}")
    private String speechKey;
    @Value("${speech.microsoft.region}")
    private String speechRegion;

    // 获取访问令牌接口
    private String issueTokenUrl;
    // 文字转语音接口
    private String textToSpeechUrl;
    // 语音转文字接口
    private String speechToTextUrl;

    private final Map<String, String> recognizeMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        String issueTokenUrlTemplate = "https://{}.api.cognitive.microsoft.com/sts/v1.0/issueToken";
        issueTokenUrl = StrUtil.format(issueTokenUrlTemplate, speechRegion);

        String textToSpeechUrlTemplate = "https://{}.tts.speech.microsoft.com/cognitiveservices/v1";
        textToSpeechUrl = StrUtil.format(textToSpeechUrlTemplate, speechRegion);

        String speechToTextTemplate = "https://{}.stt.speech.microsoft.com/speech/recognition/conversation/cognitiveservices/v1?language=zh-CN";
        speechToTextUrl = StrUtil.format(speechToTextTemplate, speechRegion);
    }

    @Override
    public String speechToText(byte[] audioData) {
        // 获取访问令牌
        String token = this.issueToken();
        if (StrUtil.isBlank(token)) {
            log.error("token 为空");
            return "";
        }

        HttpRequest request = HttpRequest.post(speechToTextUrl)
                .header("Ocp-Apim-Subscription-Key", speechKey)
                .header("Content-type", "audio/wav; codecs=audio/pcm; samplerate=16000")
                .header("Accept", "application/json")
                .body(audioData)
                .timeout(50000);
        try (HttpResponse response = request.execute()) {
            String body = response.body();
            log.info("microsoft speech to text response: {}", body);
            JSONObject jsonObject = JSONUtil.parseObj(body);
            return jsonObject.getStr("DisplayText");
        }
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
        // 获取访问令牌
        String token = this.issueToken();
        if (StrUtil.isBlank(token)) {
            log.error("token 为空");
            return "";
        }
        log.info("microsoft token {}", token);

        return textToSpeechWithHttp(EmojiParser.removeAllEmojis(text), token);
    }

    /**
     * 文本转语音
     */
    private String textToSpeechWithHttp(String text, String token) {
        // https://learn.microsoft.com/zh-cn/azure/ai-services/speech-service/language-support?tabs=tts
        String requestBodyTemplate = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\" xml:lang=\"zh-CN\"><voice name=\"zh-CN-XiaoyouNeural\" effect=\"eq_car\">{}</voice></speak>";
        String requestBody = StrUtil.format(requestBodyTemplate, text);

        HttpRequest request = HttpRequest.post(textToSpeechUrl)
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/ssml+xml")
                .header("X-Microsoft-OutputFormat", "riff-8khz-16bit-mono-pcm")
                .header("User-Agent", "OpenFactor")
                .body(requestBody)
                .timeout(50000);
        try (HttpResponse response = request.execute()) {
            return Base64.encode(response.bodyBytes());
        }
    }

    /**
     * 文本转语音，使用SDK，但是SDK在Docker中会有依赖问题
     *
     * @param text 文本
     * @return 语音
     */
    private String textToSpeechWithSdk(String text) {
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

    /**
     * 获取访问令牌，有效期10分钟
     *
     * @return 访问令牌
     */
    private String issueToken() {
        HttpRequest request = HttpRequest.post(issueTokenUrl)
                .header("Ocp-Apim-Subscription-Key", speechKey)
                .timeout(2000);
        try (HttpResponse response = request.execute()) {
            return response.body();
        }
    }

    @Override
    public void textToSpeechStream(String text, Long clientId) {
        // 获取访问令牌
        String token = this.issueToken();
        if (StrUtil.isBlank(token)) {
            log.error("token 为空");
            return;
        }

        // https://learn.microsoft.com/zh-cn/azure/ai-services/speech-service/language-support?tabs=tts
        String requestBodyTemplate = "<speak version=\"1.0\" xmlns=\"http://www.w3.org/2001/10/synthesis\" xmlns:mstts=\"https://www.w3.org/2001/mstts\" xml:lang=\"zh-CN\"><voice name=\"zh-CN-XiaoyouNeural\" effect=\"eq_car\">{}</voice></speak>";
        String requestBody = StrUtil.format(requestBodyTemplate, text);

        log.info("microsoft token {}", token);

        OkHttpClient client = new OkHttpClient();
        Request req = new Request.Builder()
                .url(textToSpeechUrl)
                .post(RequestBody.create(requestBody.getBytes(StandardCharsets.UTF_8)))
                .addHeader("Authorization", "Bearer " + token)
                .addHeader("Content-Type", "application/ssml+xml")
                .addHeader("X-Microsoft-OutputFormat", "raw-8khz-8bit-mono-alaw")
                .addHeader("User-Agent", "OpenFactor")
                .build();

        log.info("microsoft request: {}", req);
        client.newCall(req).enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                log.error("microsoft on failure", e);
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                log.info("microsoft on response, {}", response);
                if (response.isSuccessful()) {
                    ResponseBody responseBody = response.body();
                    if (null != responseBody) {
                        BufferedSource source = responseBody.source();
                        // 请求整个响应体
                        source.request(Long.MAX_VALUE);
                        while (!source.exhausted()) {
                            Buffer buffer = new Buffer();
                            source.read(buffer, 1024);
                            byte[] data = buffer.readByteArray();
                            log.info("microsoft data {}", data);
                            sseService.pushObj(clientId, data);
                        }
                    }
                } else {
                    log.error("microsoft error: code={}, message={}", response.code(), response.message());
                }
            }
        });
    }
}
