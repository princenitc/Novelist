import React from "react";
import { useQuery } from "@tanstack/react-query";
import { analyticsApi } from "../api/analytics";
import type { Book, GenreCount } from "../api/types";
import { Star, TrendingUp, BarChart2 } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function TrendingPage() {
  const navigate = useNavigate();

  const { data: trending, isLoading: tLoading } = useQuery<Book[]>({
    queryKey: ["trending"],
    queryFn: () => analyticsApi.trending(10),
  });

  const { data: genres, isLoading: gLoading } = useQuery<GenreCount[]>({
    queryKey: ["genres"],
    queryFn: analyticsApi.genres,
  });

  const maxGenreCount = genres ? Math.max(...genres.map((g) => g.count), 1) : 1;

  return (
    <div>
      <h1 style={styles.pageTitle}>Trending & Analytics</h1>

      <div style={styles.layout}>
        {/* Trending books */}
        <section style={styles.section}>
          <div style={styles.sectionHeader}>
            <TrendingUp size={18} color="#6366f1" />
            <h2 style={styles.sectionTitle}>Top Rated Books</h2>
          </div>
          {tLoading ? (
            <p style={styles.muted}>Loading…</p>
          ) : !trending || trending.length === 0 ? (
            <p style={styles.muted}>No trending data yet.</p>
          ) : (
            <div style={styles.trendingList}>
              {trending.map((book, i) => (
                <div
                  key={book.bookId}
                  style={styles.trendingRow}
                  onClick={() => navigate(`/books/${book.bookId}`)}
                >
                  <span style={styles.rank}>#{i + 1}</span>
                  <div style={styles.trendingInfo}>
                    <p style={styles.trendingTitle}>{book.title}</p>
                    <p style={styles.trendingAuthor}>{book.author}</p>
                  </div>
                  <div style={styles.ratingChip}>
                    <Star size={12} color="#f59e0b" fill="#f59e0b" />
                    <span>
                      {book.averageRating != null
                        ? book.averageRating.toFixed(1)
                        : "—"}
                    </span>
                  </div>
                </div>
              ))}
            </div>
          )}
        </section>

        {/* Genre breakdown */}
        <section style={styles.section}>
          <div style={styles.sectionHeader}>
            <BarChart2 size={18} color="#6366f1" />
            <h2 style={styles.sectionTitle}>Genre Popularity</h2>
          </div>
          {gLoading ? (
            <p style={styles.muted}>Loading…</p>
          ) : !genres || genres.length === 0 ? (
            <p style={styles.muted}>No genre data yet.</p>
          ) : (
            <div style={styles.genreList}>
              {genres.slice(0, 12).map((g) => (
                <div key={g.genre} style={styles.genreRow}>
                  <span style={styles.genreLabel}>{g.genre}</span>
                  <div style={styles.barTrack}>
                    <div
                      style={{
                        ...styles.barFill,
                        width: `${(g.count / maxGenreCount) * 100}%`,
                      }}
                    />
                  </div>
                  <span style={styles.genreCount}>{g.count}</span>
                </div>
              ))}
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  pageTitle: { fontSize: 22, fontWeight: 700, color: "#111827", marginBottom: 28 },
  layout: { display: "flex", gap: 24, flexWrap: "wrap", alignItems: "flex-start" },
  section: {
    flex: 1,
    minWidth: 280,
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: 12,
    padding: 24,
  },
  sectionHeader: { display: "flex", alignItems: "center", gap: 8, marginBottom: 16 },
  sectionTitle: { fontSize: 15, fontWeight: 600, color: "#111827", margin: 0 },
  muted: { color: "#9ca3af", fontSize: 13 },
  trendingList: { display: "flex", flexDirection: "column", gap: 2 },
  trendingRow: {
    display: "flex",
    alignItems: "center",
    gap: 12,
    padding: "9px 8px",
    borderRadius: 8,
    cursor: "pointer",
  },
  rank: { fontSize: 13, fontWeight: 700, color: "#6366f1", width: 28, flexShrink: 0 },
  trendingInfo: { flex: 1 },
  trendingTitle: { fontSize: 13, fontWeight: 600, color: "#111827", margin: "0 0 2px" },
  trendingAuthor: { fontSize: 12, color: "#6b7280", margin: 0 },
  ratingChip: {
    display: "flex",
    alignItems: "center",
    gap: 3,
    fontSize: 12,
    fontWeight: 600,
    color: "#92400e",
    background: "#fffbeb",
    padding: "3px 7px",
    borderRadius: 5,
  },
  genreList: { display: "flex", flexDirection: "column", gap: 10 },
  genreRow: { display: "flex", alignItems: "center", gap: 10 },
  genreLabel: { fontSize: 13, color: "#374151", width: 90, flexShrink: 0 },
  barTrack: {
    flex: 1,
    height: 8,
    background: "#f1f5f9",
    borderRadius: 4,
    overflow: "hidden",
  },
  barFill: { height: "100%", background: "#6366f1", borderRadius: 4, transition: "width 0.4s" },
  genreCount: { fontSize: 12, color: "#6b7280", width: 28, textAlign: "right" },
};
