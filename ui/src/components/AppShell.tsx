import React from "react";
import { NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { BookOpen, Library, User, MessageSquare, LogOut, TrendingUp } from "lucide-react";

const NAV: { to: string; label: string; icon: React.ReactNode }[] = [
  { to: "/books", label: "Browse Books", icon: <Library size={18} /> },
  { to: "/trending", label: "Trending", icon: <TrendingUp size={18} /> },
  { to: "/profile", label: "My Profile", icon: <User size={18} /> },
  { to: "/chat", label: "AI Search", icon: <MessageSquare size={18} /> },
];

export function AppShell({ children }: { children: React.ReactNode }) {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate("/login");
  };

  return (
    <div style={styles.shell}>
      {/* Sidebar */}
      <aside style={styles.sidebar}>
        <div style={styles.logo}>
          <BookOpen size={24} color="#6366f1" />
          <span style={styles.logoText}>Novelist</span>
        </div>
        <nav style={styles.nav}>
          {NAV.map(({ to, label, icon }) => (
            <NavLink
              key={to}
              to={to}
              style={({ isActive }) => ({
                ...styles.navItem,
                ...(isActive ? styles.navItemActive : {}),
              })}
            >
              {icon}
              <span>{label}</span>
            </NavLink>
          ))}
        </nav>
        <button onClick={handleLogout} style={styles.logoutBtn}>
          <LogOut size={16} />
          <span>Sign out</span>
        </button>
      </aside>

      {/* Main content */}
      <main style={styles.main}>{children}</main>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  shell: {
    display: "flex",
    minHeight: "100vh",
    background: "#f8fafc",
  },
  sidebar: {
    width: 220,
    background: "#fff",
    borderRight: "1px solid #e5e7eb",
    display: "flex",
    flexDirection: "column",
    padding: "24px 0",
    flexShrink: 0,
  },
  logo: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    padding: "0 20px 24px",
    borderBottom: "1px solid #f1f5f9",
  },
  logoText: {
    fontSize: 18,
    fontWeight: 700,
    color: "#1e1b4b",
  },
  nav: {
    flex: 1,
    display: "flex",
    flexDirection: "column",
    gap: 2,
    padding: "16px 8px",
  },
  navItem: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    padding: "9px 12px",
    borderRadius: 8,
    color: "#6b7280",
    textDecoration: "none",
    fontSize: 14,
    fontWeight: 500,
    transition: "background 0.15s",
  },
  navItemActive: {
    background: "#eef2ff",
    color: "#6366f1",
  },
  logoutBtn: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    background: "none",
    border: "none",
    cursor: "pointer",
    color: "#9ca3af",
    fontSize: 14,
    padding: "10px 20px",
    margin: "0 8px",
    borderRadius: 8,
  },
  main: {
    flex: 1,
    padding: "32px 36px",
    overflowY: "auto",
  },
};
