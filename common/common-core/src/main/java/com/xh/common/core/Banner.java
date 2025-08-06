package com.xh.common.core;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * @author sunxh
 * @since 2025/5/28
 */
public class Banner {

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        // 在这里调用你的方法
        System.out.println("""
                 __  ___   _                  _       _           _
                 \\ \\/ / | | | __ _ _ __      / \\   __| |_ __ ___ (_)_ __
                  \\  /| |_| |/ _` | '_ \\    / _ \\ / _` | '_ ` _ \\| | '_ \\
                  /  \\|  _  | (_| | | | |  / ___ \\ (_| | | | | | | | | | |
                 /_/\\_\\_| |_|\\__,_|_| |_| /_/   \\_\\__,_|_| |_| |_|_|_| |_|
                https://www.xhansky.cn
                """);
    }
}
