#!/bin/bash

# Test Analytics Service Integration
# This script tests all the Analytics API endpoints used by the dashboard

echo "🧪 Testing Analytics Service API Endpoints"
echo "=========================================="
echo ""

ANALYTICS_URL="http://localhost:8085/analytics"

# Color codes
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test function
test_endpoint() {
    local endpoint=$1
    local name=$2
    
    echo -n "Testing $name... "
    
    response=$(curl -s -w "\n%{http_code}" "$ANALYTICS_URL$endpoint" 2>&1)
    http_code=$(echo "$response" | tail -n1)
    body=$(echo "$response" | sed '$d')
    
    if [ "$http_code" -eq 200 ]; then
        echo -e "${GREEN}✓ PASS${NC} (HTTP $http_code)"
        echo "   Response: $(echo $body | jq -c . 2>/dev/null || echo $body)"
    else
        echo -e "${RED}✗ FAIL${NC} (HTTP $http_code)"
        echo "   Error: $body"
    fi
    echo ""
}

# Check if Analytics Service is running
echo "🔍 Checking if Analytics Service is running..."
if curl -s http://localhost:8085/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ Analytics Service is running${NC}"
    echo ""
else
    echo -e "${RED}✗ Analytics Service is NOT running on port 8085${NC}"
    echo ""
    echo "Please start the Analytics Service first:"
    echo "  cd backend/Analytics-service"
    echo "  ./mvnw spring-boot:run"
    echo ""
    exit 1
fi

# Test all endpoints
echo "📊 Testing Dashboard API Endpoints:"
echo "-----------------------------------"
echo ""

test_endpoint "/dashboard/metrics" "Dashboard Metrics"
test_endpoint "/dashboard/violations/recent?limit=10&page=0" "Recent Violations"
test_endpoint "/dashboard/violations/today/count" "Violations Today Count"
test_endpoint "/dashboard/requests/blocked/count" "Blocked Requests Count"
test_endpoint "/dashboard/latency/average" "Average Latency"

echo ""
echo "=========================================="
echo "✅ All tests completed!"
echo ""
echo "🚀 You can now access the dashboard at:"
echo "   http://localhost:3001/dashboard/logs"
echo ""
