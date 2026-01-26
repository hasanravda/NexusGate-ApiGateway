#!/bin/bash

echo "=== Testing NexusGate API Gateway ==="
echo ""

echo "1. Testing Gateway Health:"
curl -s http://localhost:8081/actuator/health | jq .
echo ""

echo "2. Testing without API Key (should fail with 401):"
curl -s http://localhost:8081/api/users
echo ""
echo ""

echo "3. Testing with valid API Key - Users endpoint:"
curl -s -H "X-API-Key: nx_lendingkart_prod_abc123" http://localhost:8081/api/users | jq .
echo ""

echo "4. Testing with valid API Key - Orders endpoint:"
curl -s -H "X-API-Key: nx_paytm_prod_xyz789" http://localhost:8081/api/orders | jq .
echo ""

echo "5. Testing with valid API Key - Payments endpoint:"
curl -s -H "X-API-Key: nx_mobikwik_test_def456" http://localhost:8081/api/payments | jq .
echo ""

echo "6. Testing Backend Services Directly:"
echo "   Users:"
curl -s http://localhost:8091/users | jq '.[0]'
echo ""
echo "   Orders:"
curl -s http://localhost:8091/orders | jq '.[0]'
echo ""
echo "   Payments:"
curl -s http://localhost:8091/payments | jq '.[0]'
echo ""

echo "=== Test Complete ==="
