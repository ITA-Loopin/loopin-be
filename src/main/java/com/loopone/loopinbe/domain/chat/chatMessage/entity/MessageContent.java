package com.loopone.loopinbe.domain.chat.chatMessage.entity;

import com.loopone.loopinbe.domain.loop.ai.dto.res.RecommendationsLoop;
import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "message_content")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class MessageContent {
    @Id // MongoDB에서는 @Id 필드가 String 또는 ObjectId 타입이어야 자동으로 생성
    private String id;

    @Column(length = 1000)
    @Size(max = 1000)
    private String content;

    private RecommendationsLoop recommendations;
}
