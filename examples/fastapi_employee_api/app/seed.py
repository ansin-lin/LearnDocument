from datetime import date

from sqlalchemy import select

from app.config import settings
from app.database import SessionLocal
from app.models import Department, Employee, UserAccount
from app.security import hash_password


def main() -> None:
    admin_password = settings.seed_admin_password
    if not admin_password:
        raise RuntimeError("SEED_ADMIN_PASSWORD is required")

    with SessionLocal.begin() as session:
        development = session.execute(
            select(Department).where(Department.name == "开发部")
        ).scalar_one_or_none()
        if development is None:
            development = Department(name="开发部")
            session.add(development)

        sales = session.execute(
            select(Department).where(Department.name == "营业部")
        ).scalar_one_or_none()
        if sales is None:
            sales = Department(name="营业部")
            session.add(sales)

        session.flush()

        sample_employees = [
            {
                "employee_number": "E001",
                "name": "山田太郎",
                "department_id": development.id,
                "email": "yamada@example.com",
                "joined_on": date(2026, 4, 1),
            },
            {
                "employee_number": "E002",
                "name": "佐藤花子",
                "department_id": sales.id,
                "email": "",
                "joined_on": date(2025, 10, 1),
            },
        ]
        for values in sample_employees:
            employee = session.execute(
                select(Employee).where(
                    Employee.employee_number == values["employee_number"]
                )
            ).scalar_one_or_none()
            if employee is None:
                session.add(Employee(**values))

        admin = session.execute(
            select(UserAccount).where(UserAccount.login_id == "admin")
        ).scalar_one_or_none()
        if admin is None:
            session.add(
                UserAccount(
                    login_id="admin",
                    password_hash=hash_password(admin_password),
                    role_code="SYSTEM_ADMIN",
                )
            )

    print("sample data is ready")


if __name__ == "__main__":
    main()
