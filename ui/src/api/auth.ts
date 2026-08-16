import { api } from "./client";
import type { TokenOut, UserOut } from "./types";

export const authApi = {
  register: (name: string, email: string, age: number, password: string): Promise<UserOut> =>
    api.post("/auth/register", { name, email, age, password }).then((r) => r.data),

  login: (email: string, password: string): Promise<TokenOut> =>
    api.post("/auth/login", { email, password }).then((r) => r.data),

  logout: (refresh_token: string): Promise<void> =>
    api.post("/auth/logout", { refresh_token }).then(() => undefined),
};
