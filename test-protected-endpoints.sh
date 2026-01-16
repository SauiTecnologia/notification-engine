#!/bin/bash

# Script para testar endpoints protegidos do Notification Engine
# Uso: ./test-protected-endpoints.sh

set -euo pipefail

echo "=================================================="
echo "🔐 Testando Endpoints Protegidos"
echo "=================================================="
echo ""

# Carregar token
if [ ! -f .token ]; then
    echo "❌ Arquivo .token não encontrado!"
    echo "Execute primeiro: ./test-keycloak.sh"
    exit 1
fi

TOKEN=$(cat .token)
BASE_URL="http://localhost:8082"

# Cores para output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Função para testar endpoint
test_endpoint() {
    local method=$1
    local path=$2
    local description=$3
    local data=${4:-}
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "📌 $description"
    echo "   $method $path"
    echo ""
    
    if [ -n "$data" ]; then
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$data" \
            "$BASE_URL$path")
    else
        response=$(curl -s -w "\n%{http_code}" -X "$method" \
            -H "Authorization: Bearer $TOKEN" \
            "$BASE_URL$path")
    fi
    
    # Separar body e status code
    body=$(echo "$response" | head -n -1)
    status=$(echo "$response" | tail -n 1)
    
    if [ "$status" -ge 200 ] && [ "$status" -lt 300 ]; then
        echo -e "${GREEN}✅ Sucesso (HTTP $status)${NC}"
    elif [ "$status" -eq 403 ]; then
        echo -e "${YELLOW}⚠️  Acesso negado (HTTP 403) - Role necessária${NC}"
    elif [ "$status" -eq 401 ]; then
        echo -e "${RED}❌ Não autenticado (HTTP 401)${NC}"
    else
        echo -e "${RED}❌ Erro (HTTP $status)${NC}"
    fi
    
    echo ""
    echo "Response:"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
    echo ""
}

echo "1️⃣ Endpoint Público (sem autenticação)"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
response=$(curl -s -w "\n%{http_code}" "$BASE_URL/api/notifications")
body=$(echo "$response" | head -n -1)
status=$(echo "$response" | tail -n 1)
echo -e "${GREEN}✅ GET /api/notifications (HTTP $status)${NC}"
echo "$body" | jq '.'
echo ""

echo "2️⃣ Endpoints Autenticados - Viewer"
test_endpoint "GET" "/api/notifications/status/1" "Buscar status de notificação"
test_endpoint "GET" "/api/notifications/user/f1dc24f3-569a-4394-9e0a-4190fd7f7938" "Listar notificações do usuário (próprio ID)"

echo "3️⃣ Endpoints Autenticados - Sender"
test_endpoint "POST" "/api/notifications/send" "Enviar notificação manual" '{
  "recipientId": "user123",
  "eventType": "test_notification",
  "channel": "email",
  "context": {
    "message": "Teste de segurança"
  }
}'

echo "4️⃣ Endpoints Admin"
test_endpoint "GET" "/api/admin/notifications" "Listar todas notificações (paginado)"
test_endpoint "GET" "/api/admin/notifications/stats?days=7" "Estatísticas (últimos 7 dias)"

echo "5️⃣ Endpoints Admin - Delete (requer admin role)"
test_endpoint "DELETE" "/api/admin/notifications/999" "Deletar notificação (apenas admin)"

echo ""
echo "=================================================="
echo "✅ Testes concluídos!"
echo "=================================================="
echo ""
echo "📝 Notas:"
echo "  - ✅ HTTP 200-299: Sucesso"
echo "  - ⚠️  HTTP 403: Acesso negado (role insuficiente)"
echo "  - ❌ HTTP 401: Não autenticado (token inválido/expirado)"
echo ""
