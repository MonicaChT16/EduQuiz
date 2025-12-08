import Head from 'next/head';
import { FormEvent, useState } from 'react';
import { GoogleAuthProvider, signInWithPopup } from 'firebase/auth';
import { getFirebaseAuth } from '../lib/firebaseClient';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [status, setStatus] = useState<string | null>(null);

  const handleSubmit = (event: FormEvent) => {
    event.preventDefault();
    setStatus('Modo demo: implementa lógica real de autenticación.');
  };

  const handleGoogle = async () => {
    try {
      const auth = getFirebaseAuth();
      const provider = new GoogleAuthProvider();
      await signInWithPopup(auth, provider);
      setStatus('Sesión iniciada con Google (placeholder).');
    } catch (error) {
      setStatus((error as Error).message);
    }
  };

  return (
    <>
      <Head>
        <title>EduQuiz Admin | Login</title>
      </Head>
      <main className="auth-container">
        <section className="auth-panel">
          <h1>Panel Admin</h1>
          <p>Ingresa con tu cuenta corporativa para gestionar campañas y simulacros.</p>
          <form onSubmit={handleSubmit} className="auth-form">
            <label htmlFor="email">Correo institucional</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="admin@eduquiz.com"
              required
            />
            <button type="submit">Recibir enlace mágico</button>
          </form>
          <button type="button" onClick={handleGoogle} className="secondary">
            Continuar con Google Workspace
          </button>
          {status && <p className="status">{status}</p>}
        </section>
      </main>
    </>
  );
}
