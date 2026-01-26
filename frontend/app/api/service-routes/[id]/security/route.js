import { NextResponse } from 'next/server';

export async function PATCH(request, { params }) {
  try {
    const { id } = params;
    const body = await request.json();
    
    console.log('API Route - Updating security for service:', id, body);
    
    const backendUrl = `http://localhost:8082/service-routes/${id}/security`;
    console.log('Calling backend:', backendUrl);
    
    const response = await fetch(backendUrl, {
      method: 'PATCH',
      headers: { 
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      body: JSON.stringify(body),
    });

    console.log('Backend response status:', response.status);

    if (!response.ok) {
      const errorText = await response.text();
      console.error('Backend error:', errorText);
      return NextResponse.json(
        { message: errorText || 'Failed to update security' },
        { status: response.status }
      );
    }

    const data = await response.json();
    console.log('Success:', data);
    return NextResponse.json(data);
    
  } catch (error) {
    console.error('API Route error:', error);
    return NextResponse.json(
      { message: error.message || 'Internal server error' },
      { status: 500 }
    );
  }
}
