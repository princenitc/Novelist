import React, { createContext, useContext, useState, useCallback } from "react";
import { authApi } from "../api/auth";

interface AuthState {
  userId: string | null;
  isAuthenticated: boolean;
}

interface AuthContextValue extends AuthState {
  login: (email: string, password: string) => Promise<void>;
  register: (name: string, email: string, age: number, password: string) => Promise<void>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>(() => ({
    userId: localStorage.getItem("user_id"),
    isAuthenticated: !!localStorage.getItem("access_token"),
  }));

  const login = useCallback(async (email: string, password: string) => {
    const tokens = await authApi.login(email, password);
    localStorage.setItem("access_token", tokens.access_token);
    localStorage.setItem("refresh_token", tokens.refresh_token);
    // Decode userId from JWT payload
    const payload = JSON.parse(atob(tokens.access_token.split(".")[1]));
    const userId: string = payload.sub;
    localStorage.setItem("user_id", userId);
    setState({ userId, isAuthenticated: true });
  }, []);

  const register = useCallback(
    async (name: string, email: string, age: number, password: string) => {
      await authApi.register(name, email, age, password);
      await login(email, password);
    },
    [login]
  );

  const logout = useCallback(async () => {
    const rt = localStorage.getItem("refresh_token");
    if (rt) {
      try {
        await authApi.logout(rt);
      } catch {
        // best-effort
      }
    }
    localStorage.removeItem("access_token");
    localStorage.removeItem("refresh_token");
    localStorage.removeItem("user_id");
    setState({ userId: null, isAuthenticated: false });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider>");
  return ctx;
}
