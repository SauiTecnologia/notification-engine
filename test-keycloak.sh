#!/bin/bash
# ============================================
# Test Keycloak Authentication - Notification Engine
# ============================================

set -e  # Exit on any error

KEYCLOAK_URL="https://auth.apporte.work"
REALM="development"
CLIENT_ID="notification-engine-dev"
CLIENT_SECRET="0iMoppp4bzBvxH6lOwu9NWY0kCjsfqqE"
USERNAME="admin@example.com"
PASSWORD="senha123"

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================================="
echo "🔐 Testing Keycloak Authentication"
echo "=================================================="
echo ""
echo "Keycloak Server: $KEYCLOAK_URL"
echo "Realm: $REALM"
echo "Client ID: $CLIENT_ID"
echo "User: $USERNAME"
echo ""

# 1. Get Access Token
echo -e "${YELLOW}1️⃣ Obtaining access token...${NC}"
TOKEN_RESPONSE=$(curl -s -X POST \
  "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$CLIENT_ID" \
  -d "client_secret=$CLIENT_SECRET" \
  -d "username=$USERNAME" \
  -d "password=$PASSWORD" \
  -d "grant_type=password" \
  -d "scope=openid profile email")

# Check if token was obtained
if echo "$TOKEN_RESPONSE" | jq -e '.access_token' > /dev/null 2>&1; then
    ACCESS_TOKEN=$(echo "$TOKEN_RESPONSE" | jq -r '.access_token')
    echo -e "${GREEN}✅ Token obtained successfully!${NC}"
    echo "$ACCESS_TOKEN" > .token
    echo ""
    
    # Display token info
    echo -e "${YELLOW}📋 Token Info:${NC}"
    echo "$TOKEN_RESPONSE" | jq '{
        token_type,
        expires_in,
        refresh_expires_in,
        scope
    }'
    echo ""
    
    # Decode JWT payload
    echo -e "${YELLOW}🔍 JWT Payload (User Info):${NC}"
    PAYLOAD=$(echo "$ACCESS_TOKEN" | cut -d'.' -f2)
    # Add padding if needed
    PADDED_PAYLOAD=$(echo "$PAYLOAD" | awk '{while(length($0) % 4 != 0) $0 = $0"="; print}')
    echo "$PADDED_PAYLOAD" | base64 -d 2>/dev/null | jq '.'
    echo ""
    
else
    echo -e "${RED}❌ Failed to obtain token!${NC}"
    echo "Response:"
    echo "$TOKEN_RESPONSE" | jq '.'
    exit 1
fi

# 2. Test /api/auth/me endpoint
echo -e "${YELLOW}2️⃣ Testing /api/auth/me endpoint...${NC}"
AUTH_RESPONSE=$(curl -s -w "\n%{http_code}" \
  http://localhost:8082/api/auth/me \
  -H "Authorization: Bearer $ACCESS_TOKEN")

HTTP_CODE=$(echo "$AUTH_RESPONSE" | tail -n1)
RESPONSE_BODY=$(echo "$AUTH_RESPONSE" | sed '$d')

if [ "$HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Authentication successful! (HTTP $HTTP_CODE)${NC}"
    echo ""
    echo -e "${YELLOW}👤 User Context:${NC}"
    echo "$RESPONSE_BODY" | jq '.'
else
    echo -e "${RED}❌ Authentication failed! (HTTP $HTTP_CODE)${NC}"
    echo "Response:"
    echo "$RESPONSE_BODY"
    exit 1
fi

# 3. Test /api/auth/health endpoint (public)
echo ""
echo -e "${YELLOW}3️⃣ Testing /api/auth/health endpoint (public)...${NC}"
HEALTH_RESPONSE=$(curl -s -w "\n%{http_code}" http://localhost:8082/api/auth/health)
HEALTH_HTTP_CODE=$(echo "$HEALTH_RESPONSE" | tail -n1)
HEALTH_BODY=$(echo "$HEALTH_RESPONSE" | sed '$d')

if [ "$HEALTH_HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Health check OK! (HTTP $HEALTH_HTTP_CODE)${NC}"
    echo "Response: $HEALTH_BODY"
else
    echo -e "${RED}❌ Health check failed! (HTTP $HEALTH_HTTP_CODE)${NC}"
    echo "Response: $HEALTH_BODY"
fi

# 4. Test /api/auth/admin-only endpoint (requires notification-admin role)
echo ""
echo -e "${YELLOW}4️⃣ Testing /api/auth/admin-only endpoint...${NC}"
ADMIN_RESPONSE=$(curl -s -w "\n%{http_code}" \
  http://localhost:8082/api/auth/admin-only \
  -H "Authorization: Bearer $ACCESS_TOKEN")

ADMIN_HTTP_CODE=$(echo "$ADMIN_RESPONSE" | tail -n1)
ADMIN_BODY=$(echo "$ADMIN_RESPONSE" | sed '$d')

if [ "$ADMIN_HTTP_CODE" -eq 200 ]; then
    echo -e "${GREEN}✅ Admin access granted! (HTTP $ADMIN_HTTP_CODE)${NC}"
    echo "Response: $ADMIN_BODY"
elif [ "$ADMIN_HTTP_CODE" -eq 403 ]; then
    echo -e "${YELLOW}⚠️  Admin access denied (HTTP 403) - User doesn't have notification-admin role${NC}"
    echo "Response: $ADMIN_BODY"
else
    echo -e "${RED}❌ Unexpected response! (HTTP $ADMIN_HTTP_CODE)${NC}"
    echo "Response: $ADMIN_BODY"
fi

echo ""
echo "=================================================="
echo -e "${GREEN}✅ All tests completed!${NC}"
echo "=================================================="
echo ""
echo "Token saved to .token file"
echo "You can use it for manual testing:"
echo "  export TOKEN=\$(cat .token)"
echo "  curl -H \"Authorization: Bearer \$TOKEN\" http://localhost:8082/api/auth/me"
