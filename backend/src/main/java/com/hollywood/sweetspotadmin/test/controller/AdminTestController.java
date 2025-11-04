package com.hollywood.sweetspotadmin.test.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminTestController {

    // 클래스 레벨에 frames 정의
    private static final String[] FRAMES = {
        "🏃💨              ",
        "  🏃💨            ",
        "    🏃💨          ",
        "      🏃💨        ",
        "        🏃💨      ",
        "          🏃💨    ",
        "            🏃💨  ",
        "              🏃💨",
    };

    @GetMapping("/test")
    public String test() {
        // 원한다면 콘솔에도 출력
        printRunningAnimation();

        // HTTP 응답에 frames를 포함
        StringBuilder sb = new StringBuilder();
        for (String frame : FRAMES) {
            sb.append(frame).append("\n");
        }
        return sb.toString();
    }

    private void printRunningAnimation() {
        System.out.println("\n--- Debug Animation: Admin API Running ---");
        try {
            for (int i = 0; i < 2; i++) {
                for (String frame : FRAMES) {
                    System.out.print("\r" + frame);
                    Thread.sleep(100);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        System.out.println("\r🏁  Admin API ready!\n");
    }
}