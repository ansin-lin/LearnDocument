from sqlalchemy import func, or_, select
from sqlalchemy.orm import Session, selectinload

from app.models import Department, Employee


class EmployeeRepository:
    def __init__(self, session: Session) -> None:
        self.session = session

    def find_active(
        self,
        keyword: str | None,
        offset: int,
        limit: int,
    ) -> list[Employee]:
        statement = (
            select(Employee)
            .options(selectinload(Employee.department))
            .where(Employee.is_active.is_(True))
            .order_by(Employee.employee_number)
            .offset(offset)
            .limit(limit)
        )
        if keyword:
            statement = statement.where(
                or_(
                    Employee.employee_number.contains(keyword),
                    Employee.name.contains(keyword),
                )
            )
        return list(self.session.execute(statement).scalars().all())

    def count_active(self, keyword: str | None) -> int:
        statement = select(func.count(Employee.id)).where(
            Employee.is_active.is_(True)
        )
        if keyword:
            statement = statement.where(
                or_(
                    Employee.employee_number.contains(keyword),
                    Employee.name.contains(keyword),
                )
            )
        return self.session.execute(statement).scalar_one()

    def find_by_number(self, employee_number: str) -> Employee | None:
        statement = (
            select(Employee)
            .options(selectinload(Employee.department))
            .where(
                Employee.employee_number == employee_number,
                Employee.is_active.is_(True),
            )
        )
        return self.session.execute(statement).scalar_one_or_none()

    def employee_number_exists(self, employee_number: str) -> bool:
        statement = select(Employee.id).where(
            Employee.employee_number == employee_number
        )
        return self.session.execute(statement).scalar_one_or_none() is not None

    def add(self, employee: Employee) -> Employee:
        self.session.add(employee)
        self.session.flush()
        return employee

    def find_department(self, department_id: int) -> Department | None:
        return self.session.get(Department, department_id)

    def find_departments(self) -> list[Department]:
        statement = select(Department).order_by(Department.id)
        return list(self.session.execute(statement).scalars().all())
