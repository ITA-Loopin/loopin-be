package com.loopone.loopinbe;

import com.loopone.loopinbe.global.s3.S3Service;
import com.loopone.loopinbe.support.TestContainersConfig;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestContainersConfig.class)
class LoopinBeApplicationTests {
	@MockitoBean S3Service s3Service;
	@MockitoBean(name = "geminiChatModel") ChatModel geminiChatModel;
	@MockitoBean(name = "openAiChatModel") ChatModel gptChatModel;

	@Test
	void contextLoads() {}
}
