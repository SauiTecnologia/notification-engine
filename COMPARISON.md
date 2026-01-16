# 📋 Comparação de Estrutura: Workflow-Engine vs Notification-Engine

## ✅ Padronização Completa Aplicada

### 📁 Arquivos de Configuração

| Aspecto | Workflow-Engine | Notification-Engine | Status |
|---------|----------------|---------------------|--------|
| **Formato Config** | `.properties` | `.properties` ✅ | ✅ Alinhado |
| **Arquivo Base** | `application.properties` | `application.properties` | ✅ Alinhado |
| **Dev Config** | `application-dev.properties` | `application-dev.properties` | ✅ Alinhado |
| **Prod Config** | `application-prod.properties` | `application-prod.properties` | ✅ Alinhado |
| **Variáveis Env** | `.envrc` (direnv) | `.envrc` (direnv) | ✅ Alinhado |

### 🗄️ Banco de Dados

| Configuração | Workflow-Engine | Notification-Engine |
|--------------|----------------|---------------------|
| **Provider** | Supabase PostgreSQL | Supabase PostgreSQL ✅ |
| **Host** | `db.rpkqbesfgjdeolketoug.supabase.co` | `db.rpkqbesfgjdeolketoug.supabase.co` ✅ |
| **Port** | 5432 (IPv6) | 5432 (IPv6) ✅ |
| **Database** | postgres | postgres ✅ |
| **User** | workflow_backend | workflow_backend ✅ |
| **Password** | `R$97J2:mtRz;g5R?3#$n#@BMs~0SB.)J` | Compartilhado ✅ |
| **SSL Mode** | require | require ✅ |
| **Pool Size** | 5-20 | 5-20 ✅ |
| **Hibernate DDL** | update | update ✅ |
| **SQL Logging** | true (dev) | true (dev) ✅ |

### 🔐 Autenticação Keycloak

| Configuração | Workflow-Engine | Notification-Engine |
|--------------|----------------|---------------------|
| **Auth Server** | `https://auth.apporte.work` | `https://auth.apporte.work` ✅ |
| **Realm** | development | development ✅ |
| **Client ID** | workflow-engine-dev | notification-engine ✅ |
| **Client Secret** | E6Vy7He2wemRyUDdfXDfyNsOAIwNk43u | (Criar client) ⏳ |
| **Roles Path** | realm_access/roles | realm_access/roles ✅ |
| **Token Issuer** | Configurado | Configurado ✅ |
| **OIDC Enabled** | true | true ✅ |

### 🚀 Servidor HTTP

| Configuração | Workflow-Engine | Notification-Engine |
|--------------|----------------|---------------------|
| **Port** | 8080 | 8082 ✅ |
| **Test Port** | 8081 | 8083 ✅ |
| **Host** | 0.0.0.0 | 0.0.0.0 ✅ |
| **CORS** | Enabled | Enabled ✅ |
| **CORS Origins** | localhost:3000, 3001 | localhost:3000, 3001, 8080 ✅ |

### 📊 Logs e Observabilidade

| Configuração | Workflow-Engine | Notification-Engine |
|--------------|----------------|---------------------|
| **Log Level** | DEBUG (dev) | DEBUG (dev) ✅ |
| **SQL Logging** | DEBUG | DEBUG ✅ |
| **Bind Params** | TRACE | TRACE ✅ |
| **OIDC Logging** | DEBUG | DEBUG ✅ |
| **Console Format** | HH:mm:ss pattern | HH:mm:ss pattern ✅ |
| **File Logging** | false (dev) | false (dev) ✅ |
| **Health Checks** | /q/health | /q/health ✅ |
| **Metrics** | Disabled (dev) | Disabled (dev) ✅ |

### 🏗️ Estrutura de Código Java

#### Workflow-Engine
```
com.apporte/
├── controller/       → REST endpoints (@RestController)
├── domain/          → Modelos de domínio + eventos
├── entity/          → Entidades JPA (@Entity)
├── repository/      → Repositórios (@Repository)
├── service/         → Serviços de negócio (@ApplicationScoped)
├── security/        → KeycloakUserContext, UserContext
├── dto/             → Records Java 21
├── command/         → Command pattern
├── client/          → Clientes REST externos
├── health/          → Health checks customizados
└── validator/       → Validações customizadas
```

#### Notification-Engine
```
com.apporte/
├── api/             → Controllers REST (@Path)
│   ├── dto/         → DTOs de API
│   └── util/        → Utilities da API
├── core/            → Lógica de negócio
│   ├── model/       → Entidades (@Entity)
│   ├── repository/  → Repositórios (@Repository)
│   ├── service/     → Serviços (@ApplicationScoped)
│   └── dto/         → DTOs do core
└── infrastructure/  → Infraestrutura
    ├── client/      → Clientes externos
    │   └── dto/     → DTOs de clientes
    ├── security/    → KeycloakUserContext, UserContext
    └── exception/   → Exceções customizadas
```

**Observação:** Ambas seguem Clean Architecture com separação clara de camadas.

### 🛠️ Classes de Segurança (Idênticas)

| Classe | Workflow-Engine | Notification-Engine |
|--------|----------------|---------------------|
| **UserContext** | Java 21 Record | Java 21 Record ✅ |
| **KeycloakUserContext** | @ApplicationScoped | @ApplicationScoped ✅ |
| **AuthController/AuthTestController** | Endpoints de teste | Endpoints de teste ✅ |
| **Constructor Injection** | Sim | Sim ✅ |
| **Optional Pattern** | getCurrentUser() → Optional | getCurrentUser() → Optional ✅ |

### 🧪 Scripts de Teste

| Script | Workflow-Engine | Notification-Engine |
|--------|----------------|---------------------|
| **test-keycloak.sh** | ✅ Criado | ✅ Criado |
| **Obtém Token** | ✅ | ✅ |
| **Testa /api/auth/me** | ✅ | ✅ |
| **Testa /health** | ✅ | ✅ |
| **Testa admin-only** | ✅ | ✅ |
| **Decodifica JWT** | ✅ | ✅ |

### 📦 Dependências Maven (Comuns)

- ✅ `quarkus-oidc` - Autenticação Keycloak
- ✅ `quarkus-smallrye-jwt` - JWT handling
- ✅ `quarkus-jdbc-postgresql` - PostgreSQL driver
- ✅ `quarkus-hibernate-orm` - ORM
- ✅ `quarkus-rest-jackson` - REST + JSON
- ✅ `quarkus-smallrye-health` - Health checks

### 🎯 Endpoints Públicos vs Autenticados

#### Workflow-Engine
```properties
# Públicos
/q/health/*, /q/metrics

# Autenticados
/* (todos os demais)
```

#### Notification-Engine
```properties
# Públicos
/q/health/*, /q/metrics, /api/auth/health, /swagger-ui/*, /openapi

# Autenticados
/* (todos os demais)
```

### ⏭️ Próximos Passos

1. **Criar Client no Keycloak** ⏳
   - Client ID: `notification-engine`
   - Type: confidential
   - URL: `http://localhost:8082`

2. **Configurar Secret** ⏳
   ```bash
   # Atualizar .envrc
   export OIDC_CLIENT_SECRET="<secret_gerado>"
   direnv allow
   ```

3. **Testar Notification-Engine** ⏳
   ```bash
   cd /home/joaopedro/notification-engine
   ./mvnw clean quarkus:dev
   ./test-keycloak.sh
   ```

4. **Adicionar @Authenticated aos Controllers** ⏳
   - NotificationController
   - AdminNotificationController
   - Outros endpoints sensíveis

### 🎉 Benefícios da Padronização

1. ✅ **Configuração Unificada** - Mesmo formato (.properties)
2. ✅ **Banco Compartilhado** - Supabase PostgreSQL (economia)
3. ✅ **SSO Completo** - Keycloak realm development
4. ✅ **Mesmos Padrões** - Java 21, Records, Constructor Injection
5. ✅ **Logs Consistentes** - Mesmo formato e níveis
6. ✅ **Scripts Reutilizáveis** - test-keycloak.sh
7. ✅ **Fácil Manutenção** - Estrutura similar
8. ✅ **IPv6 Ready** - Cloudflare WARP

### 📊 Status de Compilação

| Projeto | Status | Comando |
|---------|--------|---------|
| **workflow-engine** | ✅ BUILD SUCCESS | `./mvnw clean compile` |
| **notification-engine** | ✅ BUILD SUCCESS | `./mvnw clean compile` |

---

**Data:** 16 de Janeiro de 2026  
**Versão:** 1.0.0-SNAPSHOT  
**Framework:** Quarkus 3.30.x  
**Java:** 21
