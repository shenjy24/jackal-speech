package com.jonas.speech.controller;

import com.jonas.speech.service.SseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * SseController
 *
 * @author shenjy
 * @time 2023/11/14 16:51
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/sse")
public class SseController {

    private final SseService sseService;

    /**
     * 创建SSE长连接
     * 返回SseEmitter，会在服务端和客户端创建长连接
     *
     * @param clientId 客户端ID
     * @return SseEmitter
     */
    @GetMapping(path = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(Long clientId) {
        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        // Add the emitter to a list of subscribers or handle it in another way
        sseService.addEmitter(clientId, emitter);
        return emitter;
    }

    /**
     * 向指定客户端推送内容
     *
     * @param clientId 客户端ID
     * @param content  推送内容
     */
    @GetMapping("/push")
    public void push(Long clientId, String content) {
        sseService.push(clientId, content);
    }

    /**
     * 关闭SSE长连接
     *
     * @param clientId 客户端ID
     */
    @GetMapping("/over")
    public void over(Long clientId) {
        sseService.over(clientId);
    }
}
