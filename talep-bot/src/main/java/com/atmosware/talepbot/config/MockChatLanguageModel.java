package com.atmosware.talepbot.config;

import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Mock ChatLanguageModel — gerçek LLM API'si olmadan pipeline'ı test etmek için
 * her agent rolüne uygun sahte JSON yanıtları döner.
 */
public class MockChatLanguageModel implements ChatLanguageModel {

    private static final Logger log = LoggerFactory.getLogger(MockChatLanguageModel.class);

    @Override
    public ChatResponse chat(ChatRequest chatRequest) {
        List<ChatMessage> messages = chatRequest.messages();
        String lastUserMsg = messages.stream()
                .filter(m -> m instanceof UserMessage)
                .map(m -> ((UserMessage) m).singleText())
                .reduce((a, b) -> b)
                .orElse("");

        String response = generateMockResponse(lastUserMsg);
        log.info("[MockLLM] Yanıt üretildi ({} karakter)", response.length());

        return ChatResponse.builder()
                .aiMessage(AiMessage.from(response))
                .build();
    }

    private String generateMockResponse(String userMessage) {
        String lower = userMessage.toLowerCase();

        // Tester Agent — Test Report (check BEFORE analyst/developer since message contains teknik spesifikasyon too)
        if (lower.contains("test et") || lower.contains("kabul kriterleri")) {
            return generateTesterResponse();
        }

        // Developer Agent — Code Output (check before analyst)
        if (lower.contains("geliştirme yap") || lower.contains("gelistirme yap")) {
            return generateDeveloperResponse();
        }

        // Deployment Agent — Deploy Report
        if (lower.contains("deploy") || lower.contains("canlı ortam") || lower.contains("canli ortam")) {
            return generateDeploymentResponse();
        }

        // PO Agent — User Story
        if (lower.contains("user story") && lower.contains("talep")) {
            return generatePoResponse(userMessage);
        }

        // Analyst Agent — Technical Spec (last resort for teknik spesifikasyon)
        if (lower.contains("teknik spesifikasyon") || lower.contains("teknik analiz")) {
            return generateAnalystResponse();
        }

        // Fallback — generic
        return """
                {
                  "message": "Mock yanıt üretildi.",
                  "details": "Bu bir test yanıtıdır. Gerçek LLM API bağlantısı için ANTHROPIC_API_KEY ayarlayın."
                }
                """;
    }

    private String generatePoResponse(String userMessage) {
        return """
                {
                  "title": "İcra Dosyası Sorgulama ve Borç Ödeme Modülü",
                  "epicContext": "İcra Takip Sistemi — Vatandaş Self-Servis Portalı epik'ine ait. Vatandaşların icra dosyalarını online sorgulaması ve ödeme yapabilmesi.",
                  "description": "Vatandaşlar, üzerlerine açılmış icra dosyalarını TC kimlik numarası veya dosya numarası ile sorgulayabilmeli, dosya detaylarını görüntüleyebilmeli ve online ödeme yapabilmelidir. Sistem UYAP entegrasyonlu çalışmalıdır.",
                  "asA": "Bir borçlu vatandaş olarak",
                  "iWant": "Üzerimdeki icra dosyalarını online sorgulayabilmek ve borcumu ödeyebilmek",
                  "soThat": "İcra dairesine fiziksel olarak gitmeden borcumu öğrenip ödeme yapabileyim ve %10 indirimden faydalanabileyim",
                  "acceptanceCriteria": [
                    "GIVEN vatandaş TC kimlik numarası ile sisteme giriş yaptığında WHEN icra dosya sorgulama ekranını açtığında THEN üzerindeki tüm aktif icra dosyaları listelenmeli",
                    "GIVEN vatandaş bir icra dosyası seçtiğinde WHEN dosya detay sayfasını görüntülediğinde THEN alacaklı bilgisi, ana borç, faiz, masraf ve toplam borç tutarı gösterilmeli",
                    "GIVEN vatandaş ödeme butonuna tıkladığında WHEN geçerli bir kredi kartı bilgisi girdiğinde THEN ödeme işlemi başarıyla tamamlanmalı ve makbuz oluşturulmalı",
                    "GIVEN vatandaş borcun tamamını tek seferde ödediğinde WHEN ödeme onaylandığında THEN %10 peşin ödeme indirimi otomatik uygulanmalı",
                    "GIVEN ödeme başarısız olduğunda WHEN banka reddetti THEN kullanıcıya anlaşılır hata mesajı gösterilmeli ve işlem loglanmalı",
                    "GIVEN dosya durumu 'kapatılmış' olduğu WHEN vatandaş sorgulama yaptığında THEN dosya 'Kapatılmış' etiketiyle gösterilmeli, ödeme butonu pasif olmalı"
                  ],
                  "priority": "HIGH",
                  "priorityJustification": "Vatandaş memnuniyeti ve icra dairesi iş yükünü azaltma açısından kritik. Yıllık ~2M icra dosyası sorgulaması fiziksel olarak yapılıyor.",
                  "businessValue": {
                    "impact": "HIGH",
                    "reach": "Yıllık ~2 milyon aktif icra dosyası borçlusu",
                    "confidence": "HIGH",
                    "effort": "L",
                    "riceScore": "Reach(8) × Impact(3) × Confidence(0.9) / Effort(5) = 4.32"
                  },
                  "userPersonas": [
                    {
                      "persona": "Borçlu Vatandaş",
                      "needsAndPains": "İcra dairesine gitmek zaman kaybı, kuyruk bekleme süresi uzun, mesai saatleri dışında işlem yapamıyor"
                    },
                    {
                      "persona": "İcra Memuru",
                      "needsAndPains": "Fiziksel sorgu talepleri iş yükünü artırıyor, dijitalleşme ile memur verimliliği artacak"
                    }
                  ],
                  "businessRules": [
                    {
                      "rule": "Peşin ödeme indirimi: Borcun tamamı tek seferde ödendiğinde %10 indirim uygulanır",
                      "source": "2004 sayılı İcra ve İflas Kanunu - Madde 82 ilgili yönetmelik",
                      "exceptions": "Nafaka alacaklarında indirim uygulanmaz"
                    },
                    {
                      "rule": "Taksitlendirme: Borçlu en fazla 12 taksit talep edebilir, ilk taksit %25 olmalıdır",
                      "source": "İcra ve İflas Kanunu Madde 111",
                      "exceptions": "Alacaklının onayı gereklidir"
                    },
                    {
                      "rule": "Faiz hesaplaması: Yasal faiz oranı TÜİK tarafından yıllık belirlenen orana göre günlük hesaplanır",
                      "source": "3095 sayılı Kanuni Faiz ve Temerrüt Faizi Kanunu",
                      "exceptions": "Sözleşmede farklı faiz oranı belirlenmiş olabilir"
                    }
                  ],
                  "scope": {
                    "inScope": ["TC kimlik ile dosya sorgulama", "Dosya detay görüntüleme", "Kredi kartı ile online ödeme", "Ödeme makbuzu oluşturma", "Peşin ödeme indirimi"],
                    "outOfScope": ["Taksitlendirme modülü (Faz 2)", "Avukat portalı", "UYAP direkt entegrasyonu (mock API kullanılacak)", "Haciz işlemleri yönetimi"],
                    "futureConsiderations": ["Taksit ödeme planı oluşturma", "SMS/e-posta bildirim sistemi", "Avukat yetkilendirme modülü"]
                  },
                  "dependencies": [
                    {
                      "type": "DEPENDS_ON",
                      "description": "UYAP Web Servis API — icra dosya bilgilerinin çekilmesi için gerekli"
                    },
                    {
                      "type": "DEPENDS_ON",
                      "description": "Sanal POS entegrasyonu — online ödeme altyapısı için banka POS API gerekli"
                    },
                    {
                      "type": "RELATED",
                      "description": "e-Devlet Kimlik Doğrulama — vatandaş giriş için e-Devlet OAuth entegrasyonu"
                    }
                  ],
                  "risks": [
                    {
                      "risk": "UYAP API'sinin yüksek response time'ı (>5sn) kullanıcı deneyimini bozabilir",
                      "probability": "HIGH",
                      "impact": "MEDIUM",
                      "mitigation": "Cache layer + async sorgulama + loading skeleton UI"
                    },
                    {
                      "risk": "Ödeme sırasında bağlantı kopması — çift ödeme riski",
                      "probability": "MEDIUM",
                      "impact": "HIGH",
                      "mitigation": "Idempotency key + ödeme durumu kontrol mekanizması"
                    }
                  ],
                  "uxConsiderations": [
                    "Mobil öncelikli tasarım — vatandaşların %70'i mobil cihazdan erişecek",
                    "WCAG 2.1 AA uyumlu — yaşlı vatandaşlar için büyük font ve yüksek kontrast",
                    "Borç tutarı kırmızı/yeşil renk kodlaması ile gösterilmeli"
                  ],
                  "nonFunctionalRequirements": [
                    "Sorgulama response time < 3 saniye (p95)",
                    "Ödeme işlemi PCI-DSS uyumlu olmalı",
                    "Sistem 7/24 erişilebilir olmalı, %99.9 uptime"
                  ],
                  "successMetrics": [
                    {
                      "metric": "Online sorgu oranı",
                      "currentBaseline": "%0 (mevcut sistem yok)",
                      "target": "İlk 6 ayda toplam sorguların %30'u online yapılmalı"
                    },
                    {
                      "metric": "Online ödeme oranı",
                      "currentBaseline": "%0",
                      "target": "İlk 6 ayda toplam ödemelerin %15'i online yapılmalı"
                    }
                  ],
                  "openQuestions": [
                    {
                      "question": "UYAP API erişim yetkisi ve SLA anlaşması mevcut mu?",
                      "suggestedDefault": "Mock API ile geliştirme başlansın, UYAP entegrasyonu paralel ilerlesin",
                      "impact": "Entegrasyon testleri gecikebilir"
                    },
                    {
                      "question": "Hangi bankalar ile sanal POS anlaşması yapılacak?",
                      "suggestedDefault": "İş Bankası ve Garanti BBVA ile başlansın",
                      "impact": "Ödeme modülü geliştirme kapsamını etkiler"
                    }
                  ]
                }
                """;
    }

    private String generateAnalystResponse() {
        return """
                {
                  "summary": "İcra dosyası sorgulama ve online ödeme modülü için teknik spesifikasyon. Vatandaş TC kimlik ile giriş yaparak dosyalarını sorgulayıp kredi kartı ile ödeme yapabilecek. UYAP mock API entegrasyonu, sanal POS entegrasyonu ve faiz hesaplama motoru ana bileşenlerdir.",
                  "assumptions": [
                    "Varsayım 1: UYAP API'si REST tabanlı olacak ve dosya bilgilerini JSON formatında dönecek",
                    "Varsayım 2: PCI-DSS uyumluluk için ödeme bilgileri backend'de saklanmayacak, tokenization kullanılacak",
                    "Varsayım 3: Faiz hesaplaması günlük bazda yapılacak, TCMB yasal faiz oranı kullanılacak"
                  ],
                  "apiEndpoints": [
                    {
                      "method": "GET",
                      "path": "/api/v1/icra-dosya/sorgula",
                      "description": "Vatandaşın TC kimlik numarasına göre aktif icra dosyalarını listeler. UYAP API ile entegre çalışır.",
                      "requestBody": "Query param: tcKimlikNo (11 haneli)",
                      "responseBody": "{ dosyalar: [{ dosyaNo: string, alacakli: string, anaBorcTutari: BigDecimal, toplamBorc: BigDecimal, durum: string, acilisTarihi: LocalDate }] }",
                      "headers": "Authorization: Bearer {jwt-token}",
                      "statusCodes": "200: Başarılı, 400: Geçersiz TC, 401: Yetkisiz, 404: Dosya bulunamadı, 429: Rate limit, 503: UYAP erişilemez",
                      "rateLimiting": "5 req/dk/kullanıcı",
                      "cachingStrategy": "Redis — TTL 5 dakika, kullanıcı bazlı cache key"
                    },
                    {
                      "method": "GET",
                      "path": "/api/v1/icra-dosya/{dosyaNo}/detay",
                      "description": "Belirli bir icra dosyasının detay bilgilerini getirir — alacaklı, borçlu, ana borç, faiz, masraf, toplam",
                      "responseBody": "{ dosyaNo: string, alacakli: { ad: string, vkn: string }, borclu: { ad: string, tcKimlik: string }, anaBorcTutari: BigDecimal, islemiseFaiz: BigDecimal, masraflar: BigDecimal, toplamBorc: BigDecimal, pesinIndirimliTutar: BigDecimal, durum: string }",
                      "headers": "Authorization: Bearer {jwt-token}",
                      "statusCodes": "200: Başarılı, 404: Dosya bulunamadı, 403: Yetkisiz erişim (başka kişinin dosyası)",
                      "cachingStrategy": "Redis — TTL 2 dakika"
                    },
                    {
                      "method": "POST",
                      "path": "/api/v1/icra-dosya/{dosyaNo}/odeme",
                      "description": "İcra dosyası için online ödeme başlatır. Sanal POS entegrasyonu ile kredi kartı ödemesi alır.",
                      "requestBody": "{ kartNumarasi: string (masked), sonKullanma: string, cvv: string, odemeTutari: BigDecimal, idempotencyKey: UUID }",
                      "responseBody": "{ odemeId: UUID, durum: BASARILI|BASARISIZ, makbuzNo: string, indirimliBorcTutari: BigDecimal, odenenTutar: BigDecimal, islemTarihi: LocalDateTime }",
                      "headers": "Authorization: Bearer {jwt-token}, X-Idempotency-Key: UUID",
                      "statusCodes": "200: Ödeme başarılı, 400: Geçersiz kart bilgisi, 402: Ödeme reddedildi, 409: Tekrarlı ödeme (idempotency), 503: POS erişilemez"
                    }
                  ],
                  "dataModels": [
                    {
                      "tableName": "icra_dosya",
                      "description": "UYAP'tan çekilen icra dosya bilgilerinin cache tablosu",
                      "fields": [
                        "id UUID PRIMARY KEY DEFAULT gen_random_uuid()",
                        "dosya_no VARCHAR(20) NOT NULL UNIQUE -- İcra dosya numarası (örn: 2024/12345)",
                        "borclu_tc VARCHAR(11) NOT NULL -- Borçlu TC kimlik numarası",
                        "borclu_ad VARCHAR(200) NOT NULL -- Borçlu ad soyad",
                        "alacakli_ad VARCHAR(200) NOT NULL -- Alacaklı ad/unvan",
                        "ana_borc_tutari DECIMAL(15,2) NOT NULL -- Ana borç tutarı (TL)",
                        "faiz_tutari DECIMAL(15,2) NOT NULL DEFAULT 0 -- İşlemiş faiz",
                        "masraf_tutari DECIMAL(15,2) NOT NULL DEFAULT 0 -- İcra masrafları",
                        "toplam_borc DECIMAL(15,2) NOT NULL -- Toplam borç (ana+faiz+masraf)",
                        "durum VARCHAR(20) NOT NULL DEFAULT 'AKTIF' -- AKTIF, KAPALI, TAKSITLI",
                        "acilis_tarihi DATE NOT NULL",
                        "son_guncelleme TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP",
                        "uyap_sync_tarihi TIMESTAMP -- UYAP ile son senkronizasyon tarihi"
                      ],
                      "indexes": [
                        "CREATE INDEX idx_icra_dosya_borclu_tc ON icra_dosya(borclu_tc) -- TC ile dosya sorgulama",
                        "CREATE INDEX idx_icra_dosya_durum ON icra_dosya(durum) -- Aktif dosya filtreleme"
                      ],
                      "constraints": [
                        "CHECK (durum IN ('AKTIF', 'KAPALI', 'TAKSITLI'))",
                        "CHECK (ana_borc_tutari >= 0)",
                        "CHECK (toplam_borc >= ana_borc_tutari)"
                      ],
                      "estimatedRowCount": "1 yılda ~2M satır",
                      "partitioningStrategy": "acilis_tarihi üzerinden yıllık range partitioning önerilir"
                    },
                    {
                      "tableName": "odeme_islem",
                      "description": "Online ödeme işlem kayıtları — audit trail ve idempotency kontrolü",
                      "fields": [
                        "id UUID PRIMARY KEY DEFAULT gen_random_uuid()",
                        "dosya_no VARCHAR(20) NOT NULL REFERENCES icra_dosya(dosya_no)",
                        "borclu_tc VARCHAR(11) NOT NULL",
                        "odeme_tutari DECIMAL(15,2) NOT NULL",
                        "indirim_tutari DECIMAL(15,2) DEFAULT 0 -- Peşin ödeme indirimi",
                        "net_odenen DECIMAL(15,2) NOT NULL",
                        "durum VARCHAR(20) NOT NULL -- BEKLEMEDE, BASARILI, BASARISIZ, IPTAL",
                        "makbuz_no VARCHAR(30) UNIQUE",
                        "idempotency_key UUID NOT NULL UNIQUE -- Çift ödeme koruması",
                        "banka_referans_no VARCHAR(50)",
                        "hata_mesaji TEXT -- Başarısız ise banka hata mesajı",
                        "islem_tarihi TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP"
                      ],
                      "indexes": [
                        "CREATE UNIQUE INDEX idx_odeme_idempotency ON odeme_islem(idempotency_key)",
                        "CREATE INDEX idx_odeme_dosya ON odeme_islem(dosya_no)",
                        "CREATE INDEX idx_odeme_tarih ON odeme_islem(islem_tarihi)"
                      ],
                      "constraints": [
                        "CHECK (durum IN ('BEKLEMEDE', 'BASARILI', 'BASARISIZ', 'IPTAL'))",
                        "CHECK (odeme_tutari > 0)"
                      ],
                      "estimatedRowCount": "1 yılda ~300K satır"
                    }
                  ],
                  "businessRules": [
                    {
                      "rule": "Peşin ödeme indirimi: Borcun tamamı tek seferde ödendiğinde ana borca %10 indirim uygulanır",
                      "implementation": "Application logic — OdemeService.hesaplaIndirim() metodu, sadece odemeTutari >= toplamBorc ise aktif",
                      "validationMessage": "Peşin ödeme indirimi uygulandı: {indirimTutari} TL"
                    },
                    {
                      "rule": "TC kimlik doğrulama: Vatandaş sadece kendi dosyalarını sorgulayabilir",
                      "implementation": "JWT token içindeki TC ile sorgulanan TC eşleşmeli, Authorization filter'da kontrol",
                      "validationMessage": "Bu icra dosyasına erişim yetkiniz bulunmamaktadır."
                    }
                  ],
                  "constraints": [
                    "Teknik kısıtlama: UYAP API max 5 req/sn — connection pooling ve rate limiter gerekli",
                    "Teknik kısıtlama: PCI-DSS uyumluluk — kart bilgileri backend'de saklanmamalı, POS API'ye direkt iletilmeli"
                  ],
                  "edgeCases": [
                    {
                      "scenario": "Ödeme sırasında UYAP'ta dosya kapanmış olabilir (başka kanaldan ödeme)",
                      "impact": "Mükerrer ödeme — vatandaş mağduriyeti",
                      "mitigation": "Ödeme öncesi dosya durumu tekrar kontrol edilmeli (double-check pattern)"
                    },
                    {
                      "scenario": "Faiz hesaplaması gün sonunda değişiyor — gece 00:00'da sorgulayan kişi farklı tutar görebilir",
                      "impact": "Tutarsızlık, güven kaybı",
                      "mitigation": "Ödeme ekranında 'Bu tutar {tarih} {saat} itibarıyla geçerlidir' uyarısı göster"
                    }
                  ],
                  "securityConsiderations": [
                    {
                      "threat": "IDOR — Başka vatandaşın dosyasına erişim (A01:2021 Broken Access Control)",
                      "severity": "HIGH",
                      "mitigation": "JWT token içindeki TC ile dosya sahibi TC eşleşmesi zorunlu, her endpoint'te authorization check"
                    },
                    {
                      "threat": "SQL Injection — TC kimlik parametresinde (A03:2021 Injection)",
                      "severity": "HIGH",
                      "mitigation": "Parameterized query, TC formatı regex validasyonu: ^[1-9][0-9]{10}$"
                    }
                  ],
                  "impactAnalysis": "Yeni modül — mevcut sisteme doğrudan etkisi yok. UYAP ve POS entegrasyonları yeni bağımlılıklar ekler."
                }
                """;
    }

    private String generateDeveloperResponse() {
        return """
                {
                  "summary": "İcra dosyası sorgulama ve online ödeme modülü geliştirildi. 3 katmanlı mimari: Controller → Service → Repository. UYAP mock client, faiz hesaplama motoru ve sanal POS entegrasyonu kodlandı. Idempotency key ile çift ödeme koruması, IDOR koruması ve input validasyonu uygulandı.",
                  "architectureDecisions": [
                    {
                      "decision": "Hexagonal Architecture — Port & Adapter pattern ile UYAP ve POS entegrasyonu",
                      "rationale": "Dış sistemlere olan bağımlılığı soyutlayarak test edilebilirliği ve değiştirilebilirliği artırmak",
                      "tradeoffs": "Daha fazla interface ve adapter sınıfı, ancak mock test ve gelecekte farklı POS/UYAP implementasyonuna geçiş kolaylığı"
                    },
                    {
                      "decision": "Idempotency Key pattern ile ödeme güvenliği",
                      "rationale": "Çift ödeme riskini önlemek — ağ kesintisi veya kullanıcı çift tıklama durumlarında",
                      "tradeoffs": "Her ödeme isteğinde UUID üretme ve DB unique constraint maliyeti, ancak finansal güvenlik açısından zorunlu"
                    }
                  ],
                  "files": [
                    {
                      "filePath": "src/main/java/com/atmosware/icratakip/controller/IcraDosyaController.java",
                      "language": "java",
                      "content": "package com.atmosware.icratakip.controller;\\n\\nimport com.atmosware.icratakip.dto.DosyaDetayResponse;\\nimport com.atmosware.icratakip.dto.DosyaListeResponse;\\nimport com.atmosware.icratakip.dto.OdemeRequest;\\nimport com.atmosware.icratakip.dto.OdemeResponse;\\nimport com.atmosware.icratakip.service.IcraDosyaService;\\nimport com.atmosware.icratakip.service.OdemeService;\\nimport jakarta.validation.Valid;\\nimport jakarta.validation.constraints.Pattern;\\nimport lombok.RequiredArgsConstructor;\\nimport lombok.extern.slf4j.Slf4j;\\nimport org.springframework.http.ResponseEntity;\\nimport org.springframework.web.bind.annotation.*;\\n\\nimport java.util.UUID;\\n\\n@Slf4j\\n@RestController\\n@RequestMapping(\\"/api/v1/icra-dosya\\")\\n@RequiredArgsConstructor\\npublic class IcraDosyaController {\\n\\n    private final IcraDosyaService dosyaService;\\n    private final OdemeService odemeService;\\n\\n    @GetMapping(\\"/sorgula\\")\\n    public ResponseEntity<DosyaListeResponse> sorgula(\\n            @RequestParam @Pattern(regexp = \\"^[1-9][0-9]{10}$\\") String tcKimlikNo) {\\n        log.info(\\"İcra dosya sorgusu: tc={}***\\", tcKimlikNo.substring(0, 3));\\n        return ResponseEntity.ok(dosyaService.sorgula(tcKimlikNo));\\n    }\\n\\n    @GetMapping(\\"/{dosyaNo}/detay\\")\\n    public ResponseEntity<DosyaDetayResponse> detay(@PathVariable String dosyaNo) {\\n        log.info(\\"İcra dosya detay: dosyaNo={}\\", dosyaNo);\\n        return ResponseEntity.ok(dosyaService.detay(dosyaNo));\\n    }\\n\\n    @PostMapping(\\"/{dosyaNo}/odeme\\")\\n    public ResponseEntity<OdemeResponse> odemeYap(\\n            @PathVariable String dosyaNo,\\n            @Valid @RequestBody OdemeRequest request,\\n            @RequestHeader(\\"X-Idempotency-Key\\") UUID idempotencyKey) {\\n        log.info(\\"Ödeme başlatıldı: dosyaNo={}, idempotencyKey={}\\", dosyaNo, idempotencyKey);\\n        return ResponseEntity.ok(odemeService.odemeYap(dosyaNo, request, idempotencyKey));\\n    }\\n}",
                      "description": "İcra dosya sorgulama, detay görüntüleme ve ödeme endpoint'leri. TC kimlik regex validasyonu, idempotency key zorunluluğu ve hassas veri maskeleme loglaması uygulandı.",
                      "layer": "controller",
                      "designPatterns": ["REST Controller", "Input Validation", "Structured Logging"]
                    },
                    {
                      "filePath": "src/main/java/com/atmosware/icratakip/service/IcraDosyaService.java",
                      "language": "java",
                      "content": "package com.atmosware.icratakip.service;\\n\\nimport com.atmosware.icratakip.client.UyapClient;\\nimport com.atmosware.icratakip.dto.DosyaDetayResponse;\\nimport com.atmosware.icratakip.dto.DosyaListeResponse;\\nimport com.atmosware.icratakip.exception.DosyaBulunamadiException;\\nimport lombok.RequiredArgsConstructor;\\nimport lombok.extern.slf4j.Slf4j;\\nimport org.springframework.cache.annotation.Cacheable;\\nimport org.springframework.stereotype.Service;\\n\\n@Slf4j\\n@Service\\n@RequiredArgsConstructor\\npublic class IcraDosyaService {\\n\\n    private final UyapClient uyapClient;\\n\\n    @Cacheable(value = \\"icra-dosya-listesi\\", key = \\"#tcKimlikNo\\", unless = \\"#result.dosyalar.isEmpty()\\")\\n    public DosyaListeResponse sorgula(String tcKimlikNo) {\\n        log.info(\\"UYAP'tan dosya listesi çekiliyor: tc={}***\\", tcKimlikNo.substring(0, 3));\\n        return uyapClient.dosyaListesiGetir(tcKimlikNo);\\n    }\\n\\n    @Cacheable(value = \\"icra-dosya-detay\\", key = \\"#dosyaNo\\")\\n    public DosyaDetayResponse detay(String dosyaNo) {\\n        DosyaDetayResponse detay = uyapClient.dosyaDetayGetir(dosyaNo);\\n        if (detay == null) throw new DosyaBulunamadiException(dosyaNo);\\n        return detay;\\n    }\\n}",
                      "description": "İcra dosya iş mantığı — UYAP client üzerinden dosya sorgulama, Redis cache entegrasyonu ile performans optimizasyonu.",
                      "layer": "service",
                      "designPatterns": ["Service Layer", "Caching", "Port & Adapter"]
                    },
                    {
                      "filePath": "src/main/java/com/atmosware/icratakip/service/OdemeService.java",
                      "language": "java",
                      "content": "package com.atmosware.icratakip.service;\\n\\nimport com.atmosware.icratakip.dto.OdemeRequest;\\nimport com.atmosware.icratakip.dto.OdemeResponse;\\nimport com.atmosware.icratakip.entity.OdemeIslem;\\nimport com.atmosware.icratakip.exception.MukerrerOdemeException;\\nimport com.atmosware.icratakip.repository.OdemeIslemRepository;\\nimport lombok.RequiredArgsConstructor;\\nimport lombok.extern.slf4j.Slf4j;\\nimport org.springframework.stereotype.Service;\\nimport org.springframework.transaction.annotation.Transactional;\\n\\nimport java.math.BigDecimal;\\nimport java.time.LocalDateTime;\\nimport java.util.UUID;\\n\\n@Slf4j\\n@Service\\n@RequiredArgsConstructor\\npublic class OdemeService {\\n\\n    private static final BigDecimal PESIN_INDIRIM_ORANI = new BigDecimal(\\"0.10\\");\\n    private final OdemeIslemRepository repository;\\n\\n    @Transactional\\n    public OdemeResponse odemeYap(String dosyaNo, OdemeRequest req, UUID idempotencyKey) {\\n        repository.findByIdempotencyKey(idempotencyKey).ifPresent(existing -> {\\n            throw new MukerrerOdemeException(idempotencyKey);\\n        });\\n\\n        BigDecimal indirimTutari = req.tamOdeme() ? req.odemeTutari().multiply(PESIN_INDIRIM_ORANI) : BigDecimal.ZERO;\\n        BigDecimal netOdenen = req.odemeTutari().subtract(indirimTutari);\\n\\n        OdemeIslem islem = new OdemeIslem();\\n        islem.setDosyaNo(dosyaNo);\\n        islem.setOdemeTutari(req.odemeTutari());\\n        islem.setIndirimTutari(indirimTutari);\\n        islem.setNetOdenen(netOdenen);\\n        islem.setDurum(\\"BASARILI\\");\\n        islem.setMakbuzNo(\\"MKB-\\" + System.currentTimeMillis());\\n        islem.setIdempotencyKey(idempotencyKey);\\n        islem.setIslemTarihi(LocalDateTime.now());\\n        repository.save(islem);\\n\\n        log.info(\\"Ödeme başarılı: dosyaNo={}, tutar={}, indirim={}, net={}\\", dosyaNo, req.odemeTutari(), indirimTutari, netOdenen);\\n        return new OdemeResponse(islem.getId(), \\"BASARILI\\", islem.getMakbuzNo(), indirimTutari, netOdenen, islem.getIslemTarihi());\\n    }\\n}",
                      "description": "Ödeme iş mantığı — idempotency kontrolü, peşin ödeme indirimi hesaplama, transaction yönetimi. Kart bilgileri loglanmıyor (PCI-DSS).",
                      "layer": "service",
                      "designPatterns": ["Idempotency Key", "Transaction Script", "Defensive Programming"]
                    }
                  ],
                  "configChanges": [
                    {
                      "file": "application.yml",
                      "change": "Redis cache TTL ayarları ve UYAP client timeout konfigürasyonu eklendi",
                      "reason": "UYAP API yavaş yanıt verebilir, cache ile kullanıcı deneyimi iyileştirildi"
                    }
                  ],
                  "dependencies": [
                    {
                      "groupId": "org.springframework.boot",
                      "artifactId": "spring-boot-starter-data-redis",
                      "version": "3.4.4",
                      "scope": "compile",
                      "reason": "İcra dosya sorgulama sonuçlarının cache'lenmesi için"
                    }
                  ],
                  "securityMeasures": [
                    {
                      "threat": "IDOR — Başka vatandaşın icra dosyasına erişim (OWASP A01)",
                      "implementation": "JWT token içindeki TC ile dosya sahibi TC'nin eşleşmesi controller seviyesinde kontrol ediliyor"
                    },
                    {
                      "threat": "PCI-DSS — Kart bilgisi sızıntısı",
                      "implementation": "Kart bilgileri hiçbir katmanda loglanmıyor, sadece POS API'ye iletilip yanıt alınıyor"
                    }
                  ],
                  "performanceOptimizations": [
                    {
                      "area": "UYAP dosya sorgusu",
                      "technique": "Redis cache — TTL 5 dakika, kullanıcı bazlı key",
                      "expectedImpact": "İlk sorgu sonrası tekrarlı istekler <50ms"
                    }
                  ],
                  "testSuggestions": [
                    {
                      "type": "unit",
                      "description": "OdemeService.hesaplaIndirim() — peşin ödeme %10 indirim doğrulaması",
                      "priority": "HIGH"
                    },
                    {
                      "type": "integration",
                      "description": "Idempotency key testleri — aynı key ile çift ödeme 409 dönmeli",
                      "priority": "HIGH"
                    }
                  ],
                  "bugFixes": []
                }
                """;
    }

    private String generateTesterResponse() {
        return """
                {
                  "status": "PASS",
                  "summary": "İcra Takip modülü kapsamlı test edildi. 15 test senaryosu çalıştırıldı, tamamı başarılı. Fonksiyonel doğruluk, güvenlik kontrolleri ve edge case'ler doğrulandı. Peşin ödeme indirimi hesaplaması, idempotency koruması ve IDOR güvenlik kontrolü başarıyla geçti.",
                  "overallQualityScore": "A",
                  "qualityGateDetails": {
                    "functionalCorrectness": "PASS",
                    "securityCompliance": "PASS",
                    "performanceAcceptable": "PASS",
                    "codeQuality": "PASS",
                    "errorHandling": "PASS"
                  },
                  "testMetrics": {
                    "totalTests": 15,
                    "passedTests": 15,
                    "failedTests": 0,
                    "skippedTests": 0,
                    "coverageEstimate": "%87",
                    "criticalDefects": 0,
                    "majorDefects": 0,
                    "minorDefects": 0
                  },
                  "testCases": [
                    {
                      "id": "TC-001",
                      "name": "testDosyaSorgula_GecerliTC_DosyaListesiDoner",
                      "category": "FUNCTIONAL",
                      "priority": "CRITICAL",
                      "type": "INTEGRATION",
                      "preconditions": "Geçerli JWT token, UYAP mock'ta 2 adet aktif dosya mevcut",
                      "steps": ["GET /api/v1/icra-dosya/sorgula?tcKimlikNo=12345678901", "Response body kontrol edilir"],
                      "expectedResult": "200 OK, 2 adet icra dosyası listede döner",
                      "actualResult": "200 OK, 2 adet icra dosyası başarıyla döndü",
                      "status": "PASS",
                      "failureReason": "",
                      "linkedAcceptanceCriteria": "Vatandaş TC kimlik ile aktif icra dosyalarını listeleyebilmeli"
                    },
                    {
                      "id": "TC-002",
                      "name": "testDosyaSorgula_GecersizTC_400Doner",
                      "category": "EDGE_CASE",
                      "priority": "HIGH",
                      "type": "API",
                      "preconditions": "Geçerli JWT token",
                      "steps": ["GET /api/v1/icra-dosya/sorgula?tcKimlikNo=ABC", "Response status kontrol edilir"],
                      "expectedResult": "400 Bad Request, validasyon hata mesajı",
                      "actualResult": "400 Bad Request, 'TC kimlik numarası 11 haneli rakam olmalıdır' mesajı döndü",
                      "status": "PASS",
                      "failureReason": "",
                      "linkedAcceptanceCriteria": "Geçersiz TC formatında hata mesajı gösterilmeli"
                    },
                    {
                      "id": "TC-003",
                      "name": "testOdeme_PesinIndirim_YuzdeOnUygulanir",
                      "category": "FUNCTIONAL",
                      "priority": "CRITICAL",
                      "type": "UNIT",
                      "preconditions": "Aktif dosya, toplamBorc=10000 TL, tamOdeme=true",
                      "steps": ["OdemeService.odemeYap() çağrılır", "İndirim tutarı kontrol edilir"],
                      "expectedResult": "İndirim tutarı: 1000 TL, net ödenen: 9000 TL",
                      "actualResult": "İndirim tutarı: 1000.00 TL, net ödenen: 9000.00 TL",
                      "status": "PASS",
                      "failureReason": "",
                      "linkedAcceptanceCriteria": "Borcun tamamı tek seferde ödendiğinde %10 indirim uygulanmalı"
                    },
                    {
                      "id": "TC-004",
                      "name": "testOdeme_IdempotencyKey_CiftOdemeEngellenir",
                      "category": "SECURITY",
                      "priority": "CRITICAL",
                      "type": "INTEGRATION",
                      "preconditions": "İlk ödeme başarıyla tamamlanmış, aynı idempotency key",
                      "steps": ["Aynı idempotency key ile ikinci ödeme isteği gönderilir"],
                      "expectedResult": "409 Conflict, mükerrer ödeme hatası",
                      "actualResult": "409 Conflict, 'Bu ödeme işlemi zaten gerçekleştirilmiştir' mesajı döndü",
                      "status": "PASS",
                      "failureReason": "",
                      "linkedAcceptanceCriteria": "Aynı istek iki kez gönderildiğinde çift ödeme oluşmamalı"
                    },
                    {
                      "id": "TC-005",
                      "name": "testDosyaDetay_BaskaKisininDosyasi_403Doner",
                      "category": "SECURITY",
                      "priority": "CRITICAL",
                      "type": "API",
                      "preconditions": "JWT token TC=11111111111, dosya sahibi TC=22222222222",
                      "steps": ["GET /api/v1/icra-dosya/2024-12345/detay", "Response status kontrol edilir"],
                      "expectedResult": "403 Forbidden",
                      "actualResult": "403 Forbidden, 'Bu icra dosyasına erişim yetkiniz bulunmamaktadır' mesajı döndü",
                      "status": "PASS",
                      "failureReason": "",
                      "linkedAcceptanceCriteria": "Vatandaş sadece kendi dosyalarına erişebilmeli"
                    },
                    {
                      "id": "TC-006",
                      "name": "testOdeme_KapaliDosya_OdemeEngellenir",
                      "category": "EDGE_CASE",
                      "priority": "HIGH",
                      "type": "INTEGRATION",
                      "preconditions": "Dosya durumu KAPALI",
                      "steps": ["POST /api/v1/icra-dosya/2024-99999/odeme", "Response kontrol edilir"],
                      "expectedResult": "400 Bad Request, 'Bu dosya kapalıdır, ödeme yapılamaz' hatası",
                      "actualResult": "400 Bad Request, beklenen hata mesajı döndü",
                      "status": "PASS",
                      "failureReason": "",
                      "linkedAcceptanceCriteria": "Kapatılmış dosyaya ödeme yapılamamalı"
                    },
                    {
                      "id": "TC-007",
                      "name": "testOdeme_SQLInjection_Engellenir",
                      "category": "SECURITY",
                      "priority": "CRITICAL",
                      "type": "SECURITY",
                      "preconditions": "Geçerli JWT token",
                      "steps": ["tcKimlikNo=' OR 1=1-- parametresi gönderilir"],
                      "expectedResult": "400 Bad Request, validasyon hatası",
                      "actualResult": "400 Bad Request, regex validasyonu engelledi",
                      "status": "PASS",
                      "failureReason": ""
                    }
                  ],
                  "securityFindings": [],
                  "performanceFindings": [],
                  "codeQualityNotes": [
                    {
                      "area": "Yapısal Loglama",
                      "location": "IcraDosyaController, OdemeService",
                      "observation": "TC kimlik numarası loglarda maskeleniyor — sadece ilk 3 hane gösteriliyor",
                      "suggestion": "Mevcut uygulama yeterli ve PCI-DSS uyumlu",
                      "impact": "IMPROVEMENT"
                    }
                  ],
                  "acceptanceCriteriaVerification": [
                    {
                      "criterion": "TC kimlik ile dosya sorgulama",
                      "status": "VERIFIED",
                      "evidence": "TC-001 başarılı, doğru dosya listesi döndü"
                    },
                    {
                      "criterion": "Dosya detay görüntüleme (alacaklı, borç, faiz, masraf, toplam)",
                      "status": "VERIFIED",
                      "evidence": "TC-001 detay API testiyle doğrulandı, tüm alanlar mevcut"
                    },
                    {
                      "criterion": "Peşin ödeme %10 indirim",
                      "status": "VERIFIED",
                      "evidence": "TC-003 — 10000 TL'ye 1000 TL indirim doğru hesaplandı"
                    },
                    {
                      "criterion": "Çift ödeme koruması (idempotency)",
                      "status": "VERIFIED",
                      "evidence": "TC-004 — aynı key ile 409 Conflict döndü"
                    },
                    {
                      "criterion": "Yetkisiz dosya erişimi engelleme",
                      "status": "VERIFIED",
                      "evidence": "TC-005 — başka kişinin dosyasına 403 döndü"
                    }
                  ],
                  "bugs": [],
                  "suggestions": [
                    {
                      "type": "TEST_COVERAGE",
                      "suggestion": "UYAP API timeout senaryosu için circuit breaker testi eklenebilir",
                      "priority": "MEDIUM",
                      "effort": "M"
                    },
                    {
                      "type": "PERFORMANCE",
                      "suggestion": "Yüksek yük altında Redis cache hit/miss oranı izlenebilir",
                      "priority": "LOW",
                      "effort": "S"
                    }
                  ],
                  "regressionRisks": [
                    "Mevcut sisteme etkisi yok — tamamen yeni modül. Ancak UYAP client değişikliklerinde regression testi gerekli."
                  ],
                  "testDebt": [
                    "Taksitlendirme modülü (Faz 2) için test senaryoları henüz yazılmadı",
                    "UYAP API gerçek entegrasyonu için E2E testler gerekecek"
                  ]
                }
                """;
    }

    private String generateDeploymentResponse() {
        return """
                {
                  "deploymentId": "deploy-icra-takip-001",
                  "status": "SUCCESS",
                  "environment": "production",
                  "artifact": "icra-takip-service",
                  "version": "1.0.0",
                  "steps": [
                    {"step": "Docker image build (icra-takip-service:1.0.0)", "status": "SUCCESS", "duration": "52s"},
                    {"step": "Güvenlik taraması (Trivy scan — 0 critical, 0 high)", "status": "SUCCESS", "duration": "18s"},
                    {"step": "Docker image push to Harbor registry", "status": "SUCCESS", "duration": "15s"},
                    {"step": "Flyway DB migration (icra_dosya + odeme_islem tabloları)", "status": "SUCCESS", "duration": "3s"},
                    {"step": "Redis cache cluster bağlantı testi", "status": "SUCCESS", "duration": "2s"},
                    {"step": "Kubernetes rolling update (3 replica)", "status": "SUCCESS", "duration": "45s"},
                    {"step": "Health check (/actuator/health)", "status": "SUCCESS", "duration": "5s"},
                    {"step": "UYAP API bağlantı testi", "status": "SUCCESS", "duration": "3s"},
                    {"step": "Sanal POS bağlantı testi", "status": "SUCCESS", "duration": "4s"},
                    {"step": "Smoke test (dosya sorgulama + ödeme akışı)", "status": "SUCCESS", "duration": "12s"}
                  ],
                  "healthCheck": {
                    "endpoint": "/actuator/health",
                    "status": "UP",
                    "responseTimeMs": 38,
                    "components": {
                      "db": "UP",
                      "redis": "UP",
                      "uyapClient": "UP",
                      "posClient": "UP"
                    }
                  },
                  "rollbackPlan": "Önceki versiyon image'ı Harbor registry'de mevcut. 'kubectl rollout undo deployment/icra-takip-service' ile 30sn içinde geri alınabilir. DB migration geri alma: V1__rollback_icra_tables.sql hazır.",
                  "notes": "İcra Takip Uygulaması v1.0.0 production'a başarıyla deploy edildi. Zero-downtime rolling update uygulandı. 3 pod aktif ve sağlıklı. UYAP API ve Sanal POS bağlantıları doğrulandı. İlk 1 saat enhanced monitoring aktif."
                }
                """;
    }
}
