import { api } from "./client";
import type { UserOut } from "./types";

export const profileApi = {
  me: (): Promise<UserOut> => api.get("/api/v1/me").then((r) => r.data),
};
