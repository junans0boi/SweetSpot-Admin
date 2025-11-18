// SweetspotAdminApplication.java
package com.hollywood.sweetspotadmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync      // ✅ [추가]
@EnableScheduling // ✅ [추가]
@SpringBootApplication
public class SweetspotAdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(SweetspotAdminApplication.class, args);
    }
}