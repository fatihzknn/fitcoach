/** @type {import('next').NextConfig} */
const nextConfig = {
  reactStrictMode: true,
  // PWA-ready: manifest + meta are wired in app/layout.tsx. A service worker can be
  // layered on later (e.g. next-pwa / Serwist) without restructuring the app.
};

export default nextConfig;
