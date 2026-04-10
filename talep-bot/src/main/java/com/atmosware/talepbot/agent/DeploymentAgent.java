package com.atmosware.talepbot.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Deployment (DevOps / Operations) Subagent
 * Onaylı kodu canlı ortama deploy eder (simülasyon).
 */
public interface DeploymentAgent {

    @SystemMessage(fromResource = "prompts/deployment-system.txt")
    @UserMessage("""
            Aşağıdaki kodu canlı ortama deploy et.
            
            Deploy edilecek kod:
            {{code}}
            
            Test Raporu:
            {{testReport}}
            
            Uygulama bilgileri:
            - Artifact: talep-bot-service
            - Ortam: production
            """)
    String deploy(@V("code") String code, @V("testReport") String testReport);
}
