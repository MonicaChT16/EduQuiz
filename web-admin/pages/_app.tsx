import type { AppProps } from 'next/app';
import '../styles/globals.css';

export default function EduQuizAdminApp({ Component, pageProps }: AppProps) {
  return <Component {...pageProps} />;
}
