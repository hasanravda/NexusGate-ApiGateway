# NexusGate Control Plane

A production-ready API Gateway management dashboard built with Next.js, designed to work seamlessly with your config-service backend.

## Features

### Overview Dashboard
- Real-time metrics display (requests/sec, latency, error rate)
- Traffic visualization with filtering options
- Top services by traffic
- Recent rate limit violations

### Gateway Services Management
- Full CRUD operations for service routes
- Configure public paths and target URLs
- Set allowed HTTP methods
- Define rate limits per service
- Enable/disable services with toggle
- View service details

### API Keys Management
- Create and manage client API keys
- Secure key display with show/hide toggle
- One-click copy to clipboard
- Set expiration dates
- Revoke keys when needed
- Track key usage

### Rate Limits Management
- Three types of rate limits:
  - **Specific**: API Key + Service Route combination
  - **Route Default**: Applies to all API keys for a service
  - **Key Global**: Applies to all services for an API key
- Configure limits per minute, hour, and day
- Visual priority hierarchy
- Enable/disable limits
- Smart priority resolution

### Logs & Violations
- Monitor rate limit violations (mock data for now)
- View blocked requests
- Track system activity

### Settings
- System information display
- Placeholder for future configuration options

## Backend Integration

The frontend integrates with your config-service on **http://localhost:8082**

### API Endpoints Used

#### Service Routes
- `GET /service-routes` - List all service routes
- `POST /service-routes` - Create new service route
- `PUT /service-routes/{id}` - Update service route
- `PATCH /service-routes/{id}/toggle` - Toggle active status
- `DELETE /service-routes/{id}` - Delete service route

#### API Keys
- `GET /api/keys` - List all API keys
- `POST /api/keys` - Create new API key
- `PUT /api/keys/{id}` - Update API key
- `DELETE /api/keys/{id}` - Revoke API key

#### Rate Limits
- `GET /rate-limits` - List all rate limits
- `POST /rate-limits` - Create new rate limit
- `PUT /rate-limits/{id}` - Update rate limit
- `PATCH /rate-limits/{id}/toggle` - Toggle active status
- `DELETE /rate-limits/{id}` - Delete rate limit

## Getting Started

### Prerequisites
- Node.js 18+ installed
- config-service running on port 8082

### Installation

1. Install dependencies:
```bash
npm install
```

2. Start the development server:
```bash
npm run dev
```

3. Open your browser to http://localhost:3000

The app will automatically redirect to `/dashboard`

### Production Build

```bash
npm run build
npm run start
```

## Project Structure

```
/app
  /dashboard
    page.js                 # Overview dashboard
    layout.js               # Dashboard layout
    /services
      page.js               # Gateway Services management
    /keys
      page.js               # API Keys management
    /rate-limits
      page.js               # Rate Limits management
    /logs
      page.js               # Logs & Violations
    /settings
      page.js               # Settings

/components
  Sidebar.js                # Navigation sidebar
  TopBar.js                 # Top status bar
  ServiceModal.js           # Service create/edit modal
  ApiKeyModal.js            # API key create/edit modal
  RateLimitModal.js         # Rate limit create/edit modal

/lib
  api.js                    # API client for config-service
```

## Design System

The application uses a custom dark theme inspired by Kong Manager:

- **Colors**: Dark blue/slate palette with blue accents
- **Components**: Custom styled tables, buttons, inputs, badges
- **Icons**: Lucide React icons
- **Responsive**: Works on desktop and mobile devices

## Key Features

### Client-Side Only
All pages are client-side rendered using the `'use client'` directive for optimal interactivity.

### Error Handling
Toast notifications for all API operations (success/error messages).

### Confirmation Dialogs
Destructive actions (delete, revoke) require user confirmation.

### Form Validation
All forms include required field validation and proper data formatting.

### Loading States
Loading indicators during data fetching operations.

## Future Enhancements

The codebase is structured to support future services:
- Analytics service integration
- Gateway service integration
- Authentication middleware
- Real-time logging
- Advanced settings configuration

## Tech Stack

- **Framework**: Next.js 13 (App Router)
- **Language**: JavaScript (no TypeScript)
- **Styling**: Tailwind CSS
- **Icons**: Lucide React
- **Backend**: Config Service (Port 8082)

## Notes

- No authentication is implemented yet (future feature)
- Logs page uses mock data (will integrate with analytics service)
- Settings page is a placeholder for future configuration options
- All API calls go to http://localhost:8082 only

## Support

For issues or questions about the config-service API, refer to the API documentation provided by the backend team.
