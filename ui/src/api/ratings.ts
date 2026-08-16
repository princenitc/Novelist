import { api } from "./client";
import type { RatingOut } from "./types";

export const ratingsApi = {
  add: (userId: string, bookId: string, rating: number, review?: string): Promise<RatingOut> =>
    api
      .post(`/api/v1/users/${userId}/ratings/${bookId}`, { rating, review })
      .then((r) => r.data),
};
