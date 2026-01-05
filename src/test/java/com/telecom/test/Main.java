package com.telecom.test;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xuehui_li
 * @Version 1.0
 * @date 2026/1/4 10:36
 * @Content
 */

@Slf4j
public class Main {
    public static void main(String[] args) throws Exception {
        Thread.sleep(10_000);
        while (true) {
            test();
            Thread.sleep(1000);
        }
    }

    static void test() {
        log.info("hello");
    }
}
