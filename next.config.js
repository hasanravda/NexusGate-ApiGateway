/** @type {import('next').NextConfig} */
const nextConfig = {
  eslint: {
    ignoreDuringBuilds: true,
  },
  images: { unoptimized: true },
  async rewrites() {
    return [
      {
        source: '/api/load-test/:path*',
        destination: 'http://localhost:8083/load-test/:path*',
      },
      {
        source: '/service-routes/:path*',
        destination: 'http://localhost:8082/service-routes/:path*',
      },
      {
        source: '/api/keys/:path*',
        destination: 'http://localhost:8082/api/keys/:path*',
      },
      {
        source: '/rate-limits/:path*',
        destination: 'http://localhost:8082/rate-limits/:path*',
      },
    ];
  },
};

module.exports = nextConfig;
