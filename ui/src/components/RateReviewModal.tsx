import React, { useState } from "react";
import { useMutation } from "@tanstack/react-query";
import { ratingsApi } from "../api/ratings";
import type { Book } from "../api/types";
import { Star, X } from "lucide-react";

interface Props {
  book: Book;
  userId: string;
  onClose: () => void;
  onSubmitted: () => void;
}

export function RateReviewModal({ book, userId, onClose, onSubmitted }: Props) {
  const [rating, setRating] = useState(0);
  const [hovered, setHovered] = useState(0);
  const [review, setReview] = useState("");
  const [error, setError] = useState("");

  const mutation = useMutation({
    mutationFn: () => ratingsApi.add(userId, book.bookId, rating, review.trim() || undefined),
    onSuccess: onSubmitted,
    onError: (err: unknown) => {
      const msg =
        (err as { response?: { data?: { detail?: string } } })?.response?.data?.detail ??
        "Failed to submit rating.";
      setError(msg);
    },
  });

  const submit = (e: React.FormEvent) => {
    e.preventDefault();
    if (rating === 0) { setError("Please select a star rating."); return; }
    setError("");
    mutation.mutate();
  };

  const display = hovered || rating;

  return (
    <div style={overlay}>
      <div style={styles.modal}>
        <div style={styles.header}>
          <div>
            <h2 style={styles.title}>Rate & Review</h2>
            <p style={styles.bookName}>{book.title}</p>
          </div>
          <button style={styles.closeBtn} onClick={onClose}><X size={18} /></button>
        </div>

        {error && <div style={styles.errorBanner}>{error}</div>}

        <form onSubmit={submit}>
          {/* Star picker */}
          <p style={styles.label}>Your rating</p>
          <div style={styles.stars}>
            {[1, 2, 3, 4, 5].map((n) => (
              <Star
                key={n}
                size={32}
                color={n <= display ? "#f59e0b" : "#d1d5db"}
                fill={n <= display ? "#f59e0b" : "none"}
                style={{ cursor: "pointer", transition: "color 0.1s" }}
                onMouseEnter={() => setHovered(n)}
                onMouseLeave={() => setHovered(0)}
                onClick={() => setRating(n)}
              />
            ))}
          </div>
          <p style={styles.ratingLabel}>
            {["", "Poor", "Fair", "Good", "Very Good", "Excellent"][display] ?? ""}
          </p>

          {/* Review text */}
          <p style={{ ...styles.label, marginTop: 16 }}>Review (optional)</p>
          <textarea
            style={styles.textarea}
            placeholder="Share your thoughts about this book…"
            value={review}
            onChange={(e) => setReview(e.target.value)}
            maxLength={1000}
            rows={4}
          />
          <p style={styles.charCount}>{review.length}/1000</p>

          <div style={styles.footer}>
            <button type="button" style={styles.cancelBtn} onClick={onClose}>Cancel</button>
            <button type="submit" style={styles.submitBtn} disabled={mutation.isPending || rating === 0}>
              {mutation.isPending ? "Submitting…" : "Submit Review"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}

const overlay: React.CSSProperties = {
  position: "fixed", inset: 0,
  background: "rgba(0,0,0,0.4)",
  display: "flex", alignItems: "center", justifyContent: "center",
  zIndex: 1000, padding: 16,
};

const styles: Record<string, React.CSSProperties> = {
  modal: { background: "#fff", borderRadius: 12, width: "100%", maxWidth: 440, padding: 28 },
  header: { display: "flex", justifyContent: "space-between", alignItems: "flex-start", marginBottom: 16 },
  title: { fontSize: 17, fontWeight: 700, color: "#111827", margin: "0 0 4px" },
  bookName: { fontSize: 13, color: "#6b7280", margin: 0 },
  closeBtn: { background: "none", border: "none", cursor: "pointer", color: "#9ca3af", padding: 4 },
  errorBanner: {
    background: "#fef2f2", border: "1px solid #fecaca", color: "#b91c1c",
    borderRadius: 8, padding: "9px 12px", fontSize: 13, marginBottom: 14,
  },
  label: { fontSize: 13, fontWeight: 500, color: "#374151", margin: "0 0 8px" },
  stars: { display: "flex", gap: 6 },
  ratingLabel: { fontSize: 13, color: "#f59e0b", fontWeight: 600, minHeight: 18, margin: "6px 0 0" },
  textarea: {
    width: "100%", border: "1px solid #d1d5db", borderRadius: 8,
    padding: "9px 12px", fontSize: 13, resize: "vertical",
    outline: "none", color: "#111827", boxSizing: "border-box",
  },
  charCount: { fontSize: 11, color: "#9ca3af", textAlign: "right", margin: "4px 0 0" },
  footer: { display: "flex", justifyContent: "flex-end", gap: 10, marginTop: 20 },
  cancelBtn: {
    background: "#f9fafb", border: "1px solid #d1d5db", borderRadius: 8,
    padding: "8px 16px", fontSize: 13, cursor: "pointer", color: "#374151",
  },
  submitBtn: {
    background: "#6366f1", color: "#fff", border: "none", borderRadius: 8,
    padding: "8px 18px", fontSize: 13, fontWeight: 600, cursor: "pointer",
  },
};
