# Notification Engine — Sistema de Notificações Multi-canal

Microserviço **Quarkus** responsável pelo gerenciamento e envio de notificações através de múltiplos canais (**email, WhatsApp, SMS, in-app, push**) com integração completa ao workflow de negócio da plataforma **Apporte**.

---

## ✨ Características Principais

- **Arquitetura em Camadas**  
  Controller → Service → Repository → Integrações

- **Design Patterns**
  - Repository  
  - Strategy (Template)  
  - Observer (Event-driven)  
  - Circuit Breaker  
  - Cache  

- **Multi-canal**
  - Email  
  - WhatsApp Web  
  - SMS  
  - Notificações in-app  
  - Push  

- **Validações em Cascata**
  - Bean Validation em DTOs  
  - Validações de negócio  

- **Templates Dinâmicos**
  - Qute templates para email e WhatsApp  
  - Variáveis contextuais  

- **Resolução Inteligente de Destinatários**
  - Baseada em roles, projetos e contexto  

- **Histórico Completo**
  - Persistência de todas as notificações  
  - Status e retry  

- **Event-Driven**
  - Integração com eventos de workflow  

- **Resiliência**
  - Retry automático  
  - Circuit breakers  
  - Timeouts configuráveis  

- **Administração Completa**
  - Dashboard  
  - Estatísticas  
  - Filtros e gerenciamento  

---

## 🏗️ Estrutura do Sistema

39 classes Java
├── 4 Entidades JPA
├── 6 Repositories / Panache
├── 10 Serviços de Negócio
├── 4 Controladores REST
├── 12 DTOs (Request / Response)
├── 4 Componentes de Segurança
└── 2 Handlers de Exceção

---

## 📦 Entidades Principais

### Notification
Registro central de todas as notificações enviadas.

- `userId`, `eventType`, `channel`
- `status` (pending, sent, error, retrying, retried)
- `payloadJson` (dados completos para retry)
- `errorMessage`
- `createdAt`, `sentAt`

### User (Cache)
Cache de usuários sincronizado com Keycloak.

- Email, nome, telefone  
- Roles em JSON  
- Última sincronização  

### Project
Usada para resolução de destinatários `project_owner`.

- ID, email e nome do dono do projeto  

### RecipientResolution
Objeto de resolução em tempo de execução.

- userId, email, telefone, nome  
- Tipo de destinatário  
- Metadados contextuais  

---

## 🔄 Fluxo: Processar Notificação de Workflow

POST /api/notifications/from-workflow
↓
NotificationController → NotificationService
↓
processWorkflowNotification()
├─ RecipientResolverService.resolveRecipients()
│ ├─ project_owner
│ ├─ admins
│ ├─ workflow_participants
│ └─ manual
↓
Para cada destinatário e canal:
├─ EmailService.sendEmail()
├─ WhatsAppService.sendMessage()
├─ In-app / SMS
↓
NotificationRepository.persist()
↓
WorkflowEventManager (Future)
↓
HTTP 202 Accepted + requestId

---

## 🚀 Quick Start (Ambiente de Desenvolvimento)

### 1️⃣ Pré-requisitos

```bash
java -version          # Java 21+
mvn -v                 # Maven 3.8+
chrome --version       # Chrome/Chromium
docker --version       # Docker (opcional)
2️⃣ Configurar Ambiente

git clone https://github.com/SauiTecnologia/notification-engine.git
cd notification-engine

cp .env.example .env
nano .env

./mvnw quarkus:dev -Dquarkus.profile=dev
3️⃣ Verificar Saúde

curl http://localhost:8082/api/notifications
Swagger UI:
http://localhost:8082/swagger-ui

4️⃣ Testar Notificações

curl -X POST "http://localhost:8082/api/notifications/send" \
  -H "Content-Type: application/json" \
  -d '{
    "eventType": "TASK_ASSIGNMENT",
    "channel": "email",
    "recipientId": "user-001",
    "context": {
      "taskTitle": "Revisar Documentação",
      "priority": "HIGH"
    }
  }'
📚 Documentação da API
Endpoints Públicos
Método	Endpoint	Descrição	Auth
GET	/api/notifications	Health check	❌
POST	/from-workflow	Workflow	JWT
POST	/send	Envio manual	JWT
POST	/batch	Envio em lote	JWT
GET	/status/{id}	Status	JWT
GET	/user/{userId}	Por usuário	JWT

Endpoints Administrativos
Método	Endpoint	Descrição	Role
GET	/admin/notifications	Listar	notification-admin
GET	/admin/notifications/{id}	Buscar	notification-admin
DELETE	/admin/notifications/{id}	Remover	admin
POST	/admin/notifications/{id}/retry	Retry	notification-admin
GET	/admin/notifications/stats	Estatísticas	notification-admin
GET	/admin/notifications/health	Health	notification-admin

🧾 Exemplos de Payload
WorkflowNotificationRequest
json

{
  "eventType": "PROJECT_APPROVAL",
  "entityType": "project",
  "entityId": "proj-12345",
  "channels": ["email", "whatsapp"],
  "recipients": ["project_owner", "admins"],
  "context": {
    "projectTitle": "Novo Projeto",
    "priority": "HIGH",
    "deadline": "2024-12-31"
  }
}
SimpleNotificationRequest
json

{
  "eventType": "TASK_ASSIGNMENT",
  "channel": "email",
  "recipientId": "user-001",
  "context": {
    "taskTitle": "Revisar Documentação",
    "priority": "HIGH"
  }
}
🧠 Padrões de Projeto Implementados
Repository Pattern

Strategy Pattern

Command Pattern

Observer Pattern (planejado)

Circuit Breaker

🧩 Serviços Principais
NotificationService
java

@Transactional
@Retry(maxRetries = 3, delay = 1000)
@Timeout(5000)
public void processWorkflowNotification(WorkflowNotificationRequest request) {
    // fluxo completo
}
RecipientResolverService
Estratégias:

project_owner

admins

workflow_participants

specific_users

manual

Cache: TTL 300s | máx. 1000 entradas

📧 EmailService
Qute templates

Quarkus Mailer

Fallback automático

💬 WhatsAppService
Selenium WebDriver

QR Code

Retry exponencial

Rate limit: 1 msg/hora

Configurações:

properties

whatsapp.enabled=false
whatsapp.headless=false
whatsapp.max.retries=3
whatsapp.timeout.seconds=30
🔐 Segurança
Roles:

admin

notification-admin

notification-sender

Keycloak configurável via application.yaml.

🐳 Build & Deployment
Maven

mvn clean package -Dquarkus.profile=prod
Docker

mvn package -Dquarkus.container-image.build=true
docker run -p 8082:8082 --env-file .env apporte/notification-engine
Kubernetes
(Snippet incluído no README original)

📊 Monitoramento
Health checks (/q/health)

Métricas Prometheus (/q/metrics)

Logs estruturados

🧪 Testes
Testes unitários (QuarkusTest)

Testes de integração (REST + Security)

⚙️ Configuração Avançada
PostgreSQL

Cache Caffeine

Retry configurável

Fault tolerance

🛠️ Troubleshooting
Banco de dados

WhatsApp / Selenium

SMTP / Email mock

📌 Status do Projeto
✅ Implementado
Core funcional

Email e WhatsApp

Templates

Segurança

Métricas

Retry

Dashboard

🔄 Em Desenvolvimento
Filas assíncronas

Webhooks de status

Versão: 1.0.0-SNAPSHOT
Última atualização: Dezembro 2024
Status: Produção — Fase 1
Ambiente: DigitalOcean + PostgreSQL