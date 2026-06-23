import type { Metadata, Viewport } from "next";
import { Inter, Archivo } from "next/font/google";
import "./globals.css";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-sans",
  display: "swap",
});

const archivo = Archivo({
  subsets: ["latin"],
  variable: "--font-display",
  weight: ["600", "700", "800"],
  display: "swap",
});

export const metadata: Metadata = {
  title: "FitCoach — What am I doing today?",
  description:
    "A personalized AI gym coach that tells you exactly what to train today.",
  manifest: "/manifest.json",
  applicationName: "FitCoach",
  appleWebApp: { capable: true, title: "FitCoach", statusBarStyle: "black-translucent" },
};

export const viewport: Viewport = {
  themeColor: "#0a0e12",
  width: "device-width",
  initialScale: 1,
  maximumScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({
  children,
}: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en" className={`dark ${inter.variable} ${archivo.variable}`}>
      <body className="min-h-dvh antialiased">{children}</body>
    </html>
  );
}
