from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models import UserAccount
from app.security import verify_password


def find_user(
    session: Session,
    login_id: str,
) -> UserAccount | None:
    return session.execute(
        select(UserAccount).where(UserAccount.login_id == login_id)
    ).scalar_one_or_none()


def authenticate_user(
    session: Session,
    login_id: str,
    password: str,
) -> UserAccount | None:
    user = find_user(session, login_id)
    if user is None or not user.is_active:
        return None
    if not verify_password(password, user.password_hash):
        return None
    return user
