package com.atmosware.talepbot.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Analist (Systems / Business Analyst) Subagent
 * User Story'yi teknik spesifikasyona dönüştürür.
 */
public interface AnalystAgent {

    @SystemMessage(fromResource = "prompts/analyst-system.txt")
    @UserMessage("""
            Aşağıdaki User Story için teknik spesifikasyon hazırla.
            
            User Story:
            {{userStory}}
            
            {{repoContext}}
            """)
    String analyze(@V("userStory") String userStory, @V("repoContext") String repoContext);
}
