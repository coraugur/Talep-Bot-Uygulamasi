package com.atmosware.talepbot.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * PO (Product Owner) Subagent
 * İş biriminden gelen serbest metin talepleri User Story formatına dönüştürür.
 */
public interface ProductOwnerAgent {

    @SystemMessage(fromResource = "prompts/po-system.txt")
    @UserMessage("""
            İş biriminden aşağıdaki talep geldi. Bu talebi analiz et ve User Story formatına dönüştür.
            
            Talep:
            {{request}}
            """)
    String processRequest(@V("request") String request);
}
