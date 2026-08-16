from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    app_name: str = "Novelist"
    port: int = 8081
    log_level: str = "INFO"          # DEBUG | INFO | WARNING | ERROR

    neo4j_uri: str = "bolt://localhost:7687"
    neo4j_user: str = "neo4j"
    neo4j_password: str = "password"

    rabbitmq_enabled: bool = True
    rabbitmq_host: str = "localhost"
    rabbitmq_port: int = 5672
    rabbitmq_user: str = "novelist"
    rabbitmq_password: str = "password"

    jwt_secret_key: str = "change-this-secret"
    jwt_algorithm: str = "HS256"
    jwt_expiry_minutes: int = 60           # access token lifetime
    jwt_refresh_expiry_days: int = 7       # refresh token lifetime

    # CORS — comma-separated list of allowed origins, e.g. "http://localhost:3000,https://myapp.com"
    # Use "*" to allow all origins (development only — insecure in production)
    cors_origins: list[str] = ["*"]

    # RAG
    rag_embedding_model: str = "all-MiniLM-L6-v2"  # 384-dim, fast local model
    rag_chunk_size: int = 512        # characters per chunk
    rag_chunk_overlap: int = 64      # overlap between consecutive chunks
    rag_top_k: int = 5               # default number of results returned


@lru_cache
def get_settings() -> Settings:
    return Settings()
