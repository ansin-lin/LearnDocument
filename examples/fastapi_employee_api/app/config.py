from functools import lru_cache

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    database_url: str
    secret_key: str = Field(min_length=32)
    app_name: str = "Employee API"
    seed_admin_password: str | None = None


@lru_cache
def get_settings() -> Settings:
    return Settings()


settings = get_settings()
