from datetime import datetime, timedelta, timezone

import jwt
from pwdlib import PasswordHash

from app.config import settings


ALGORITHM = "HS256"
ACCESS_TOKEN_MINUTES = 30
password_hash = PasswordHash.recommended()


def hash_password(password: str) -> str:
    return password_hash.hash(password)


def verify_password(plain_password: str, hashed_password: str) -> bool:
    return password_hash.verify(plain_password, hashed_password)


def create_access_token(subject: str, role_code: str) -> str:
    expires_at = datetime.now(timezone.utc) + timedelta(
        minutes=ACCESS_TOKEN_MINUTES
    )
    return jwt.encode(
        {
            "sub": subject,
            "role": role_code,
            "exp": expires_at,
        },
        settings.secret_key,
        algorithm=ALGORITHM,
    )
