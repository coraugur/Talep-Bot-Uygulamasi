# Talep Bot — Proje Analiz Dokümanı

## 1. Proje Özeti

**Talep Bot**, bir IT departmanının Agile iş akışını simüle eden çok ajanlı (multi-agent) bir yapay zeka sistemidir. İş biriminden gelen serbest metin talepleri, LLM destekli beş özelleşmiş ajan tarafından sırasıyla işlenir:

**Product Owner → Analist → Developer ↔ Tester (döngü) → Deployment**

Her ajan, Anthropic Claude LLM modeli tarafından desteklenir ve kendi uzmanlık alanına göre yapılandırılmış system prompt'larla çalışır. Sistem, gerçek bir Agile ekibin iş akışını uçtan uca taklit eder.

---

## 2. Teknoloji Yığını

| Katman | Teknoloji | Versiyon |
|--------|-----------|----------|
| **Backend** | Java + Spring Boot | JDK 24 / Spring Boot 3.4.4 |
| **LLM Framework** | LangChain4j | 1.0.0-beta3 |
| **LLM Provider** | Anthropic Claude | claude-sonnet-4-20250514 |
| **Veritabanı** | H2 (dev) / PostgreSQL (prod) | 2.3.232 / 16+ |
| **Migration** | Flyway | 10.20.1 |
| **Build** | Maven (Wrapper) | 3.9.9 |
| **Frontend** | React + TypeScript + Vite | React 19 / Vite 8 |
| **Gerçek Zamanlı** | Server-Sent Events (SSE) | - |

---

## 3. Fonksiyonel Gereksinimler

### 3.1 Talep Yönetimi

| ID | Gereksinim | Öncelik |
|----|-----------|---------|
| FR-01 | Kullanıcı serbest metin olarak iş talebi oluşturabilmeli | Yüksek |
| FR-02 | Tüm talepler listelenebilmeli (tarih sırasıyla) | Yüksek |
| FR-03 | Talep detayı görüntülenebilmeli (tüm ajan çıktılarıyla) | Yüksek |
| FR-04 | Talep durumu izlenebilmeli (PENDING → ... → COMPLETED/FAILED) | Yüksek |

### 3.2 Pipeline İşleyişi

| ID | Gereksinim | Öncelik |
|----|-----------|---------|
| FR-05 | Talep oluşturulduktan sonra pipeline otomatik başlatılabilmeli | Yüksek |
| FR-06 | Pipeline asenkron çalışabilmeli (kullanıcıyı bekletmeden) | Yüksek |
| FR-07 | Pipeline senkron çalışabilmeli (test amaçlı) | Orta |
| FR-08 | SSE ile pipeline ilerlemesi gerçek zamanlı izlenebilmeli | Yüksek |

### 3.3 Ajan Gereksinimleri

| ID | Gereksinim | Öncelik |
|----|-----------|---------|
| FR-09 | **PO Ajanı**: Serbest metin talebi → User Story + Kabul Kriterleri dönüşümü | Yüksek |
| FR-10 | **Analist Ajanı**: User Story → Teknik Spesifikasyon (API, DB, güvenlik) | Yüksek |
| FR-11 | **Developer Ajanı**: Teknik Spec → Kaynak kodu (SOLID, Clean Code) | Yüksek |
| FR-12 | **Tester Ajanı**: Kod → Test raporu (PASS/FAIL) + Bug listesi | Yüksek |
| FR-13 | **Deployment Ajanı**: Onaylı kod → Deploy raporu (simülasyon) | Yüksek |
| FR-14 | Developer ↔ Tester döngüsü: FAIL durumunda developer'a geri bildirim | Yüksek |
| FR-15 | Döngü maksimum iterasyon ile sınırlandırılmalı (varsayılan: 3) | Yüksek |

### 3.4 Tool (Araç) Gereksinimleri

| ID | Gereksinim | Öncelik |
|----|-----------|---------|
| FR-16 | **Jira Mock**: Issue oluşturma, durum güncelleme, detay sorgulama | Orta |
| FR-17 | **GitHub Repo**: Dosya listeleme ve dosya içeriği okuma (REST API) | Orta |
| FR-18 | **Deploy Simülatörü**: Deploy, rollback ve health check simülasyonu | Orta |

---

## 4. Fonksiyonel Olmayan Gereksinimler

| ID | Gereksinim | Detay |
|----|-----------|-------|
| NFR-01 | **Performans** | Pipeline 5 dakika içinde tamamlanmalı |
| NFR-02 | **Eşzamanlılık** | Birden fazla pipeline paralel çalışabilmeli (4 thread pool) |
| NFR-03 | **Hata Toleransı** | Ajan hatalarında pipeline durmalı ve hata raporu kaydedilmeli |
| NFR-04 | **Güvenlik** | API key'ler environment variable ile yönetilmeli, .env dosyası git-ignored |
| NFR-05 | **CORS** | Frontend localhost:* portlarından erişebilmeli |
| NFR-06 | **Veri Bütünlüğü** | Her pipeline adımının çıktısı veritabanına kaydedilmeli |
| NFR-07 | **Mock Mod** | API key olmadan çalışabilmeli (test için mock LLM) |
| NFR-08 | **Gözlemlenebilirlik** | Tüm ajan çağrıları DEBUG seviyesinde loglanmalı |

---

## 5. Entity (Varlık) Diyagramı

### 5.1 Veritabanı Şeması

```
┌─────────────────────────────────────────────────────────┐
│                        TALEP                            │
├─────────────────────────────────────────────────────────┤
│  id              VARCHAR(36)    PK, UUID                │
│  description     VARCHAR(2000)  NOT NULL                │
│  status          VARCHAR(30)    NOT NULL, DEFAULT       │
│  created_at      TIMESTAMP      NOT NULL, DEFAULT NOW   │
│  updated_at      TIMESTAMP      nullable                │
│  user_story      TEXT           PO çıktısı (JSON)       │
│  tech_spec       TEXT           Analist çıktısı (JSON)  │
│  code_output     TEXT           Developer çıktısı (JSON)│
│  test_report     TEXT           Tester çıktısı (JSON)   │
│  deploy_report   TEXT           Deployment çıktısı(JSON)│
│  error_message   TEXT           Hata mesajı             │
│  iteration_count INT            NOT NULL, DEFAULT 0     │
├─────────────────────────────────────────────────────────┤
│  INDEX: idx_talep_status (status)                       │
│  INDEX: idx_talep_created_at (created_at DESC)          │
└─────────────────────────────────────────────────────────┘
```

### 5.2 Durum Makinesi (PipelineStatus)

```
PENDING
   │
   ▼
PO_IN_PROGRESS ──────────────────┐
   │                              │
   ▼                              │
ANALYST_IN_PROGRESS               │
   │                              │
   ▼                              │ (herhangi bir adımda
DEVELOPER_IN_PROGRESS             │  hata → FAILED)
   │                              │
   ▼                              │
TESTER_IN_PROGRESS ──┐            │
   │                 │            │
   │  FAIL ──► DEVELOPER_IN...   │
   │  (max 3 iterasyon)          │
   │                              │
   ▼ PASS                        │
DEPLOYMENT_IN_PROGRESS            │
   │                              │
   ▼                              ▼
COMPLETED                      FAILED
```

### 5.3 Veri Modelleri (DTO / Record)

```
┌──────────────────────┐   ┌───────────────────────────┐
│     UserStory        │   │     TechnicalSpec         │
├──────────────────────┤   ├───────────────────────────┤
│ title: String        │   │ summary: String           │
│ description: String  │   │ apiEndpoints: List        │
│ asA: String          │   │   ├─ method: String       │
│ iWant: String        │   │   ├─ path: String         │
│ soThat: String       │   │   ├─ description: String  │
│ acceptanceCriteria[] │   │   ├─ requestBody: String  │
│ priority: String     │   │   └─ responseBody: String │
│ businessRules[]      │   │ dataModels: List          │
│ openQuestions[]      │   │   ├─ tableName: String    │
└──────────────────────┘   │   ├─ fields: List         │
                           │   └─ indexes: List        │
                           │ constraints[]             │
                           │ edgeCases[]               │
                           │ securityConsiderations[]   │
                           │ impactAnalysis: String     │
                           └───────────────────────────┘

┌──────────────────────┐   ┌───────────────────────────┐
│     CodeOutput       │   │      TestReport           │
├──────────────────────┤   ├───────────────────────────┤
│ summary: String      │   │ status: PASS | FAIL       │
│ files: List          │   │ summary: String           │
│   ├─ filePath        │   │ totalTests: int           │
│   ├─ language        │   │ passedTests: int          │
│   ├─ content         │   │ failedTests: int          │
│   └─ description     │   │ testCases: List           │
│ configChanges[]      │   │   ├─ name: String         │
│ dependencies[]       │   │   ├─ type: UNIT|INTEG|... │
└──────────────────────┘   │   ├─ status: PASS|FAIL    │
                           │   ├─ description           │
                           │   └─ failureReason         │
┌──────────────────────┐   │ bugs[]                    │
│  DeploymentReport    │   │ suggestions[]             │
├──────────────────────┤   └───────────────────────────┘
│ success: boolean     │
│ environment: String  │   ┌───────────────────────────┐
│ version: String      │   │     PipelineEvent         │
│ deployedAt: DateTime │   ├───────────────────────────┤
│ steps[]              │   │ talepId: String            │
│ rollbackPlan: String │   │ agentName: String          │
│ healthCheckResults[] │   │ status: String             │
│ notes: String        │   │ message: String            │
└──────────────────────┘   │ timestamp: long            │
                           └───────────────────────────┘
```

---

## 6. Sistem Mimarisi

### 6.1 Katmanlı Mimari

```
┌─────────────────────────────────────────────────────────────┐
│                    FRONTEND (React + Vite)                   │
│  ┌──────────┐ ┌──────────────┐ ┌──────────┐ ┌───────────┐  │
│  │TalepForm │ │PipelineDash. │ │TalepList │ │TalepDetail│  │
│  └─────┬────┘ └──────┬───────┘ └─────┬────┘ └─────┬─────┘  │
│        └──────┬──────┘───────────────┘             │        │
│               │ REST API + SSE                     │        │
└───────────────┼────────────────────────────────────┼────────┘
                │ HTTP :5173 → :8080                 │
┌───────────────┼────────────────────────────────────┼────────┐
│               │        BACKEND (Spring Boot)       │        │
│  ┌────────────▼──────────────────┐  ┌──────────────▼─────┐  │
│  │       Controller Layer        │  │  GlobalException   │  │
│  │  TalepController              │  │  Handler           │  │
│  │  PipelineController (+ SSE)   │  └────────────────────┘  │
│  └────────────┬──────────────────┘                          │
│               │                                              │
│  ┌────────────▼──────────────────┐                          │
│  │        Service Layer          │                          │
│  │  TalepService (CRUD)          │                          │
│  │  PipelineService (async/SSE)  │                          │
│  └────────────┬──────────────────┘                          │
│               │                                              │
│  ┌────────────▼──────────────────┐  ┌─────────────────────┐ │
│  │     Orchestration Layer       │  │    Tool Layer       │ │
│  │  TalepPipelineOrchestrator    │  │  JiraMockTool       │ │
│  │  (PO→Analist→Dev↔Test→Deploy) │  │  GitHubRepoTool     │ │
│  └────────────┬──────────────────┘  │  DeploySimulatorTool│ │
│               │                      └──────────┬──────────┘ │
│  ┌────────────▼──────────────────────────────────▼─────────┐ │
│  │              Agent Layer (LangChain4j)                   │ │
│  │  ProductOwnerAgent   ──── @SystemMessage + @Tool        │ │
│  │  AnalystAgent        ──── @SystemMessage + @Tool        │ │
│  │  DeveloperAgent      ──── @SystemMessage + @Tool        │ │
│  │  TesterAgent         ──── @SystemMessage                │ │
│  │  DeploymentAgent     ──── @SystemMessage + @Tool        │ │
│  └────────────┬────────────────────────────────────────────┘ │
│               │                                              │
│  ┌────────────▼──────────────┐  ┌──────────────────────┐    │
│  │    LLM Provider           │  │   Database Layer     │    │
│  │  Anthropic Claude API     │  │   H2 / PostgreSQL    │    │
│  │  (veya MockChatModel)     │  │   Flyway Migrations  │    │
│  └───────────────────────────┘  └──────────────────────┘    │
└──────────────────────────────────────────────────────────────┘
```

---

## 7. Pipeline Akış Diyagramı

### 7.1 Ana Pipeline Akışı

```
Kullanıcı
   │
   │ POST /api/talep {description: "..."}
   ▼
┌─────────────────────┐
│ Talep Oluştur       │ status = PENDING
│ (TalepService)      │
└────────┬────────────┘
         │ POST /api/pipeline/run/{id}
         ▼
┌─────────────────────┐
│ PipelineService     │ status = PO_IN_PROGRESS
│ runAsync()          │──── SSE Emitter oluştur
└────────┬────────────┘
         │
         ▼
┌══════════════════════════════════════════════════════════════┐
║              TalepPipelineOrchestrator.run()                ║
║                                                              ║
║  ┌──────────────────┐                                        ║
║  │ ADIM 1: PO Agent │                                        ║
║  │ processRequest() │                                        ║
║  │ Tool: JiraMock   │                                        ║
║  └───────┬──────────┘                                        ║
║          │ userStory (JSON)                                  ║
║          ▼                                                    ║
║  ┌──────────────────────┐                                    ║
║  │ ADIM 2: Analist Agent│                                    ║
║  │ analyze()            │                                    ║
║  │ Tool: GitHubRepo     │                                    ║
║  └───────┬──────────────┘                                    ║
║          │ techSpec (JSON)                                   ║
║          ▼                                                    ║
║  ┌═══════════════════════════════════════════┐                ║
║  ║  ADIM 3: Developer ↔ Tester DÖNGÜSÜ      ║                ║
║  ║  (maks 3 iterasyon)                       ║                ║
║  ║                                            ║                ║
║  ║  ┌──────────────────┐                      ║                ║
║  ║  │ Developer Agent  │◄──── testFeedback    ║                ║
║  ║  │ develop()        │      (FAIL ise)      ║                ║
║  ║  │ Tool: GitHubRepo │                      ║                ║
║  ║  └───────┬──────────┘                      ║                ║
║  ║          │ codeOutput (JSON)               ║                ║
║  ║          ▼                                  ║                ║
║  ║  ┌──────────────────┐                      ║                ║
║  ║  │ Tester Agent     │                      ║                ║
║  ║  │ test()           │                      ║                ║
║  ║  └───────┬──────────┘                      ║                ║
║  ║          │ testReport (JSON)               ║                ║
║  ║          ▼                                  ║                ║
║  ║     PASS? ──── Evet ─► döngüden çık        ║                ║
║  ║       │                                    ║                ║
║  ║      Hayır ─► iterasyon < max? ─► devam    ║                ║
║  ║                    │                        ║                ║
║  ║                  Hayır ─► FAILED            ║                ║
║  ╚════════════════════════════════════════════╝                ║
║          │ (PASS)                                              ║
║          ▼                                                    ║
║  ┌─────────────────────────┐                                  ║
║  │ ADIM 4: Deployment Agent│                                  ║
║  │ deploy()                │                                  ║
║  │ Tool: DeploySimulator   │                                  ║
║  └───────┬─────────────────┘                                  ║
║          │ deployReport (JSON)                                ║
║          ▼                                                    ║
║       COMPLETED                                               ║
╚══════════════════════════════════════════════════════════════╝
         │
         ▼
┌─────────────────────┐
│ Sonuçları DB'ye     │ talep.userStory = ...
│ kaydet              │ talep.techSpec = ...
│ (PipelineService)   │ talep.status = COMPLETED/FAILED
└─────────────────────┘
```

### 7.2 SSE (Gerçek Zamanlı İzleme) Akışı

```
Frontend                          Backend
   │                                 │
   │ GET /api/pipeline/stream/{id}   │
   │────────────────────────────────►│
   │                                 │ SseEmitter oluştur
   │◄────── SSE Connection ─────────│
   │                                 │
   │  event: pipeline-event          │
   │  data: {stage:"PO",            │
   │         status:"STARTED",...}   │
   │◄───────────────────────────────│──── PO Agent başladı
   │                                 │
   │  event: pipeline-event          │
   │  data: {stage:"PO",            │
   │         status:"COMPLETED",...} │
   │◄───────────────────────────────│──── PO Agent bitti
   │                                 │
   │  event: pipeline-event          │
   │  data: {stage:"ANALYST",...}    │
   │◄───────────────────────────────│──── Analist başladı
   │         ...                     │
   │                                 │
   │  event: pipeline-event          │
   │  data: {stage:"PIPELINE",      │
   │         status:"COMPLETED"}    │
   │◄───────────────────────────────│──── Pipeline bitti
   │                                 │
   │ Connection closed               │
   │◄───────────────────────────────│
```

---

## 8. API Endpoint'leri

### 8.1 Talep Yönetimi

| Method | Endpoint | Açıklama | Request Body | Response |
|--------|----------|----------|-------------|----------|
| `POST` | `/api/talep` | Yeni talep oluştur | `{"description": "..."}` | `201 Created` + Talep JSON |
| `GET` | `/api/talep/{id}` | Talep detayını getir | - | `200 OK` + Talep JSON |
| `GET` | `/api/talep` | Tüm talepleri listele | - | `200 OK` + Talep[] JSON |

### 8.2 Pipeline Yönetimi

| Method | Endpoint | Açıklama | Response |
|--------|----------|----------|----------|
| `POST` | `/api/pipeline/run/{talepId}` | Pipeline'ı asenkron başlat | `202 Accepted` |
| `POST` | `/api/pipeline/run-sync/{talepId}` | Pipeline'ı senkron çalıştır | `200 OK` + Talep JSON |
| `GET` | `/api/pipeline/stream/{talepId}` | SSE stream (gerçek zamanlı) | `text/event-stream` |

### 8.3 Hata Yanıtları

| HTTP Kodu | Durum | Açıklama |
|-----------|-------|----------|
| `404` | Not Found | Talep bulunamadı |
| `409` | Conflict | Pipeline zaten çalışıyor |
| `500` | Internal Server Error | Beklenmeyen hata |

---

## 9. Ajan Detaylı Spesifikasyonları

### 9.1 Product Owner (PO) Ajanı

| Özellik | Değer |
|---------|-------|
| **Rol** | İş birimi ile teknik ekip arasında köprü |
| **Girdi** | Serbest metin iş talebi |
| **Çıktı** | UserStory JSON (title, asA/iWant/soThat, acceptanceCriteria, businessRules) |
| **Araç** | JiraMockTool (issue oluşturma/güncelleme) |
| **Bellek** | 20 mesajlık sohbet penceresi |
| **Davranış** | Empatik, iş odaklı, eksik kuralları fark eder, Türkçe |

### 9.2 Analist Ajanı

| Özellik | Değer |
|---------|-------|
| **Rol** | User Story'yi teknik tasarıma dönüştürme |
| **Girdi** | UserStory JSON + repo bağlamı |
| **Çıktı** | TechnicalSpec JSON (API, DB, kısıtlamalar, güvenlik) |
| **Araç** | GitHubRepoTool (mevcut kodu inceleme) |
| **Bellek** | 20 mesajlık sohbet penceresi |
| **Davranış** | Analitik, kuralcı, OWASP farkındalığı, Türkçe |

### 9.3 Developer Ajanı

| Özellik | Değer |
|---------|-------|
| **Rol** | Teknik spesifikasyonu çalışan koda dönüştürme |
| **Girdi** | TechSpec JSON + önceki test geri bildirimi + repo bağlamı |
| **Çıktı** | CodeOutput JSON (dosyalar, config değişiklikleri, bağımlılıklar) |
| **Araç** | GitHubRepoTool (mevcut kodla uyum) |
| **Bellek** | 30 mesajlık sohbet penceresi |
| **Davranış** | SOLID, Clean Code, loglama, hata yakalama, Türkçe açıklama |

### 9.4 Tester Ajanı

| Özellik | Değer |
|---------|-------|
| **Rol** | Kod kalite kontrolü ve test |
| **Girdi** | Kod + kabul kriterleri + teknik spec |
| **Çıktı** | TestReport JSON (PASS/FAIL, test listesi, buglar, öneriler) |
| **Araç** | Yok (saf analiz) |
| **Bellek** | 30 mesajlık sohbet penceresi |
| **Davranış** | Şüpheci, unit/integration/edge case/security test üretir, Türkçe |

### 9.5 Deployment Ajanı

| Özellik | Değer |
|---------|-------|
| **Rol** | Onaylı kodu canlı ortama deploy etme (simülasyon) |
| **Girdi** | Kod + test raporu |
| **Çıktı** | DeploymentReport JSON (adımlar, health check, rollback planı) |
| **Araç** | DeploySimulatorTool (deploy, rollback, health check) |
| **Bellek** | 20 mesajlık sohbet penceresi |
| **Davranış** | Sıfır hata toleransı, test onaysız kodu reddeder, Türkçe |

---

## 10. Tool (Araç) Spesifikasyonları

### 10.1 JiraMockTool

In-memory ConcurrentHashMap tabanlı mock Jira servisi.

| Metod | Açıklama | Parametreler | Dönüş |
|-------|----------|-------------|-------|
| `createIssue` | Yeni issue oluşturur | title, description | Issue ID (TALEP-XXXXXX) |
| `updateIssueStatus` | Issue durumunu günceller | issueId, newStatus | Başarı mesajı |
| `getIssue` | Issue detayını getirir | issueId | Issue bilgileri |

**Durumlar:** TO_DO → IN_PROGRESS → IN_REVIEW → DONE / REJECTED

### 10.2 GitHubRepoTool

GitHub REST API v3 üzerinden repo dosyalarını okur.

| Metod | Açıklama | Parametreler | Dönüş |
|-------|----------|-------------|-------|
| `listFiles` | Klasördeki dosyaları listeler | owner, repo, path | Dosya listesi |
| `readFile` | Dosya içeriğini okur | owner, repo, filePath | Dosya içeriği (max 10K kar.) |

**Konfigürasyon:** `github.token`, `github.default-owner`, `github.default-repo`

### 10.3 DeploySimulatorTool

In-memory deploy simülasyonu (Docker/K8s senaryoları).

| Metod | Açıklama | Parametreler | Dönüş |
|-------|----------|-------------|-------|
| `deploy` | Artifact deploy eder | artifactName, version | Deploy raporu |
| `rollback` | Deployment'ı geri alır | deploymentId | Rollback sonucu |
| `healthCheck` | Sağlık kontrolü yapar | deploymentId | Health check sonucu |

**Durumlar:** ACTIVE → SUPERSEDED / ROLLED_BACK

---

## 11. Frontend Sayfa Haritası

```
/                          → TalepList (Tüm talepler)
/new                       → TalepForm (Yeni talep oluştur)
/talep/:talepId            → TalepDetail (Talep detayı + ajan çıktıları)
/pipeline/:talepId         → PipelineDashboard (Canlı pipeline izleme)
```

| Sayfa | Açıklama |
|-------|----------|
| **TalepList** | Tüm talepleri tablo halinde listeler (durum, iterasyon, tarih) |
| **TalepForm** | Textarea ile talep açıklaması alır, pipeline'ı otomatik başlatır |
| **PipelineDashboard** | 5 aşama kartı (PO, Analist, Developer, Tester, Deploy) + SSE ile canlı güncelleme + olay günlüğü |
| **TalepDetail** | Sekmeli arayüz: User Story, Teknik Spec, Kod, Test Raporu, Deploy Raporu |

---

## 12. Konfigürasyon Parametreleri

| Parametre | Varsayılan | Açıklama |
|-----------|-----------|----------|
| `server.port` | 8080 | Backend port |
| `langchain4j.anthropic.chat-model.api-key` | `${ANTHROPIC_API_KEY:demo}` | Claude API anahtarı |
| `langchain4j.anthropic.chat-model.model-name` | claude-sonnet-4-20250514 | Model adı |
| `langchain4j.anthropic.chat-model.max-tokens` | 4096 | Maksimum token |
| `langchain4j.anthropic.chat-model.temperature` | 0.3 | Yaratıcılık seviyesi |
| `pipeline.developer-tester-max-iterations` | 3 | Dev↔Tester maks döngü |
| `github.token` | `${GITHUB_TOKEN}` | GitHub API token |
| `github.default-owner` | `${GITHUB_OWNER}` | Varsayılan repo sahibi |
| `github.default-repo` | `${GITHUB_REPO}` | Varsayılan repo |

---

## 13. Proje Dizin Yapısı

```
talep-bot/                          ← Backend (Spring Boot)
├── pom.xml
├── .env                            ← API key'ler (git-ignored)
├── .gitignore
├── mvnw.cmd                        ← Maven Wrapper
├── .mvn/wrapper/
├── src/main/java/com/atmosware/talepbot/
│   ├── TalepBotApplication.java
│   ├── config/
│   │   ├── LangChainConfig.java    ← Agent bean tanımları
│   │   ├── MockChatLanguageModel.java
│   │   └── WebConfig.java          ← CORS
│   ├── agent/
│   │   ├── ProductOwnerAgent.java
│   │   ├── AnalystAgent.java
│   │   ├── DeveloperAgent.java
│   │   ├── TesterAgent.java
│   │   ├── DeploymentAgent.java
│   │   └── TalepPipelineOrchestrator.java
│   ├── tool/
│   │   ├── JiraMockTool.java
│   │   ├── GitHubRepoTool.java
│   │   └── DeploySimulatorTool.java
│   ├── model/
│   │   ├── PipelineStatus.java
│   │   ├── TestStatus.java
│   │   ├── PipelineEvent.java
│   │   ├── UserStory.java
│   │   ├── TechnicalSpec.java
│   │   ├── CodeOutput.java
│   │   ├── TestReport.java
│   │   └── DeploymentReport.java
│   ├── entity/
│   │   └── Talep.java
│   ├── repository/
│   │   └── TalepRepository.java
│   ├── service/
│   │   ├── TalepService.java
│   │   └── PipelineService.java
│   └── controller/
│       ├── TalepController.java
│       ├── PipelineController.java
│       └── GlobalExceptionHandler.java
├── src/main/resources/
│   ├── application.yml
│   ├── prompts/
│   │   ├── po-system.txt
│   │   ├── analyst-system.txt
│   │   ├── developer-system.txt
│   │   ├── tester-system.txt
│   │   └── deployment-system.txt
│   └── db/migration/
│       └── V1__init_talep_table.sql
│
talep-bot-ui/                       ← Frontend (React + Vite)
├── package.json
├── src/
│   ├── main.tsx
│   ├── App.tsx                     ← Router tanımları
│   ├── App.css                     ← Global stiller
│   ├── types.ts                    ← TypeScript tipleri
│   ├── api.ts                      ← Axios + SSE servisi
│   └── pages/
│       ├── TalepForm.tsx
│       ├── TalepList.tsx
│       ├── TalepDetail.tsx
│       └── PipelineDashboard.tsx
```

---

## 14. Kapsam Dışı

- Gerçek Jira entegrasyonu (şu an mock)
- Gerçek CI/CD pipeline tetikleme
- Kimlik doğrulama / yetkilendirme (authentication/authorization)
- WebSocket desteği (SSE yeterli)
- Gerçek Docker/Kubernetes deploy
- Çoklu LLM provider desteği (şu an sadece Anthropic)
