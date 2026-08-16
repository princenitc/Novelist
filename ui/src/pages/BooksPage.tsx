import React, { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { booksApi } from "../api/books";
import type { Book, PageOut } from "../api/types";
import { Search, Plus, Star, ChevronLeft, ChevronRight } from "lucide-react";
import { AddBookModal } from "../components/AddBookModal";

export function BooksPage() {
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [query, setQuery] = useState("");
  const [genre, setGenre] = useState("");
  const [sortBy, setSortBy] = useState<"title" | "rating" | "createdAt">("title");
  const [showAdd, setShowAdd] = useState(false);

  const isSearching = query.trim() !== "" || genre !== "";

  const { data, isLoading, refetch } = useQuery<PageOut<Book>>({
    queryKey: ["books", page, query, genre, sortBy],
    queryFn: () =>
      isSearching
        ? booksApi.search({
            query: query.trim() || undefined,
            genre: genre || undefined,
            sortBy,
            page,
            size: 20,
          })
        : booksApi.list(page, 20),
  });

  return (
    <div>
      {/* Header */}
      <div style={styles.header}>
        <div>
          <h1 style={styles.pageTitle}>Browse Books</h1>
          <p style={styles.pageSubtitle}>
            {data ? `${data.totalElements} books in library` : "Loading…"}
          </p>
        </div>
        <button onClick={() => setShowAdd(true)} style={styles.addBtn}>
          <Plus size={16} />
          Add Book
        </button>
      </div>

      {/* Search bar */}
      <div style={styles.searchRow}>
        <div style={styles.searchBox}>
          <Search size={16} color="#9ca3af" style={{ flexShrink: 0 }} />
          <input
            style={styles.searchInput}
            placeholder="Search by title or author…"
            value={query}
            onChange={(e) => { setQuery(e.target.value); setPage(0); }}
          />
        </div>
        <input
          style={styles.filterInput}
          placeholder="Genre"
          value={genre}
          onChange={(e) => { setGenre(e.target.value); setPage(0); }}
        />
        <select
          style={styles.filterInput}
          value={sortBy}
          onChange={(e) => setSortBy(e.target.value as "title" | "rating" | "createdAt")}
        >
          <option value="title">Sort: Title</option>
          <option value="rating">Sort: Rating</option>
          <option value="createdAt">Sort: Newest</option>
        </select>
      </div>

      {/* Book grid */}
      {isLoading ? (
        <div style={styles.empty}>Loading…</div>
      ) : !data || data.content.length === 0 ? (
        <div style={styles.empty}>No books found.</div>
      ) : (
        <div style={styles.grid}>
          {data.content.map((book) => (
            <BookCard
              key={book.bookId}
              book={book}
              onClick={() => navigate(`/books/${book.bookId}`)}
            />
          ))}
        </div>
      )}

      {/* Pagination */}
      {data && data.totalPages > 1 && (
        <div style={styles.pagination}>
          <button
            style={page === 0 ? styles.pageBtn : styles.pageBtnActive}
            disabled={page === 0}
            onClick={() => setPage((p) => p - 1)}
          >
            <ChevronLeft size={16} />
          </button>
          <span style={styles.pageInfo}>
            Page {page + 1} of {data.totalPages}
          </span>
          <button
            style={data.last ? styles.pageBtn : styles.pageBtnActive}
            disabled={data.last}
            onClick={() => setPage((p) => p + 1)}
          >
            <ChevronRight size={16} />
          </button>
        </div>
      )}

      {showAdd && (
        <AddBookModal
          onClose={() => setShowAdd(false)}
          onCreated={() => { setShowAdd(false); refetch(); }}
        />
      )}
    </div>
  );
}

function BookCard({ book, onClick }: { book: Book; onClick: () => void }) {
  return (
    <div style={styles.card} onClick={onClick}>
      {/* Cover placeholder */}
      <div style={styles.cover}>
        {book.coverImageUrl ? (
          <img src={book.coverImageUrl} alt={book.title} style={styles.coverImg} />
        ) : (
          <span style={styles.coverInitial}>{book.title[0]}</span>
        )}
      </div>
      <div style={styles.cardBody}>
        <p style={styles.cardTitle}>{book.title}</p>
        <p style={styles.cardAuthor}>{book.author}</p>
        {book.genres && book.genres.length > 0 && (
          <div style={styles.genreRow}>
            {book.genres.slice(0, 2).map((g) => (
              <span key={g} style={styles.genrePill}>
                {g}
              </span>
            ))}
          </div>
        )}
        <div style={styles.ratingRow}>
          <Star size={13} color="#f59e0b" fill="#f59e0b" />
          <span style={styles.ratingText}>
            {book.averageRating != null
              ? `${book.averageRating.toFixed(1)} (${book.totalRatings})`
              : "No ratings yet"}
          </span>
        </div>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  header: {
    display: "flex",
    justifyContent: "space-between",
    alignItems: "flex-start",
    marginBottom: 24,
  },
  pageTitle: { fontSize: 22, fontWeight: 700, color: "#111827", margin: 0 },
  pageSubtitle: { fontSize: 13, color: "#6b7280", marginTop: 4 },
  addBtn: {
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
  searchRow: {
    display: "flex",
    gap: 10,
    marginBottom: 24,
    flexWrap: "wrap",
  },
  searchBox: {
    display: "flex",
    alignItems: "center",
    gap: 8,
    background: "#fff",
    border: "1px solid #d1d5db",
    borderRadius: 8,
    padding: "8px 12px",
    flex: 1,
    minWidth: 200,
  },
  searchInput: {
    border: "none",
    outline: "none",
    fontSize: 14,
    flex: 1,
    color: "#111827",
    background: "transparent",
  },
  filterInput: {
    border: "1px solid #d1d5db",
    borderRadius: 8,
    padding: "8px 12px",
    fontSize: 14,
    color: "#374151",
    background: "#fff",
    outline: "none",
  },
  grid: {
    display: "grid",
    gridTemplateColumns: "repeat(auto-fill, minmax(180px, 1fr))",
    gap: 16,
  },
  card: {
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: 10,
    overflow: "hidden",
    cursor: "pointer",
    transition: "box-shadow 0.15s",
  },
  cover: {
    height: 140,
    background: "#eef2ff",
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
  },
  coverImg: { width: "100%", height: "100%", objectFit: "cover" },
  coverInitial: { fontSize: 40, fontWeight: 700, color: "#6366f1" },
  cardBody: { padding: 12 },
  cardTitle: {
    fontSize: 14,
    fontWeight: 600,
    color: "#111827",
    margin: "0 0 4px",
    display: "-webkit-box",
    WebkitLineClamp: 2,
    WebkitBoxOrient: "vertical",
    overflow: "hidden",
  },
  cardAuthor: { fontSize: 12, color: "#6b7280", margin: "0 0 8px" },
  genreRow: { display: "flex", gap: 4, flexWrap: "wrap", marginBottom: 8 },
  genrePill: {
    fontSize: 11,
    background: "#f1f5f9",
    color: "#475569",
    borderRadius: 4,
    padding: "2px 6px",
  },
  ratingRow: { display: "flex", alignItems: "center", gap: 4 },
  ratingText: { fontSize: 12, color: "#6b7280" },
  pagination: {
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    gap: 12,
    marginTop: 32,
  },
  pageBtn: {
    background: "#f3f4f6",
    border: "1px solid #e5e7eb",
    borderRadius: 6,
    padding: "6px 10px",
    cursor: "not-allowed",
    opacity: 0.5,
    display: "flex",
    alignItems: "center",
  },
  pageBtnActive: {
    background: "#fff",
    border: "1px solid #d1d5db",
    borderRadius: 6,
    padding: "6px 10px",
    cursor: "pointer",
    display: "flex",
    alignItems: "center",
  },
  pageInfo: { fontSize: 13, color: "#6b7280" },
  empty: { textAlign: "center", color: "#9ca3af", marginTop: 60, fontSize: 15 },
};
