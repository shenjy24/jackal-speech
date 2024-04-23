package com.jonas.speech.service;

/**
 * SpeechToTextCallback
 *
 * @author shenjy
 * @time 2024/4/23 17:36
 */
public interface SpeechToTextCallback {

    void onComplete(String text);
}
