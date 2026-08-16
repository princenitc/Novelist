import { api } from "./client";
import type { Book, BookStats, GenreCount } from "./types";

export const analyticsApi = {
  bookStats: (bookId: string): Promise<BookStats> =>
    api.get(`/api/v1/analytics/books/${bookId}/stats`).then((r) => r.data),

  trending: (limit = 10): Promise<Book[]> =>
    api.get("/api/v1/analytics/books/trending", { params: { limit } }).then((r) => r.data),

  genres: (): Promise<GenreCount[]> =>
    api.get("/api/v1/analytics/genres").then((r) => r.data),
};
