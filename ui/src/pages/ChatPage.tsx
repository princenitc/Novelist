import React, { useState, useRef, useEffect } from "react";
import { ragApi } from "../api/rag";
import type { RagChunk } from "../api/types";
import { Send, MessageSquare, BookOpen } from "lucide-react";
import { useNavigate } from "react-router-dom";

interface Message {
  role: "user" | "assistant";
  text: string;
  results?: RagChunk[];
}

export function ChatPage() {
  const navigate = useNavigate();
  const [messages, setMessages] = useState<Message[]>([
    {
      role: "assistant",
      text: "Hi! Ask me anything about books in your library. I'll search through the book content using AI-powered semantic search.",
    },
  ]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  const send = async () => {
    const q = input.trim();
    if (!q || loading) return;

    setMessages((m) => [...m, { role: "user", text: q }]);
    setInput("");
    setLoading(true);

    try {
      const res = await ragApi.search(q, 5);
      if (res.results.length === 0) {
        setMessages((m) => [
          ...m,
          { role: "assistant", text: "I couldn't find any relevant content for that query. Try indexing more books first." },
        ]);
      } else {
        const summary = `Found ${res.results.length} relevant passage${res.results.length > 1 ? "s" : ""} for "${q}":`;
        setMessages((m) => [
          ...m,
          { role: "assistant", text: summary, results: res.results },
        ]);
      }
    } catch {
      setMessages((m) => [
        ...m,
        { role: "assistant", text: "Something went wrong. Make sure books are indexed before searching." },
      ]);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.page}>
      <div style={styles.header}>
        <MessageSquare size={20} color="#6366f1" />
        <div>
          <h1 style={styles.title}>AI Book Search</h1>
          <p style={styles.subtitle}>Semantic search powered by RAG — searches through actual book content</p>
        </div>
      </div>

      {/* Chat thread */}
      <div style={styles.thread}>
        {messages.map((msg, i) => (
          <div key={i} style={msg.role === "user" ? styles.userBubbleWrap : styles.botBubbleWrap}>
            <div style={msg.role === "user" ? styles.userBubble : styles.botBubble}>
              <p style={styles.bubbleText}>{msg.text}</p>
            </div>
            {msg.results && msg.results.length > 0 && (
              <div style={styles.results}>
                {msg.results.map((chunk, j) => (
                  <div key={j} style={styles.resultCard}>
                    <div style={styles.resultHeader}>
                      <BookOpen size={13} color="#6366f1" />
                      <span
                        style={styles.resultTitle}
                        onClick={() => navigate(`/books/${chunk.bookId}`)}
                      >
                        {chunk.title}
                      </span>
                      <span style={styles.resultAuthor}>— {chunk.author}</span>
                      <span style={styles.resultScore}>
                        {(chunk.score * 100).toFixed(0)}% match
                      </span>
                    </div>
                    <p style={styles.resultText}>{chunk.text}</p>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}
        {loading && (
          <div style={styles.botBubbleWrap}>
            <div style={styles.botBubble}>
              <p style={styles.bubbleText}>Searching…</p>
            </div>
          </div>
        )}
        <div ref={bottomRef} />
      </div>

      {/* Input */}
      <div style={styles.inputRow}>
        <input
          style={styles.input}
          placeholder="Ask about any book content… e.g. 'books about redemption'"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={(e) => e.key === "Enter" && send()}
          disabled={loading}
        />
        <button style={styles.sendBtn} onClick={send} disabled={loading || !input.trim()}>
          <Send size={16} />
        </button>
      </div>
    </div>
  );
}

const styles: Record<string, React.CSSProperties> = {
  page: {
    display: "flex",
    flexDirection: "column",
    height: "calc(100vh - 64px)",
  },
  header: {
    display: "flex",
    alignItems: "flex-start",
    gap: 12,
    marginBottom: 20,
  },
  title: { fontSize: 20, fontWeight: 700, color: "#111827", margin: "0 0 3px" },
  subtitle: { fontSize: 13, color: "#6b7280", margin: 0 },
  thread: {
    flex: 1,
    overflowY: "auto",
    display: "flex",
    flexDirection: "column",
    gap: 16,
    paddingRight: 4,
    marginBottom: 16,
  },
  userBubbleWrap: { display: "flex", justifyContent: "flex-end" },
  botBubbleWrap: { display: "flex", flexDirection: "column", alignItems: "flex-start", gap: 10 },
  userBubble: {
    background: "#6366f1",
    color: "#fff",
    borderRadius: "12px 12px 4px 12px",
    padding: "10px 14px",
    maxWidth: "70%",
  },
  botBubble: {
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: "12px 12px 12px 4px",
    padding: "10px 14px",
    maxWidth: "80%",
  },
  bubbleText: { fontSize: 14, margin: 0, lineHeight: 1.5 },
  results: { display: "flex", flexDirection: "column", gap: 8, width: "100%" },
  resultCard: {
    background: "#fff",
    border: "1px solid #e5e7eb",
    borderRadius: 10,
    padding: "12px 14px",
  },
  resultHeader: {
    display: "flex",
    alignItems: "center",
    gap: 6,
    marginBottom: 8,
    flexWrap: "wrap",
  },
  resultTitle: {
    fontSize: 13,
    fontWeight: 600,
    color: "#6366f1",
    cursor: "pointer",
    textDecoration: "underline",
  },
  resultAuthor: { fontSize: 12, color: "#6b7280" },
  resultScore: {
    fontSize: 11,
    background: "#f0fdf4",
    color: "#16a34a",
    borderRadius: 4,
    padding: "2px 6px",
    marginLeft: "auto",
  },
  resultText: {
    fontSize: 13,
    color: "#374151",
    lineHeight: 1.6,
    margin: 0,
    display: "-webkit-box",
    WebkitLineClamp: 4,
    WebkitBoxOrient: "vertical",
    overflow: "hidden",
  },
  inputRow: {
    display: "flex",
    gap: 10,
    background: "#fff",
    border: "1px solid #d1d5db",
    borderRadius: 10,
    padding: "8px 10px",
    alignItems: "center",
  },
  input: {
    flex: 1,
    border: "none",
    outline: "none",
    fontSize: 14,
    color: "#111827",
    background: "transparent",
  },
  sendBtn: {
    background: "#6366f1",
    color: "#fff",
    border: "none",
    borderRadius: 7,
    width: 34,
    height: 34,
    display: "flex",
    alignItems: "center",
    justifyContent: "center",
    cursor: "pointer",
    flexShrink: 0,
  },
};
