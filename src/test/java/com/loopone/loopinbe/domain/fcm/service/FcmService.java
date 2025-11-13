package com.loopone.loopinbe.domain.fcm.service;

import com.loopone.loopinbe.domain.fcm.dto.res.FcmMessageResponse;

public interface FcmService {
    void sendMessageTo(FcmMessageResponse message);
}
