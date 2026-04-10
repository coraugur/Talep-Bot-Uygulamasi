package com.atmosware.talepbot.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Tester (QA - Quality Assurance) Subagent
 * Kodu test eder ve onay/ret raporu üretir.
 */
public interface TesterAgent {

    @SystemMessage(fromResource = "prompts/tester-system.txt")
    @UserMessage("""
            Aşağıdaki kodu test et. Kabul kriterlerini ve teknik standartları kontrol et.
            
            Developer'ın ürettiği kod:
            {{code}}
            
            Kabul Kriterleri (User Story'den):
            {{acceptanceCriteria}}
            
            Teknik Spesifikasyon:
            {{techSpec}}
            """)
    String test(@V("code") String code, @V("acceptanceCriteria") String acceptanceCriteria, @V("techSpec") String techSpec);
}
