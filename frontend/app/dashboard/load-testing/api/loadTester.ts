// Load Tester Service API Client

import {
  LoadTestRequest,
  StartTestResponse,
  LoadTestStatus,
  LoadTestResult,
  TestListItem,
  HealthStatus,
} from '../types/loadTester.types';

const API_BASE = "/api/load-test";

export async function startLoadTest(request: LoadTestRequest): Promise<StartTestResponse> {
  const response = await fetch(`${API_BASE}/start`, {
    method: "POST",
    headers: { 
      "Content-Type": "application/json",
    },
    body: JSON.stringify(request),
  });
  
  if (!response.ok) {
    const error = await response.text();
    throw new Error(error || "Failed to start test");
  }
  
  return response.json();
}

export async function getTestStatus(testId: string): Promise<LoadTestStatus> {
  const response = await fetch(`${API_BASE}/status/${testId}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error("Failed to fetch status");
  }
  
  return response.json();
}

export async function getTestResult(testId: string): Promise<LoadTestResult> {
  const response = await fetch(`${API_BASE}/result/${testId}`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error("Failed to fetch results");
  }
  
  return response.json();
}

export async function stopTest(testId: string): Promise<void> {
  const response = await fetch(`${API_BASE}/stop/${testId}`, {
    method: "DELETE",
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error("Failed to stop test");
  }
}

export async function listTests(): Promise<TestListItem[]> {
  const response = await fetch(`${API_BASE}/list`, {
    method: 'GET',
    headers: { 'Content-Type': 'application/json' },
  });
  
  if (!response.ok) {
    throw new Error("Failed to fetch tests");
  }
  
  return response.json();
}

export async function checkHealth(): Promise<HealthStatus> {
  try {
    const response = await fetch(`${API_BASE}/health`, {
      method: 'GET',
      headers: { 'Content-Type': 'application/json' },
      cache: 'no-store',
    });
    
    if (!response.ok) {
      console.error('Health check failed:', response.status, response.statusText);
      return { status: "offline" };
    }
    
    const data = await response.json();
    console.log('Health check response:', data);
    
    // Handle different response formats
    const status = data.status || data.Status || "UP";
    console.log('Parsed health status:', status);
    
    return { status };
  } catch (error) {
    console.error('Health check error:', error);
    return { status: "offline" };
  }
}
