package com.jonas.speech.service.impl;

import cn.hutool.core.codec.Base64;
import cn.hutool.json.JSONUtil;
import cn.xfyun.api.TtsClient;
import cn.xfyun.model.response.TtsResponse;
import cn.xfyun.service.tts.AbstractTtsWebSocketListener;
import com.google.gson.Gson;
import com.jonas.speech.common.SpeechType;
import com.jonas.speech.service.SpeechService;
import com.jonas.speech.service.SpeechToTextCallback;
import com.jonas.speech.service.SseService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Response;
import okhttp3.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

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

    private final SseService sseService;

    private TtsClient ttsClient;
    private String wsUrl;

    public static final Gson gson = new Gson();

    @PostConstruct
    public void init() {
        // 设置合成参数,这里的appid,apiKey,apiSecret是在开放平台控制台获得
        try {
            ttsClient = new TtsClient.Builder()
                    .signature(appId, apiKey, apiSecret)
                    .vcn(vcn)  // 发音人
                    .build();

            wsUrl = getAuthUrl("https://tts-api.xfyun.cn/v2/tts", apiKey, apiSecret)
                    .replace("https://", "wss://");
            log.info("ws url: {}", wsUrl);
        } catch (Exception e) {
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
            URI uri = new URI(wsUrl);
            WebSocketClient webSocketClient = new WebSocketClient(uri) {
                @Override
                public void onOpen(ServerHandshake serverHandshake) {
                    log.info("ws建立连接成功...");
                }

                @Override
                public void onMessage(String text) {
                    log.info(text);
                    JsonParse myJsonParse = gson.fromJson(text, JsonParse.class);
                    if (myJsonParse.code != 0) {
                        System.out.println("发生错误，错误码为：" + myJsonParse.code);
                        System.out.println("本次请求的sid为：" + myJsonParse.sid);
                    }
                    if (myJsonParse.data != null) {
                        try {
                            sseService.pushObj(clientId, myJsonParse.data.audio);
                        } catch (Exception e) {
                            log.error("decode error", e);
                        }
                        if (myJsonParse.data.status == 2) {
                            log.info("本次请求的sid==>" + myJsonParse.sid);
                        }
                    }
                }

                @Override
                public void onClose(int i, String s, boolean b) {
                    log.info("ws链接已关闭，本次请求完成...");
                }

                @Override
                public void onError(Exception e) {
                    log.error("发生错误 " + e.getMessage());
                }
            };
            // 建立连接
            webSocketClient.connect();
            while (!webSocketClient.getReadyState().equals(org.java_websocket.WebSocket.READYSTATE.OPEN)) {
                //System.out.println("正在连接...");
                Thread.sleep(100);
            }
            this.sendWsRequest(text, webSocketClient);
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void textToSpeechStreamWithSdk(String text, Long clientId) {
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

    // 鉴权方法
    public String getAuthUrl(String hostUrl, String apiKey, String apiSecret) throws Exception {
        URL url = new URL(hostUrl);
        // 时间
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String date = format.format(new Date());
        // 拼接
        String preStr = "host: " + url.getHost() + "\n" +
                "date: " + date + "\n" +
                "GET " + url.getPath() + " HTTP/1.1";
        //System.out.println(preStr);
        // SHA256加密
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(StandardCharsets.UTF_8), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(preStr.getBytes(StandardCharsets.UTF_8));
        // Base64加密
        String sha = java.util.Base64.getEncoder().encodeToString(hexDigits);
        // 拼接
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", apiKey, "hmac-sha256", "host date request-line", sha);
        // 拼接地址
        HttpUrl httpUrl = Objects.requireNonNull(HttpUrl.parse("https://" + url.getHost() + url.getPath())).newBuilder().//
                addQueryParameter("authorization", java.util.Base64.getEncoder().encodeToString(authorization.getBytes(StandardCharsets.UTF_8))).//
                addQueryParameter("date", date).//
                addQueryParameter("host", url.getHost()).//
                build();

        return httpUrl.toString();
    }

    // 线程来发送音频与参数
    private void sendWsRequest(String text, WebSocketClient webSocketClient) {
        try {
            //请求参数json串
            String requestJson = "{\n" +
                    "  \"common\": {\n" +
                    "    \"app_id\": \"" + appId + "\"\n" +
                    "  },\n" +
                    "  \"business\": {\n" +
                    "    \"aue\": \"lame\",\n" +
                    "    \"sfl\": 1,\n" +
                    "    \"tte\": \"" + "UTF8" + "\",\n" +
                    "    \"ent\": \"intp65\",\n" +
                    "    \"vcn\": \"" + vcn + "\",\n" +
                    "    \"pitch\": 50,\n" +
                    "    \"speed\": 50\n" +
                    "  },\n" +
                    "  \"data\": {\n" +
                    "    \"status\": 2,\n" +
                    "    \"text\": \"" + java.util.Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8)) + "\"\n" +
                    "  }\n" +
                    "}";
            webSocketClient.send(requestJson);
        } catch (Exception e) {
            log.error("send error", e);
        }
    }

    //返回的json结果拆解
    class JsonParse {
        int code;
        String sid;
        Data data;
    }

    class Data {
        int status;
        String audio;
    }
}
