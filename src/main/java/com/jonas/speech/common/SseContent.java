package com.jonas.speech.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Timestamp;

/**
 * SseContent
 *
 * @author shenjy
 * @time 2023/11/14 19:06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SseContent {
    private Object message;
    private Timestamp timestamp;
}
