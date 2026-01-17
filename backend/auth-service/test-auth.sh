#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8084"

echo -e "${BLUE}🧪 Auth Service Testing Script${NC}\n"

# Test 1: Health Check
echo -e "${BLUE}1️⃣ Testing Health Endpoint...${NC}"
curl -s $BASE_URL/actuator/health | jq
echo -e "\n"

# Test 2: Login
echo -e "${BLUE}2️⃣ Testing Login...${NC}"
LOGIN_RESPONSE=$(curl -s -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"admin123"}')

echo "$LOGIN_RESPONSE" | jq

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token')

if [ "$TOKEN" = "null" ] || [ -z "$TOKEN" ]; then
    echo -e "${RED}❌ Login failed! Token not received.${NC}"
    exit 1
fi

echo -e "${GREEN}✅ Login successful!${NC}"
echo -e "Token: ${TOKEN:0:50}...\n"

# Test 3: Get Current User
echo -e "${BLUE}3️⃣ Testing /auth/me...${NC}"
curl -s -X GET $BASE_URL/auth/me \
  -H "Authorization: Bearer $TOKEN" | jq
echo -e "\n"

# Test 4: Validate Token (existing endpoint)
echo -e "${BLUE}4️⃣ Testing Token Validation (existing)...${NC}"
curl -s -X POST $BASE_URL/auth/validate \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}" | jq
echo -e "\n"

# Test 5: Introspect Token (existing endpoint)
echo -e "${BLUE}5️⃣ Testing Token Introspection (existing)...${NC}"
curl -s -X POST $BASE_URL/auth/introspect \
  -H "Content-Type: application/json" \
  -d "{\"token\":\"$TOKEN\"}" | jq
echo -e "\n"

# Test 6: Invalid Login
echo -e "${BLUE}6️⃣ Testing Invalid Login...${NC}"
curl -s -w "\nHTTP Status: %{http_code}\n" -X POST $BASE_URL/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@demo.com","password":"wrongpassword"}'
echo -e "\n"

# Test 7: /auth/me without token
echo -e "${BLUE}7️⃣ Testing /auth/me without token...${NC}"
curl -s -w "\nHTTP Status: %{http_code}\n" -X GET $BASE_URL/auth/me
echo -e "\n"

echo -e "${GREEN}✅ All tests completed!${NC}"
