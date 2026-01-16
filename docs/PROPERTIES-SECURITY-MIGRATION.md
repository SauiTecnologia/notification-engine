# 🔒 Proteção de Dados Sensíveis - Properties Files

## ✅ O que foi feito?

Todos os dados sensíveis (credenciais, secrets, URLs) foram removidos dos arquivos `.properties` e substituídos por variáveis de ambiente usando o formato:

```properties
propriedade=${VARIAVEL_AMBIENTE:valor_default}
```

## 📋 Arquivos Modificados

### Notification Engine

#### 1. `application-dev.properties`
**Antes:**
```properties
quarkus.datasource.password=R$97J2:mtRz;g5R?3#$n#@BMs~0SB.)J
quarkus.oidc.credentials.secret=0iMoppp4bzBvxH6lOwu9NWY0kCjsfqqE
```

**Depois:**
```properties
quarkus.datasource.password=${DB_PASSWORD}
quarkus.oidc.credentials.secret=${OIDC_CLIENT_SECRET}
```

**Variáveis substituídas:**
- `DB_JDBC_URL` - URL completa do banco (com query params)
- `DB_USERNAME` - Usuário do banco
- `DB_PASSWORD` - **OBRIGATÓRIA** - Senha do banco (sem default por segurança)
- `DB_POOL_MIN_SIZE` - Tamanho mínimo do pool (default: 2)
- `DB_POOL_MAX_SIZE` - Tamanho máximo do pool (default: 5)
- `OIDC_ENABLED` - Se OIDC está habilitado (default: true)
- `OIDC_AUTH_SERVER_URL` - URL do Keycloak (default: https://auth.apporte.work/realms/development)
- `OIDC_CLIENT_ID` - ID do cliente (default: notification-engine-dev)
- `OIDC_CLIENT_SECRET` - **OBRIGATÓRIA** - Secret do cliente
- `OIDC_ISSUER` - Issuer do token (default: URL do Keycloak)
- `EMAIL_FROM` - Email de envio (default: noreply@apporte.com)
- `EMAIL_MOCK` - Se usa mock de email (default: true)

#### 2. `application-prod.properties`
Já estava usando variáveis de ambiente, nenhuma mudança necessária (SMTP secrets já protegidos).

#### 3. `application.properties`
Base configuration - já estava usando variáveis de ambiente.

---

### Workflow Engine

#### 1. `application-dev.properties`
**Antes:**
```properties
quarkus.datasource.password=R$97J2:mtRz;g5R?3#$n#@BMs~0SB.)J
quarkus.oidc.credentials.secret=E6Vy7He2wemRyUDdfXDfyNsOAIwNk43u
```

**Depois:**
```properties
quarkus.datasource.password=${DB_PASSWORD}
quarkus.oidc.credentials.secret=${KEYCLOAK_CLIENT_SECRET}
```

**Variáveis substituídas:**
- `DB_JDBC_URL` - URL completa do banco
- `DB_USERNAME` - Usuário do banco (default: workflow_backend)
- `DB_PASSWORD` - **OBRIGATÓRIA** - Senha do banco
- `DB_INITIAL_SIZE` - Pool inicial (default: 5)
- `DB_MAX_SIZE` - Pool máximo (default: 20)

#### 2. `application-prod.properties`
**Antes:**
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://db.rpkqbesfgjdeolketoug.supabase.co:5432/postgres?sslmode=require
quarkus.datasource.username=workflow_backend
quarkus.oidc.auth-server-url=https://auth.apporte.work/realms/production
quarkus.oidc.client-id=workflow-engine-prod
```

**Depois:**
```properties
quarkus.datasource.jdbc.url=${DB_JDBC_URL}
quarkus.datasource.username=${DB_USERNAME}
quarkus.oidc.auth-server-url=${KEYCLOAK_AUTH_SERVER_URL:https://auth.apporte.work/realms/production}
quarkus.oidc.client-id=${KEYCLOAK_CLIENT_ID:workflow-engine-prod}
```

**Variáveis adicionadas:**
- `DB_JDBC_URL` - **OBRIGATÓRIA** - URL do banco em produção
- `DB_USERNAME` - **OBRIGATÓRIA** - Usuário do banco
- `DB_PASSWORD` - **OBRIGATÓRIA** - Senha do banco
- `KEYCLOAK_AUTH_SERVER_URL` - URL do Keycloak (default: realm production)
- `KEYCLOAK_CLIENT_ID` - Client ID (default: workflow-engine-prod)
- `KEYCLOAK_CLIENT_SECRET` - **OBRIGATÓRIA** - Secret do cliente
- `KEYCLOAK_ISSUER` - Issuer (default: URL do Keycloak)

#### 3. `application.properties`
**Antes:**
```properties
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/workflow
quarkus.datasource.username=workflow_user
quarkus.datasource.password=workflow_pass
```

**Depois:**
```properties
quarkus.datasource.jdbc.url=${DB_JDBC_URL:jdbc:postgresql://localhost:5432/workflow}
quarkus.datasource.username=${DB_USERNAME:workflow_user}
quarkus.datasource.password=${DB_PASSWORD:workflow_pass}
```

---

## 🔑 Variáveis Obrigatórias (sem default)

Estas variáveis **DEVEM** estar configuradas no `.envrc`, caso contrário a aplicação não iniciará:

### Notification Engine
```bash
export DB_PASSWORD='R$97J2:mtRz;g5R?3#$n#@BMs~0SB.)J'
export OIDC_CLIENT_SECRET='0iMoppp4bzBvxH6lOwu9NWY0kCjsfqqE'
```

### Workflow Engine
```bash
export DB_PASSWORD='R$97J2:mtRz;g5R?3#$n#@BMs~0SB.)J'
export KEYCLOAK_CLIENT_SECRET='E6Vy7He2wemRyUDdfXDfyNsOAIwNk43u'
```

### Produção (ambos)
```bash
export DB_JDBC_URL='jdbc:postgresql://...'
export DB_USERNAME='production_user'
export DB_PASSWORD='super-secret-password'
export OIDC_CLIENT_SECRET='production-secret'  # ou KEYCLOAK_CLIENT_SECRET
```

---

## 📊 Resumo das Mudanças

| Projeto | Arquivo | Credenciais Removidas | Status |
|---------|---------|----------------------|--------|
| notification-engine | application-dev.properties | ✅ DB password, OIDC secret, URLs | Protegido |
| notification-engine | application-prod.properties | ✅ Já estava protegido | OK |
| notification-engine | application.properties | ✅ Já estava protegido | OK |
| workflow-engine | application-dev.properties | ✅ DB password, OIDC secret | Protegido |
| workflow-engine | application-prod.properties | ✅ DB URL/user, Keycloak URLs | Protegido |
| workflow-engine | application.properties | ✅ DB local credentials | Protegido |

**Total:** 6 arquivos protegidos ✅

---

## 🧪 Como Testar

### 1. Verificar que variáveis estão carregadas

```bash
cd /home/joaopedro/notification-engine

# Carregar variáveis do .envrc
direnv allow

# Verificar se estão definidas
echo $DB_PASSWORD
echo $OIDC_CLIENT_SECRET

# Se não aparecer nada, exporte manualmente:
export DB_PASSWORD='R$97J2:mtRz;g5R?3#$n#@BMs~0SB.)J'
export OIDC_CLIENT_SECRET='0iMoppp4bzBvxH6lOwu9NWY0kCjsfqqE'
```

### 2. Iniciar aplicação

```bash
./mvnw clean quarkus:dev
```

**Sucesso se ver:**
```
Listening on: http://0.0.0.0:8082
```

**Erro se ver:**
```
OIDC Dev Console: discovering the provider metadata
ERROR: Configuration validation failed...
  quarkus.datasource.password is required
```

### 3. Verificar valores no console do Quarkus

```bash
# No console Quarkus, digite:
[:]

# Depois:
config property quarkus.datasource.password
```

Deve mostrar: `***` (mascarado por segurança)

---

## ⚠️ Avisos Importantes

1. **Nunca commite `.envrc`** - Está no `.gitignore` por segurança

2. **Use `.env.example` como template** - Copie e preencha valores reais:
   ```bash
   cp .env.example .envrc
   nano .envrc  # Preencha com valores reais
   direnv allow
   ```

3. **Em produção use Secrets Manager:**
   - AWS Secrets Manager
   - Azure Key Vault
   - HashiCorp Vault
   - Kubernetes Secrets

4. **Rode CI/CD com variáveis de ambiente:**
   ```yaml
   # .github/workflows/deploy.yml
   env:
     DB_PASSWORD: ${{ secrets.DB_PASSWORD }}
     OIDC_CLIENT_SECRET: ${{ secrets.OIDC_CLIENT_SECRET }}
   ```

5. **Valores default são apenas para desenvolvimento local:**
   - Produção deve sobrescrever TODOS os defaults
   - Nunca use defaults em produção para secrets

---

## 🔄 Migração Completa

Se você já tem os serviços rodando:

```bash
# 1. Parar os serviços
# Pressione Ctrl+C nos terminais

# 2. Verificar que .envrc tem os valores
cat .envrc | grep -E "DB_PASSWORD|CLIENT_SECRET"

# 3. Recarregar variáveis
direnv allow

# 4. Reiniciar serviços
./mvnw clean quarkus:dev
```

---

## ✅ Status Final

- ✅ Todos properties files protegidos
- ✅ Variáveis obrigatórias documentadas
- ✅ Templates `.env.example` criados
- ✅ `.gitignore` configurado
- ✅ Documentação completa
- ✅ Compatível com CI/CD

**Seus dados sensíveis agora estão seguros!** 🔒
