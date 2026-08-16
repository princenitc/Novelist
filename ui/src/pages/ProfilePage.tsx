import React from "react";
import { useQuery } from "@tanstack/react-query";
import { profileApi } from "../api/profile";
import type { UserOut } from "../api/types";
import { Star, Mail, Calendar, BookMarked } from "lucide-react";
import { useNavigate } from "react-router-dom";

export function ProfilePage() {
  const navigate = useNavigate();
  const { data: user, isLoading } = useQuery<UserOut>({
    queryKey: ["profile"],
    queryFn: profileApi.me,
  });

  if (isLoading) return <div style={styles.empty}>Loading profile…</div>;
  if (!user) return <div style={styles.empty}>Could not load profile.</div>;

  return (
    <div>
      <h1 style={styles.pageTitle}>My Profile</h1>

      {/* Identity card */}
      <div style={styles.card}>
        <div style={styles.avatar}>
          {user.name[0].toUpperCase()}
        </div>
        <div>
          <p style={styles.name}>{user.name}</p>
          <div style={styles.metaRow}>
            {user.email && (
              <span style={styles.metaItem}>
                <Mail size={13} /> {user.email}
              </span>
            )}
            <span style={styles.metaItem}>
              <Calendar size={13} /> Age {user.age}
            </span>
            <span style={styles.metaItem}>
              <BookMarked size={13} /> {user.ratedBooks.length} rated books
            </span>
          </div>
          {user.preferences?.favoriteGenres && user.preferences.favoriteGenres.length > 0 && (
            <div style={styles.genreRow}>
              {user.preferences.favoriteGenres.map((g) => (
                <span key={g} style={styles.pill}>{g}</span>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Reading history */}
      <h2 style={styles.sectionTitle}>Reading History</h2>
      {user.ratedBooks.length === 0 ? (
        <div style={styles.empty}>No rated books yet. Find a book and leave a review!</div>
      ) : (
        <div style={styles.list}>
          {user.ratedBooks.map((entry, i) => (
            <div
              key={i}
              style={styles.historyRow}
              onClick={() => entry.book && navigate(`/books/${entry.book.bookId}`)}
            >
              {/* Cover */}
              <div style={styles.miniCover}>
                {entry.book?.coverImageUrl ? (
                  <img src={entry.book.coverImageUrl} alt="" style={styles.miniCoverImg} />
                ) : (
                  <span style={styles.miniCoverInitial}>{entry.book?.title?.[0] ?? "?"}</span>
                )}
              </div>
              <div style={styles.historyBody}>
                <p style={styles.historyTitle}>{entry.book?.title ?? "Unknown book"}</p>
                <p style={styles.historyAuthor}>{entry.book?.author ?? ""}</p>
                {entry.review && <p style={styles.historyReview}>"{entry.review}"</p>}
              </div>
              <div style={styles.historyStars}>
                {[1, 2, 3, 4, 5].map((n) => (
                  <Star
                    key={n}
                    size={14}
                    color={n <= entry.rating ? "#f59e0b" : "#d1d5db"}
                    fill={n <= entry.rating ? "#f59e0b" : "none"}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  pageTitle: { fontSize: 22, fontWeight: 700, color: "#111827", marginBottom: 24 },
  card: {
    display: "flex",
    alignItems: "flex-start",
    gap: 20,
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: 12,
    padding: 24,
    marginBottom: 32,
  },
  avatar: {
    width: 60,
    height: 60,
    borderRadius: "50%",
    background: "#eef2ff",
    color: "#6366f1",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    fontSize: 24,
    fontWeight: 700,
    flexShrink: 0,
  },
  name: { fontSize: 18, fontWeight: 700, color: "#111827", margin: "0 0 8px" },
  metaRow: { display: "flex", flexWrap: "wrap", gap: 16, marginBottom: 10 },
  metaItem: {
    display: "flex",
    alignItems: "center",
    gap: 5,
    fontSize: 13,
    color: "#6b7280",
  },
  genreRow: { display: "flex", flexWrap: "wrap", gap: 6 },
  pill: {
    fontSize: 12,
    background: "#eef2ff",
    color: "#6366f1",
    borderRadius: 4,
    padding: "2px 8px",
  },
  sectionTitle: { fontSize: 16, fontWeight: 600, color: "#111827", marginBottom: 14 },
  list: { display: "flex", flexDirection: "column", gap: 10 },
  historyRow: {
    display: "flex",
    alignItems: "center",
    gap: 14,
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: 10,
    padding: "12px 16px",
    cursor: "pointer",
  },
  miniCover: {
    width: 44,
    height: 60,
    borderRadius: 6,
    background: "#eef2ff",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    flexShrink: 0,
    overflow: "hidden",
  },
  miniCoverImg: { width: "100%", height: "100%", objectFit: "cover" },
  miniCoverInitial: { fontSize: 20, fontWeight: 700, color: "#6366f1" },
  historyBody: { flex: 1 },
  historyTitle: { fontSize: 14, fontWeight: 600, color: "#111827", margin: "0 0 3px" },
  historyAuthor: { fontSize: 12, color: "#6b7280", margin: "0 0 6px" },
  historyReview: {
    fontSize: 12, color: "#6b7280", fontStyle: "italic",
    margin: 0, display: "-webkit-box",
    WebkitLineClamp: 2, WebkitBoxOrient: "vertical", overflow: "hidden",
  },
  historyStars: { display: "flex", gap: 2, flexShrink: 0 },
  empty: { color: "#9ca3af", marginTop: 40, fontSize: 14 },
};
