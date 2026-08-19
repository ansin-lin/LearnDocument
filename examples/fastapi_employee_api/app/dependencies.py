from collections.abc import Generator
from typing import Annotated

import jwt
from fastapi import Depends, HTTPException, Query
from fastapi.security import OAuth2PasswordBearer
from jwt.exceptions import InvalidTokenError
from pydantic import BaseModel
from sqlalchemy.orm import Session

from app.config import settings
from app.database import SessionLocal
from app.models import UserAccount
from app.security import ALGORITHM
from app.services.auth_service import find_user


oauth2_scheme = OAuth2PasswordBearer(tokenUrl="/api/auth/token")


class PaginationParams(BaseModel):
    page: int
    size: int


def get_pagination(
    page: Annotated[int, Query(ge=1)] = 1,
    size: Annotated[int, Query(ge=1, le=100)] = 20,
) -> PaginationParams:
    return PaginationParams(page=page, size=size)


PaginationDep = Annotated[
    PaginationParams,
    Depends(get_pagination),
]


def get_db() -> Generator[Session, None, None]:
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()


SessionDep = Annotated[Session, Depends(get_db)]


def get_current_user(
    token: Annotated[str, Depends(oauth2_scheme)],
    db: SessionDep,
) -> UserAccount:
    credentials_error = HTTPException(
        status_code=401,
        detail="认证信息无效",
        headers={"WWW-Authenticate": "Bearer"},
    )
    try:
        payload = jwt.decode(
            token,
            settings.secret_key,
            algorithms=[ALGORITHM],
        )
        login_id = payload.get("sub")
        if not isinstance(login_id, str):
            raise credentials_error
    except InvalidTokenError:
        raise credentials_error

    user = find_user(db, login_id)
    if user is None or not user.is_active:
        raise credentials_error
    return user


def require_editor(
    current_user: Annotated[UserAccount, Depends(get_current_user)],
) -> UserAccount:
    if current_user.role_code not in {"SYSTEM_ADMIN", "HR_STAFF"}:
        raise HTTPException(status_code=403, detail="没有权限")
    return current_user


def require_admin(
    current_user: Annotated[UserAccount, Depends(get_current_user)],
) -> UserAccount:
    if current_user.role_code != "SYSTEM_ADMIN":
        raise HTTPException(status_code=403, detail="没有权限")
    return current_user
