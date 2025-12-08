import Head from 'next/head';
import Link from 'next/link';

const links = [
  { href: '/dashboard', label: 'General' },
  { href: '/dashboard/users', label: 'Usuarios' },
  { href: '/dashboard/exams', label: 'Simulacros' },
];

export default function DashboardPage() {
  return (
    <>
      <Head>
        <title>EduQuiz Admin | Dashboard</title>
      </Head>
      <main className="dashboard">
        <aside className="sidebar">
          <h2>EduQuiz</h2>
          <nav>
            {links.map((link) => (
              <Link key={link.href} href={link.href} className="nav-link">
                {link.label}
              </Link>
            ))}
          </nav>
        </aside>
        <section className="content">
          <h1>Panel principal</h1>
          <p>Este dashboard mostrará KPIs, últimos simulacros y accesos rápidos.</p>
        </section>
      </main>
    </>
  );
}
