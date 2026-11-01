import { NavLink, Outlet } from 'react-router-dom';
import styles from './Layout.module.css';

const linkClass = ({ isActive }: { isActive: boolean }) =>
  isActive ? `${styles.link} ${styles.active}` : styles.link;

export function Layout() {
  return (
    <>
      <header className={styles.header}>
        <nav className={styles.nav} aria-label="Основная навигация">
          <NavLink to="/" end className={linkClass}>
            Календарь
          </NavLink>
          <NavLink to="/dishes" className={linkClass}>
            Блюда
          </NavLink>
        </nav>
      </header>
      <main>
        <Outlet />
      </main>
    </>
  );
}
