# 🔐 Guia de Segurança - Notification Engine

## 📋 Roles Necessárias no Keycloak

Para o correto funcionamento do sistema, crie as seguintes roles no realm `development`:

### Roles do Notification Engine

1. **notification-admin**
   - Acesso completo a todos os endpoints
   - Pode visualizar, criar, editar e deletar notificações
   - Pode acessar estatísticas e retry de notificações
   - Pode acessar notificações de qualquer usuário

2. **notification-sender**
   - Pode enviar notificações (endpoints POST)
   - Pode visualizar status de notificações
   - Acesso restrito às próprias notificações

3. **notification-viewer**
   - Somente leitura
   - Pode visualizar status de notificações
   - Acesso restrito às próprias notificações

## 🛡️ Endpoints Protegidos

### Públicos (sem autenticação)
- `GET /api/notifications` - Health check

### Autenticados (requer token válido)
Todos os demais endpoints requerem autenticação via Bearer token.

### Por Role

#### Somente Admin (`notification-admin`, `system-admin`)
- `DELETE /api/admin/notifications/{id}` - Deletar notificação

#### Admin e Sender (`notification-admin`, `notification-sender`, `system-admin`)
- `POST /api/notifications/from-workflow` - Processar notificação de workflow
- `POST /api/notifications/send` - Enviar notificação manual
- `POST /api/notifications/batch` - Enviar lote de notificações

#### Admin, Sender e Viewer (`notification-admin`, `notification-sender`, `notification-viewer`, `system-admin`)
- `GET /api/notifications/status/{id}` - Ver status de notificação
- `GET /api/notifications/user/{userId}` - Listar notificações do usuário
- `GET /api/admin/notifications` - Listar todas notificações (admin vê todas)
- `GET /api/admin/notifications/{id}` - Buscar notificação específica
- `POST /api/admin/notifications/{id}/retry` - Tentar reenviar notificação com erro
- `GET /api/admin/notifications/stats` - Estatísticas
- `GET /api/admin/notifications/health` - Health check admin

## 🔑 Configuração de Segurança

### 1. Criar as roles no Keycloak

Acesse: `https://auth.apporte.work/admin/development/console`

1. Vá em **Realm roles**
2. Clique em **Create role**
3. Crie as 3 roles listadas acima

### 2. Atribuir roles aos usuários

1. Vá em **Users**
2. Selecione o usuário (ex: `admin@example.com`)
3. Aba **Role mapping**
4. Clique em **Assign role**
5. Selecione as roles desejadas

Exemplo de atribuição:
- `admin@example.com` → `notification-admin`, `system-admin`
- `sender@example.com` → `notification-sender`
- `viewer@example.com` → `notification-viewer`

### 3. Variáveis de ambiente sensíveis

As seguintes variáveis **NUNCA** devem ser commitadas no git:

```bash
# Database
DB_PASSWORD='...'

# Keycloak
OIDC_CLIENT_SECRET='...'
KEYCLOAK_ADMIN_PASSWORD='...'

# Email (se usado)
SMTP_PASSWORD='...'
```

**Solução implementada:**
- Arquivo `.envrc` está no `.gitignore`
- Arquivo `.env.example` criado como template
- Arquivo `.token` (do test-keycloak.sh) está no `.gitignore`

### 4. Como configurar ambiente local

```bash
# 1. Copie o template
cp .env.example .envrc

# 2. Edite com valores reais
nano .envrc

# 3. Carregue as variáveis (se usar direnv)
direnv allow

# 4. Ou exporte manualmente
source .envrc
```

## 🧪 Testando a segurança

### 1. Obter token
```bash
./test-keycloak.sh
```

### 2. Usar token nos requests
```bash
export TOKEN=$(cat .token)

# Endpoint público (sem token)
curl http://localhost:8082/api/notifications

# Endpoint autenticado
curl -H "Authorization: Bearer $TOKEN" \
     http://localhost:8082/api/notifications/status/123

# Endpoint admin-only (requer notification-admin role)
curl -H "Authorization: Bearer $TOKEN" \
     -X DELETE \
     http://localhost:8082/api/admin/notifications/123
```

### 3. Verificar logs
Os logs agora mostram o email do usuário que fez a requisição:
```
INFO  Manual notification: task_assigned to user123 from user: admin@example.com
```

## 📊 Auditoria

Todos os endpoints protegidos agora registram:
- Quem (email do usuário)
- O que (operação realizada)
- Quando (timestamp do log)

Exemplo de log:
```
2026-01-16 13:17:53 INFO  [com.apporte.api.NotificationController] 
Manual notification: task_assigned to user123 from user: admin@example.com
```

## ⚠️ Avisos de Segurança

1. **Proteção de dados pessoais:** Endpoint `GET /api/notifications/user/{userId}` valida que usuários não-admin só podem ver suas próprias notificações.

2. **Segregação de responsabilidades:** Cada serviço tem suas próprias roles (`notification-admin` ≠ `workflow-admin`).

3. **Princípio do menor privilégio:** Atribua apenas as roles necessárias para cada usuário.

4. **Rotação de secrets:** Em produção, use secrets manager (AWS Secrets, Azure Key Vault, etc.).

5. **HTTPS obrigatório:** Em produção, configure `quarkus.oidc.tls.verification=required`.
