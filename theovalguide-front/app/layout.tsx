import { Geist, Geist_Mono } from "next/font/google";
import "./globals.css";

import { themeInitScript } from "@/components/theme/theme-init";
import ThemeToggleFab from "@/components/theme/theme-toggle";

import Providers from "./providers";

import type { Metadata } from "next";
import type { ReactNode } from "react";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  title: "The Oval Guide",
  description: "Read Views of teachers, and Courses",
};

export default function RootLayout({ children }: { children: ReactNode }) {
  return (
    <html lang="en" suppressHydrationWarning>
      {/* suppressHydrationWarning is required for next-themes */}
      <head>
        <script dangerouslySetInnerHTML={{ __html: themeInitScript }} />
      </head>
      <body className={`${geistSans.variable} ${geistMono.variable} antialiased`}>
        <Providers>
          {children}
          <ThemeToggleFab />
        </Providers>
      </body>
    </html>
  );
}
