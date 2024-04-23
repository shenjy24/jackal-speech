package com.jonas.speech.service;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 语音服务工厂类
 *
 * @author shenjy
 * @time 2023/8/30 9:23
 */
@Component
public class SpeechServiceFactory {

    @Value("${speech.service}")
    private String speechService;

    @Resource
    private final Map<String, SpeechService> map = new ConcurrentHashMap<>();

    public SpeechService getService() {
        if (!map.containsKey(speechService)) {
            throw new RuntimeException("找不到对应的服务：" + speechService);
        }
        return map.get(speechService);
    }

    public SpeechService getService(String service) {
        if (!map.containsKey(service)) {
            throw new RuntimeException("找不到对应的服务：" + service);
        }
        return map.get(service);
    }
}
