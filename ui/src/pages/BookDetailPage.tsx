import React, { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { booksApi } from "../api/books";
import { analyticsApi } from "../api/analytics";
import type { Book, BookStats } from "../api/types";
import { Star, ArrowLeft, BookOpen, Calendar, Hash, FileText, Tag } from "lucide-react";
import { RateReviewModal } from "../components/RateReviewModal";
import { useAuth } from "../context/AuthContext";

export function BookDetailPage() {
  const { bookId } = useParams<{ bookId: string }>();
  const navigate = useNavigate();
  const { userId } = useAuth();
  const queryClient = useQueryClient();
  const [showRate, setShowRate] = useState(false);

  const { data: book, isLoading } = useQuery<Book>({
    queryKey: ["book", bookId],
    queryFn: () => booksApi.get(bookId!),
    enabled: !!bookId,
  });

  const { data: stats } = useQuery<BookStats>({
    queryKey: ["bookStats", bookId],
    queryFn: () => analyticsApi.bookStats(bookId!),
    enabled: !!bookId,
  });

  const deleteMutation = useMutation({
    mutationFn: () => booksApi.delete(bookId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["books"] });
      navigate("/books");
    },
  });

  if (isLoading) return <div style={styles.loading}>Loading…</div>;
  if (!book) return <div style={styles.loading}>Book not found.</div>;

  return (
    <div>
      <button style={styles.back} onClick={() => navigate(-1)}>
        <ArrowLeft size={16} />
        Back
      </button>

      <div style={styles.layout}>
        {/* Cover */}
        <div style={styles.coverWrap}>
          {book.coverImageUrl ? (
            <img src={book.coverImageUrl} alt={book.title} style={styles.coverImg} />
          ) : (
            <div style={styles.coverPlaceholder}>
              <span style={styles.coverInitial}>{book.title[0]}</span>
            </div>
          )}
        </div>

        {/* Details */}
        <div style={styles.details}>
          <h1 style={styles.title}>{book.title}</h1>
          <p style={styles.author}>by {book.author}</p>

          {/* Ratings summary */}
          <div style={styles.ratingBox}>
            <div style={styles.ratingBig}>
              <Star size={20} color="#f59e0b" fill="#f59e0b" />
              <span style={styles.ratingNum}>
                {stats?.averageRating != null
                  ? stats.averageRating.toFixed(1)
                  : book.averageRating != null
                  ? book.averageRating.toFixed(1)
                  : "—"}
              </span>
            </div>
            <span style={styles.ratingCount}>
              {(stats?.totalRatings ?? book.totalRatings ?? 0)} ratings
            </span>
          </div>

          {/* Meta */}
          <div style={styles.metaGrid}>
            {book.publishedYear && (
              <MetaItem icon={<Calendar size={14} />} label="Year" value={String(book.publishedYear)} />
            )}
            {book.pageCount && (
              <MetaItem icon={<BookOpen size={14} />} label="Pages" value={String(book.pageCount)} />
            )}
            {book.isbn && (
              <MetaItem icon={<Hash size={14} />} label="ISBN" value={book.isbn} />
            )}
            {book.language && (
              <MetaItem icon={<FileText size={14} />} label="Language" value={book.language.toUpperCase()} />
            )}
          </div>

          {/* Genres */}
          {book.genres && book.genres.length > 0 && (
            <div style={styles.genreRow}>
              <Tag size={13} color="#6b7280" />
              {book.genres.map((g) => (
                <span key={g} style={styles.genrePill}>{g}</span>
              ))}
            </div>
          )}

          {/* Description */}
          {book.description && (
            <p style={styles.description}>{book.description}</p>
          )}

          {/* Actions */}
          <div style={styles.actions}>
            <button style={styles.rateBtn} onClick={() => setShowRate(true)}>
              <Star size={15} />
              Rate & Review
            </button>
            <button
              style={styles.deleteBtn}
              onClick={() => {
                if (confirm(`Delete "${book.title}"?`)) deleteMutation.mutate();
              }}
            >
              Delete book
            </button>
          </div>
        </div>
      </div>

      {showRate && userId && (
        <RateReviewModal
          book={book}
          userId={userId}
          onClose={() => setShowRate(false)}
          onSubmitted={() => {
            setShowRate(false);
            queryClient.invalidateQueries({ queryKey: ["bookStats", bookId] });
          }}
        />
      )}
    </div>
  );
}

function MetaItem({ icon, label, value }: { icon: React.ReactNode; label: string; value: string }) {
  return (
    <div style={metaStyles.item}>
      {icon}
      <span style={metaStyles.label}>{label}</span>
      <span style={metaStyles.value}>{value}</span>
    </div>
  );
}
const metaStyles: Record<string, React.CSSProperties> = {
  item: { display: "flex", alignItems: "center", gap: 6, color: "#6b7280" },
  label: { fontSize: 12, fontWeight: 500 },
  value: { fontSize: 12, color: "#374151" },
};

const styles: Record<string, React.CSSProperties> = {
  loading: { color: "#9ca3af", marginTop: 60, textAlign: "center" },
  back: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    background: "none",
    border: "none",
    cursor: "pointer",
    color: "#6b7280",
    fontSize: 14,
    padding: 0,
    marginBottom: 24,
  },
  layout: { display: "flex", gap: 40, alignItems: "flex-start", flexWrap: "wrap" },
  coverWrap: { flexShrink: 0 },
  coverImg: { width: 200, height: 280, objectFit: "cover", borderRadius: 10 },
  coverPlaceholder: {
    width: 200,
    height: 280,
    background: "#eef2ff",
    borderRadius: 10,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  coverInitial: { fontSize: 64, fontWeight: 700, color: "#6366f1" },
  details: { flex: 1, minWidth: 260 },
  title: { fontSize: 24, fontWeight: 700, color: "#111827", margin: "0 0 6px" },
  author: { fontSize: 15, color: "#6b7280", margin: "0 0 16px" },
  ratingBox: {
    display: "flex",
    alignItems: "center",
    gap: 10,
    background: "#fffbeb",
    border: "1px solid #fde68a",
    borderRadius: 8,
    padding: "10px 14px",
    marginBottom: 16,
    width: "fit-content",
  },
  ratingBig: { display: "flex", alignItems: "center", gap: 6 },
  ratingNum: { fontSize: 20, fontWeight: 700, color: "#92400e" },
  ratingCount: { fontSize: 13, color: "#92400e" },
  metaGrid: { display: "flex", flexWrap: "wrap", gap: 16, marginBottom: 16 },
  genreRow: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    flexWrap: "wrap",
    marginBottom: 16,
  },
  genrePill: {
    fontSize: 12,
    background: "#f1f5f9",
    color: "#475569",
    borderRadius: 4,
    padding: "3px 8px",
  },
  description: { fontSize: 14, color: "#374151", lineHeight: 1.7, marginBottom: 24 },
  actions: { display: "flex", gap: 10, flexWrap: "wrap" },
  rateBtn: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    background: "#6366f1",
    color: "#fff",
    border: "none",
    borderRadius: 8,
    padding: "9px 16px",
    fontSize: 14,
    fontWeight: 600,
    cursor: "pointer",
  },
  deleteBtn: {
    background: "#fff",
    color: "#dc2626",
    border: "1px solid #fca5a5",
    borderRadius: 8,
    padding: "9px 16px",
    fontSize: 14,
    cursor: "pointer",
  },
};
