import type { Metadata } from 'next';
import './globals.css';
export const metadata: Metadata = { title: 'MONO / 创作实验室', description: '把每一个灵感，做成看得见的作品。' };
export default function RootLayout({ children }: Readonly<{children: React.ReactNode}>) { return <html lang="zh-CN"><body>{children}</body></html>; }
