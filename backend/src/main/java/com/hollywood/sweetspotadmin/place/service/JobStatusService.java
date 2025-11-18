package com.hollywood.sweetspotadmin.place.service;

import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class JobStatusService {
    // 동시성 문제를 해결하기 위해 ConcurrentHashMap 사용
    private final Map<String, String> jobStatuses = new ConcurrentHashMap<>();
    private volatile String lastJobId;

    // 작업 상태 업데이트
    public void updateStatus(String jobId, String status) {
        jobStatuses.put(jobId, status);
        this.lastJobId = jobId;
    }

    // 작업 상태 조회
    public String getStatus(String jobId) {
        return jobStatuses.getOrDefault(jobId, "작업 상태를 찾을 수 없습니다.");
    }

    // 작업 완료 후 맵에서 제거
    public void clearStatus(String jobId) {
        jobStatuses.remove(jobId);
    }

    public boolean isJobRunning() {
        if (lastJobId == null) {
            return false;
        }
        String status = getStatus(lastJobId);
        return status != null && !status.startsWith("COMPLETED") && !status.startsWith("FAILED");
    }
}