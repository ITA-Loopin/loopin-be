package com.loopone.loopinbe.global;

import com.loopone.loopinbe.global.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("api/v1/health-check")
    public ApiResponse<Void> healthCheck(){
        return ApiResponse.success();
    }
}