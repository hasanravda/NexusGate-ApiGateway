const API_BASE_URL = 'http://localhost:8082';

async function handleResponse(response) {
  if (response.status === 204) {
    return null;
  }

  // Check if response is JSON
  const contentType = response.headers.get('content-type');
  const isJson = contentType && contentType.includes('application/json');
  
  let data;
  if (isJson) {
    data = await response.json();
  } else {
    // If not JSON, get text (for CORS errors, etc.)
    const text = await response.text();
    data = { message: text };
  }

  if (!response.ok) {
    throw new Error(data.message || 'API request failed');
  }

  return data;
}

export const serviceRoutesApi = {
  getAll: async (activeOnly = false) => {
    const url = activeOnly
      ? `${API_BASE_URL}/service-routes?activeOnly=true`
      : `${API_BASE_URL}/service-routes`;
    const response = await fetch(url, { cache: 'no-store' });
    return handleResponse(response);
  },

  getById: async (id) => {
    const response = await fetch(`${API_BASE_URL}/service-routes/${id}`, { cache: 'no-store' });
    return handleResponse(response);
  },

  create: async (data) => {
    const response = await fetch(`${API_BASE_URL}/service-routes`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  update: async (id, data) => {
    const response = await fetch(`${API_BASE_URL}/service-routes/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  toggle: async (id) => {
    const response = await fetch(`${API_BASE_URL}/service-routes/${id}/toggle`, {
      method: 'PATCH',
      headers: { 
        'Content-Type': 'application/json',
      },
    });
    return handleResponse(response);
  },

  updateSecurity: async (id, requiresApiKey) => {
    const response = await fetch(`/api/service-routes/${id}/security`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ requiresApiKey }),
    });
    return handleResponse(response);
  },

  delete: async (id) => {
    const response = await fetch(`${API_BASE_URL}/service-routes/${id}`, {
      method: 'DELETE',
    });
    return handleResponse(response);
  },
};

export const apiKeysApi = {
  getAll: async () => {
    const response = await fetch(`${API_BASE_URL}/api/keys`, { cache: 'no-store' });
    return handleResponse(response);
  },

  getById: async (id) => {
    const response = await fetch(`${API_BASE_URL}/api/keys/${id}`, { cache: 'no-store' });
    return handleResponse(response);
  },

  create: async (data) => {
    const response = await fetch(`${API_BASE_URL}/api/keys`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  update: async (id, data) => {
    const response = await fetch(`${API_BASE_URL}/api/keys/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  revoke: async (id) => {
    const response = await fetch(`${API_BASE_URL}/api/keys/${id}`, {
      method: 'DELETE',
    });
    return handleResponse(response);
  },
};

export const rateLimitsApi = {
  getAll: async () => {
    const response = await fetch(`${API_BASE_URL}/rate-limits`, { cache: 'no-store' });
    return handleResponse(response);
  },

  getById: async (id) => {
    const response = await fetch(`${API_BASE_URL}/rate-limits/${id}`, { cache: 'no-store' });
    return handleResponse(response);
  },

  create: async (data) => {
    const response = await fetch(`${API_BASE_URL}/rate-limits`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  update: async (id, data) => {
    const response = await fetch(`${API_BASE_URL}/rate-limits/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse(response);
  },

  toggle: async (id) => {
    const response = await fetch(`${API_BASE_URL}/rate-limits/${id}/toggle`, {
      method: 'PATCH',
      headers: { 
        'Content-Type': 'application/json',
      },
    });
    return handleResponse(response);
  },

  delete: async (id) => {
    const response = await fetch(`${API_BASE_URL}/rate-limits/${id}`, {
      method: 'DELETE',
    });
    return handleResponse(response);
  },
};
