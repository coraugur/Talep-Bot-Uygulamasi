package com.atmosware.talepbot.config;

import com.atmosware.talepbot.agent.*;
import com.atmosware.talepbot.tool.DeploySimulatorTool;
import com.atmosware.talepbot.tool.GitHubRepoTool;
import com.atmosware.talepbot.tool.JiraMockTool;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class LangChainConfig {

    private static final Logger log = LoggerFactory.getLogger(LangChainConfig.class);

    /**
     * Mock LLM — "demo" key veya geçersiz key olduğunda devreye girer.
     * @Primary ile Anthropic auto-config bean'ini override eder.
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "langchain4j.anthropic.chat-model.api-key", havingValue = "demo")
    public ChatLanguageModel mockChatLanguageModel() {
        log.warn("============================================");
        log.warn("  MOCK MOD AKTİF — Gerçek LLM kullanılmıyor");
        log.warn("  Gerçek mod için: ANTHROPIC_API_KEY env var ayarlayın");
        log.warn("============================================");
        return new MockChatLanguageModel();
    }

    @Bean
    public ProductOwnerAgent productOwnerAgent(ChatLanguageModel chatModel, JiraMockTool jiraMockTool) {
        return AiServices.builder(ProductOwnerAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(jiraMockTool)
                .build();
    }

    @Bean
    public AnalystAgent analystAgent(ChatLanguageModel chatModel, GitHubRepoTool gitHubRepoTool) {
        return AiServices.builder(AnalystAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(gitHubRepoTool)
                .build();
    }

    @Bean
    public DeveloperAgent developerAgent(ChatLanguageModel chatModel, GitHubRepoTool gitHubRepoTool) {
        return AiServices.builder(DeveloperAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(30))
                .tools(gitHubRepoTool)
                .build();
    }

    @Bean
    public TesterAgent testerAgent(ChatLanguageModel chatModel) {
        return AiServices.builder(TesterAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(30))
                .build();
    }

    @Bean
    public DeploymentAgent deploymentAgent(ChatLanguageModel chatModel, DeploySimulatorTool deployTool) {
        return AiServices.builder(DeploymentAgent.class)
                .chatLanguageModel(chatModel)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(20))
                .tools(deployTool)
                .build();
    }
}
