package com.atmosware.talepbot.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Developer (Software Engineer) Subagent
 * Teknik spesifikasyonu çalışan koda dönüştürür.
 */
public interface DeveloperAgent {

    @SystemMessage(fromResource = "prompts/developer-system.txt")
    @UserMessage("""
            Aşağıdaki teknik spesifikasyona göre geliştirme yap.
            
            Teknik Spesifikasyon:
            {{techSpec}}
            
            {{previousTestFeedback}}
            
            {{repoContext}}
            """)
    String develop(@V("techSpec") String techSpec, @V("previousTestFeedback") String previousTestFeedback, @V("repoContext") String repoContext);
}
