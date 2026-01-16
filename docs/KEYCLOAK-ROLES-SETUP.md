# 🔑 Guia Rápido: Configurar Roles no Keycloak

## 📋 Pré-requisitos

- Acesso ao Keycloak Admin Console
- URL: `https://auth.apporte.work/admin/development/console`
- Credenciais de admin

## 🎯 Roles a Criar

### System-wide (já criada)
- ✅ `system-admin` - Acesso completo a todos os sistemas

### Workflow Engine
- `workflow-admin` - Administrador de workflows
- `workflow-user` - Usuário normal de workflows
- `workflow-viewer` - Visualização apenas

### Notification Engine
- `notification-admin` - Administrador de notificações
- `notification-sender` - Pode enviar notificações
- `notification-viewer` - Visualização apenas

## 🔧 Passos para Criar Roles

### 1. Acessar Realm Roles

```
1. Login no Keycloak Admin Console
2. Selecione o realm: "development"
3. Menu lateral → "Realm roles"
4. Clique no botão "Create role"
```

### 2. Criar Role: workflow-admin

```
Role name: workflow-admin
Description: Full access to workflow management - can create, edit, delete workflows and view all executions

Attributes (opcional):
  - type: admin
  - service: workflow-engine
```

Clique em **Save**

### 3. Criar Role: workflow-user

```
Role name: workflow-user
Description: Standard workflow user - can create and execute own workflows

Attributes (opcional):
  - type: user
  - service: workflow-engine
```

Clique em **Save**

### 4. Criar Role: workflow-viewer

```
Role name: workflow-viewer
Description: Read-only access to workflows

Attributes (opcional):
  - type: viewer
  - service: workflow-engine
```

Clique em **Save**

### 5. Criar Role: notification-admin

```
Role name: notification-admin
Description: Full access to notification management - can view, send, retry and delete notifications

Attributes (opcional):
  - type: admin
  - service: notification-engine
```

Clique em **Save**

### 6. Criar Role: notification-sender

```
Role name: notification-sender
Description: Can send notifications and view own notification history

Attributes (opcional):
  - type: sender
  - service: notification-engine
```

Clique em **Save**

### 7. Criar Role: notification-viewer

```
Role name: notification-viewer
Description: Read-only access to notifications

Attributes (opcional):
  - type: viewer
  - service: notification-engine
```

Clique em **Save**

## 👥 Atribuir Roles aos Usuários

### Usuário: admin@example.com (Super Admin)

```
1. Menu lateral → "Users"
2. Busque por: admin@example.com
3. Clique no usuário
4. Aba "Role mapping"
5. Botão "Assign role"
6. Selecione:
   ☑ system-admin
   ☑ workflow-admin
   ☑ notification-admin
7. Clique em "Assign"
```

### Usuário: developer@example.com (Desenvolvedor)

```
1. Menu lateral → "Users"
2. Busque por: developer@example.com
3. Clique no usuário
4. Aba "Role mapping"
5. Botão "Assign role"
6. Selecione:
   ☑ workflow-user
   ☑ notification-sender
7. Clique em "Assign"
```

### Usuário: viewer@example.com (Visualizador)

```
1. Menu lateral → "Users"
2. Busque por: viewer@example.com
3. Clique no usuário
4. Aba "Role mapping"
5. Botão "Assign role"
6. Selecione:
   ☑ workflow-viewer
   ☑ notification-viewer
7. Clique em "Assign"
```

## ✅ Verificar Configuração

### Verificar Roles Criadas

```bash
# Via Keycloak Admin API
curl -X GET "https://auth.apporte.work/admin/realms/development/roles" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  | jq '.[] | select(.name | contains("workflow") or contains("notification")) | {name, description}'
```

### Verificar Roles do Usuário

```bash
# 1. Obter token do usuário
./test-keycloak.sh

# 2. Decodificar JWT e ver roles
cat .token | cut -d. -f2 | base64 -d 2>/dev/null | jq '.realm_access.roles'
```

## 🧪 Testar Permissões

### Notification Engine

```bash
cd /home/joaopedro/notification-engine

# 1. Obter token
./test-keycloak.sh

# 2. Testar todos endpoints
./test-protected-endpoints.sh
```

### Workflow Engine

```bash
cd /home/joaopedro/workflow-engine

# 1. Obter token (crie script similar ao notification-engine)
# 2. Testar endpoints
curl -H "Authorization: Bearer $(cat .token)" \
     http://localhost:8080/api/auth/me
```

## 📊 Matriz de Permissões

| Role | Workflow Engine | Notification Engine |
|------|----------------|---------------------|
| `system-admin` | ✅ Todos endpoints | ✅ Todos endpoints |
| `workflow-admin` | ✅ Criar/Editar/Deletar/Executar | ❌ Sem acesso |
| `workflow-user` | ✅ Criar/Executar próprios | ❌ Sem acesso |
| `workflow-viewer` | 👁️ Visualizar apenas | ❌ Sem acesso |
| `notification-admin` | ❌ Sem acesso | ✅ Enviar/Retry/Deletar/Ver todas |
| `notification-sender` | ❌ Sem acesso | ✅ Enviar/Ver próprias |
| `notification-viewer` | ❌ Sem acesso | 👁️ Ver próprias apenas |

## 🔄 Roles Compostas (Opcional)

Para simplificar, você pode criar roles compostas:

### Role: developer (Composite)

```
1. Create role → "developer"
2. Aba "Associated roles"
3. Adicione:
   - workflow-user
   - notification-sender
```

Agora ao atribuir `developer` ao usuário, ele automaticamente recebe ambas as roles.

### Role: admin (Composite)

```
1. Create role → "admin"
2. Aba "Associated roles"
3. Adicione:
   - system-admin
   - workflow-admin
   - notification-admin
```

## ⚠️ Dicas Importantes

1. **Sempre use HTTPS em produção** - Tokens são Bearer tokens, podem ser interceptados em HTTP.

2. **Configure Token Expiration:**
   - Access Token: 5-15 minutos
   - Refresh Token: 30-60 minutos
   - SSO Session: 8-24 horas

3. **Ative logs de auditoria no Keycloak:**
   ```
   Realm Settings → Events → Save Events: ON
   Login Events: ON
   Admin Events: ON
   ```

4. **Backup das roles:**
   - Export realm: `Realm Settings → Partial export`
   - Salve o JSON em local seguro

5. **Documentação:**
   - Mantenha `SECURITY.md` atualizado em cada serviço
   - Documente mudanças de permissões

## 🆘 Troubleshooting

### Erro: "Access Denied (HTTP 403)"

1. Verifique se o usuário tem a role necessária:
   ```bash
   cat .token | cut -d. -f2 | base64 -d 2>/dev/null | jq '.realm_access.roles'
   ```

2. Verifique se a role está corretamente mapeada no código:
   ```java
   @RolesAllowed({"workflow-admin", "system-admin"})
   ```

3. Limpe o cache do Keycloak e force novo login

### Token expirado

```bash
# Obter novo token
./test-keycloak.sh

# Verificar expiração
cat .token | cut -d. -f2 | base64 -d 2>/dev/null | jq '{exp: .exp, iat: .iat, now: now}'
```

### Role não aparece no token

1. Verifique se a role foi atribuída ao usuário
2. Force logout/login no Keycloak
3. Obtenha novo token

## 📚 Referências

- [Keycloak Admin REST API](https://www.keycloak.org/docs-api/latest/rest-api/index.html)
- [Quarkus OIDC Security](https://quarkus.io/guides/security-oidc-bearer-token-authentication)
- [JWT.io](https://jwt.io/) - Decodificador de JWT online

---

**Última atualização:** 2026-01-16  
**Autor:** Equipe Apporte  
**Versão:** 1.0
