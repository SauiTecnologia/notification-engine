Notification Engine - Sistema de Notificações Multi-canal
Microserviço Quarkus responsável pelo gerenciamento e envio de notificações através de múltiplos canais (email, WhatsApp, SMS, in-app, push) com integração completa ao workflow de negócio da plataforma Apporte.

Características Principais
Arquitetura em Camadas - Controller → Service → Repository → Integrações

Design Patterns - Repository, Strategy (Template), Observer (Event-driven), Circuit Breaker, Cache

Multi-canal - Suporte a email, WhatsApp Web, SMS, notificações in-app e push

Validações em Cascata - Bean Validation em DTOs + validações de negócio

Templates Dinâmicos - Qute templates para email e WhatsApp com variáveis contextuais

Resolução Inteligente de Destinatários - Baseada em roles, projetos e contextos

Histórico Completo - Persistência de todas as notificações com status e retry

Event-Driven - Integração com eventos de workflow

Resiliente - Retry automático, circuit breakers, timeouts configuráveis

Administração Completa - Dashboard com estatísticas, filtros e gerenciamento

Estrutura do Sistema
text
39 classes Java
├── 4 Entidades JPA
├── 6 Repositories/Panache
├── 10 Serviços de Negócio
├── 4 Controladores REST
├── 12 DTOs (Request/Response)
├── 4 Componentes de Segurança
└── 2 Handlers de Exceção
Entidades Principais
Notification
Registro central de todas as notificações enviadas

userId, eventType, channel

status (pending, sent, error, retrying, retried)

payloadJson (dados completos para retry)

errorMessage, createdAt, sentAt

User (Cache)
Cache de usuários sincronizado com Keycloak

Dados básicos: email, nome, telefone

Roles em JSON para resolução de destinatários

Última sincronização para cache

Project
Entidade para resolução de destinatários "project_owner"

Dono do projeto com ID, email e nome

Usado em notificações de workflow

RecipientResolution
Objeto de resolução em tempo de execução

Combinação de userId, email, telefone, nome

Tipo de destinatário (project_owner, admin, etc)

Metadados contextuais

Fluxo: Processar Notificação de Workflow
text
POST /api/notifications/from-workflow
↓
NotificationController → NotificationService
↓
processWorkflowNotification()
├─ RecipientResolverService.resolveRecipients() # Strategy Pattern
│   ├─ project_owner → busca dono do projeto
│   ├─ admins → busca usuários com role admin
│   ├─ workflow_participants → extrai do contexto
│   └─ manual → userId direto
↓
Para cada destinatário e canal:
├─ EmailService.sendEmail()          # Qute templates
├─ WhatsAppService.sendMessage()     # Selenium WebDriver
├─ In-app/SMS (implementáveis)
↓
NotificationRepository.persist()     # Repository Pattern
↓
WorkflowEventManager (Future)        # Observer Pattern
↓
HTTP 202 Accepted + requestId
Quick Start (Ambiente de Desenvolvimento)
1. Pré-requisitos
bash
# Java 21+
java -version

# Maven 3.8+
mvn -v

# Chrome/Chromium (para WhatsApp)
chrome --version

# Docker (opcional para PostgreSQL)
docker --version
2. Configurar Ambiente
bash
# 1. Clonar projeto
git clone https://github.com/SauiTecnologia/notification-engine.git
cd notification-engine

# 2. Configurar variáveis de ambiente
cp .env.example .env
# Edite .env com suas credenciais
nano .env

# 3. Executar em modo desenvolvimento
./mvnw quarkus:dev -Dquarkus.profile=dev
3. Verificar Saúde do Sistema
bash
# Health Check básico
curl http://localhost:8082/api/notifications

# Swagger UI
http://localhost:8082/swagger-ui
4. Testar Notificações
bash
# Enviar notificação manual (email mock)
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
Documentação da API
Endpoints Públicos
Método	Endpoint	Descrição	Autenticação
GET	/api/notifications	Health check básico	Nenhuma
POST	/api/notifications/from-workflow	Processar notificação de workflow	JWT
POST	/api/notifications/send	Envio manual de notificação	JWT
POST	/api/notifications/batch	Envio em lote	JWT
GET	/api/notifications/status/{id}	Status da notificação	JWT
GET	/api/notifications/user/{userId}	Notificações por usuário	JWT
Endpoints Administrativos
Método	Endpoint	Descrição	Roles
GET	/api/admin/notifications	Listar com filtros	notification-admin
GET	/api/admin/notifications/{id}	Buscar por ID	notification-admin
DELETE	/api/admin/notifications/{id}	Remover notificação	admin
POST	/api/admin/notifications/{id}/retry	Reprocessar com erro	notification-admin
GET	/api/admin/notifications/stats	Estatísticas do sistema	notification-admin
GET	/api/admin/notifications/health	Health check detalhado	notification-admin
Exemplos de Payload
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
    "deadline": "2024-12-31",
    "fromColumn": "Backlog",
    "toColumn": "Em Desenvolvimento"
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
    "priority": "HIGH",
    "deadline": "2024-12-15"
  }
}
Padrões de Projeto Implementados
Repository Pattern
NotificationRepository - CRUD + queries customizadas

UserRepository - Cache de usuários com queries por role

ProjectRepository - Resolução de donos de projeto

Strategy Pattern
RecipientResolverService - Estratégias de resolução por tipo

WhatsAppTemplateService - Seleção de template por eventType

EmailService - Renderização de template baseado em evento

Command Pattern (Implícito)
NotificationService.processWorkflowNotification() - Comando de processamento

NotificationService.retryNotification() - Comando de retry

Fluent interface com validações em cascata

Observer Pattern (Planejado)
WorkflowEventManager - Para integração futura

Eventos: NotificationSent, NotificationFailed, RetryAttempted

Circuit Breaker Pattern
@Retry(maxRetries = 3, delay = 1000) - Em NotificationService

@Timeout(5000) - Timeout por operação

Fallbacks em serviços de integração

Serviços Principais
NotificationService
Responsabilidades:

Orquestração do fluxo completo

Criação e persistência de registros

Gerenciamento de retry automático

Tratamento de erros e fallbacks

java
@Transactional
@Retry(maxRetries = 3, delay = 1000)
@Timeout(5000)
public void processWorkflowNotification(WorkflowNotificationRequest request) {
    // 1. Resolve destinatários
    // 2. Para cada destinatário e canal:
    //    - Cria registro
    //    - Chama serviço específico
    //    - Atualiza status
    //    - Persiste
}
RecipientResolverService
Estratégias de Resolução:

project_owner - Busca dono na tabela projects

admins - Usuários com roles de administrador

workflow_participants - Extrai do contexto

specific_users - Emails do contexto

manual - userId direto

Cache: TTL de 300s, máximo 1000 entradas

WhatsAppService
Funcionalidades:

Inicialização automática do ChromeDriver

Autenticação via QR Code com timeout

Envio com retry exponencial (1, 2, 4 segundos)

Validação de números de telefone

Rate limiting (1 mensagem/hora por número)

Persistência de sessão

Configurações:

properties
whatsapp.enabled=false          # Habilitar em produção
whatsapp.headless=false         # Headless mode
whatsapp.max.retries=3          # Tentativas
whatsapp.timeout.seconds=30     # Timeout por operação
EmailService
Funcionalidades:

Renderização de templates HTML com Qute

Envio via Quarkus Mailer

Assuntos dinâmicos por tipo de evento

Fallback para template simples

Templates:

emails/project-ready-review.html - Template padrão

Variáveis: nome, projectTitle, fromColumn, toColumn, etc.

WhatsAppTemplateService
Templates Disponíveis:

project_approval - Aprovação de projeto

task_assignment - Atribuição de tarefa

deadline_reminder - Lembrete de prazo

status_update - Atualização de status

project_completed - Projeto concluído

default - Template genérico

Limitações:

Máximo 4096 caracteres

Limpeza automática de HTML

Normalização de espaços

Segurança e Autenticação
Roles e Permissões
admin - Acesso completo (inclui DELETE)

notification-admin - Acesso administrativo

notification-sender - Pode enviar notificações

Configuração Keycloak
yaml
app:
  keycloak:
    admin:
      server-url: ${KEYCLOAK_ADMIN_URL}
      client-id: admin-cli
      username: ${KEYCLOAK_ADMIN_USER}
      password: ${KEYCLOAK_ADMIN_PASS}
Filtros de Segurança
SecurityFilter - Validação JWT

RequiresRoleFilter - Controle de acesso por role

NotificationExceptionMapper - Tratamento padronizado de erros

Build e Deployment
Build com Maven
bash
# Desenvolvimento
mvn clean compile quarkus:dev -Dquarkus.profile=dev

# Testes
mvn clean test -Dquarkus.profile=test

# Produção
mvn clean package -Dquarkus.profile=prod -DskipTests
Docker
bash
# Build com Jib
mvn package -Dquarkus.container-image.build=true

# Executar com variáveis de ambiente
docker run -p 8082:8082 \
  --env-file .env \
  apporte/notification-engine:latest
Kubernetes (Exemplo)
yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: notification-engine
spec:
  replicas: 2
  template:
    spec:
      containers:
      - name: notification
        image: apporte/notification-engine:latest
        env:
        - name: DB_JDBC_URL
          valueFrom:
            secretKeyRef:
              name: notification-secrets
              key: db-jdbc-url
        - name: DB_USERNAME
          valueFrom:
            secretKeyRef:
              name: notification-secrets
              key: db-username
        - name: DB_PASSWORD
          valueFrom:
            secretKeyRef:
              name: notification-secrets
              key: db-password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
        livenessProbe:
          httpGet:
            path: /q/health/live
            port: 8082
        readinessProbe:
          httpGet:
            path: /q/health/ready
            port: 8082
Monitoramento e Observabilidade
Health Checks
text
GET /api/admin/notifications/health     # Health check customizado
GET /q/health/live                      # Liveness probe
GET /q/health/ready                     # Readiness probe
Métricas (Prometheus)
text
GET /q/metrics
Métricas principais:

app_notifications_total{status="sent"}

app_notifications_duration_seconds

app_whatsapp_messages_sent_total

http_server_requests_seconds_count

Logging Estruturado
properties
quarkus.log.level=DEBUG
quarkus.log.console.json=false
quarkus.log.category."com.apporte".level=DEBUG
Exemplo de log:

text
2024-12-15 14:30:00 INFO  [c.a.c.s.NotificationService] Processing workflow notification: PROJECT_APPROVAL
2024-12-15 14:30:01 INFO  [c.a.c.s.EmailService] Email sent successfully to user@apporte.com for event: PROJECT_APPROVAL
Testes (Estrutura)
Testes Unitários
java
@QuarkusTest
class NotificationServiceTest {
    
    @InjectMock
    EmailService emailService;
    
    @Test
    void testProcessWorkflowNotification() {
        // Testar fluxo completo
    }
    
    @Test
    void testRetryNotification() {
        // Testar mecanismo de retry
    }
}
Testes de Integração
java
@QuarkusTest
@TestHTTPEndpoint(NotificationController.class)
@TestSecurity(user = "test-user", roles = {"notification-sender"})
class NotificationControllerTest {
    
    @Test
    void testSendNotification() {
        given()
            .contentType(ContentType.JSON)
            .body(createRequest())
            .when()
            .post("/send")
            .then()
            .statusCode(202);
    }
}
Configuração Avançada
Banco de Dados
yaml
quarkus:
  datasource:
    db-kind: postgresql
    jdbc:
      url: ${DB_JDBC_URL}
      max-size: 10
      min-size: 2
  hibernate-orm:
    database:
      generation: update
Cache
yaml
quarkus:
  cache:
    caffeine:
      "recipients-cache":
        expire-after-write: 300s
        maximum-size: 1000
      "users-cache":
        expire-after-write: 3600s
        maximum-size: 5000
Fault Tolerance
yaml
app:
  notification:
    retry:
      max-attempts: 3
      initial-delay: 1000ms
      max-delay: 10000ms
      jitter: 0.5
Troubleshooting
Problemas Comuns
Erro de Conexão com Banco:

# Testar conexão
psql -h ${DB_HOST} -p ${DB_PORT} -U ${DB_USERNAME} -d ${DB_NAME} --set=sslmode=require
WhatsApp Não Envia:

Verificar se Chrome está instalado: chrome --version

Verificar permissões do diretório de sessão

Verificar logs: grep -i "whatsapp\|selenium" application.log

Email Não Envia:

Verificar configurações SMTP em .env

Usar mock em dev: EMAIL_MOCK=true

Testar conexão SMTP manualmente

Diagnóstico

# Health check
curl -s "http://localhost:8082/api/admin/notifications/health" | jq .

# Estatísticas
curl -s "http://localhost:8082/api/admin/notifications/stats?days=1" | jq .

Status do Projeto

✅ Implementado
Arquitetura em camadas completa

Entidades JPA e repositórios

Serviços de email e WhatsApp

Sistema de templates (Qute)

Resolução de destinatários

API REST completa

Segurança JWT + roles

Persistência em PostgreSQL

Health checks e métricas

Sistema de retry automático

Dashboard administrativo

Logging estruturado

🔄 Em Desenvolvimento

Sistema de filas para processamento assíncrono

Webhooks para status de entrega

Última Atualização: Dezembro 2024
Versão: 1.0.0-SNAPSHOT
Ambiente de Referência: Desenvolvimento (DigitalOcean PostgreSQL)
Status: Produção - Fase 1 (Core Funcional)