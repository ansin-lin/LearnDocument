import os

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker
from sqlalchemy.pool import StaticPool


os.environ.setdefault("DATABASE_URL", "sqlite://")
os.environ.setdefault(
    "SECRET_KEY",
    "test-only-secret-key-at-least-32-bytes-long",
)

from app.database import Base
from app.dependencies import get_db
from app.main import app
from app.models import Department, UserAccount
from app.security import hash_password


test_engine = create_engine(
    "sqlite://",
    connect_args={"check_same_thread": False},
    poolclass=StaticPool,
)
TestSessionLocal = sessionmaker(
    bind=test_engine,
    autoflush=False,
    expire_on_commit=False,
)


@pytest.fixture
def db_session():
    Base.metadata.create_all(bind=test_engine)
    with TestSessionLocal() as session:
        session.add(Department(id=1, name="开发部"))
        session.add(Department(id=2, name="营业部"))
        session.add_all(
            [
                UserAccount(
                    login_id="admin",
                    password_hash=hash_password("test-password"),
                    role_code="SYSTEM_ADMIN",
                ),
                UserAccount(
                    login_id="viewer",
                    password_hash=hash_password("viewer-password"),
                    role_code="VIEWER",
                ),
                UserAccount(
                    login_id="hr_staff",
                    password_hash=hash_password("hr-password"),
                    role_code="HR_STAFF",
                ),
            ]
        )
        session.commit()
        yield session
        session.rollback()
    Base.metadata.drop_all(bind=test_engine)


@pytest.fixture
def client(db_session: Session):
    def override_get_db():
        yield db_session

    app.dependency_overrides[get_db] = override_get_db
    with TestClient(app) as test_client:
        yield test_client
    app.dependency_overrides.clear()


def login_headers(
    client: TestClient,
    login_id: str,
    password: str,
) -> dict[str, str]:
    response = client.post(
        "/api/auth/token",
        data={
            "username": login_id,
            "password": password,
        },
    )
    assert response.status_code == 200
    token = response.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


@pytest.fixture
def admin_headers(client: TestClient):
    return login_headers(client, "admin", "test-password")


@pytest.fixture
def viewer_headers(client: TestClient):
    return login_headers(client, "viewer", "viewer-password")


@pytest.fixture
def hr_staff_headers(client: TestClient):
    return login_headers(client, "hr_staff", "hr-password")
