from datetime import date

from sqlalchemy.orm import Session

from app.models import Department, Employee
from app.repositories.employee_repository import EmployeeRepository


class EmployeeAlreadyExistsError(Exception):
    pass


class EmployeeNotFoundError(Exception):
    pass


class DepartmentNotFoundError(Exception):
    pass


class EmployeeService:
    def __init__(self, session: Session) -> None:
        self.session = session
        self.repository = EmployeeRepository(session)

    def list_employees(
        self,
        keyword: str | None,
        page: int,
        size: int,
    ) -> tuple[list[Employee], int]:
        offset = (page - 1) * size
        return (
            self.repository.find_active(keyword, offset, size),
            self.repository.count_active(keyword),
        )

    def get_employee(self, employee_number: str) -> Employee:
        employee = self.repository.find_by_number(employee_number)
        if employee is None:
            raise EmployeeNotFoundError(employee_number)
        return employee

    def list_departments(self) -> list[Department]:
        return self.repository.find_departments()

    def _require_department(self, department_id: int) -> Department:
        department = self.repository.find_department(department_id)
        if department is None:
            raise DepartmentNotFoundError(department_id)
        return department

    def create_employee(
        self,
        employee_number: str,
        name: str,
        department_id: int,
        email: str,
        joined_on: date,
    ) -> Employee:
        if self.repository.employee_number_exists(employee_number):
            raise EmployeeAlreadyExistsError(employee_number)
        department = self._require_department(department_id)

        employee = Employee(
            employee_number=employee_number,
            name=name,
            department=department,
            email=email,
            joined_on=joined_on,
        )
        try:
            self.repository.add(employee)
            self.session.commit()
            self.session.refresh(employee)
            return self.get_employee(employee_number)
        except Exception:
            self.session.rollback()
            raise

    def update_employee(
        self,
        employee_number: str,
        name: str,
        department_id: int,
        email: str,
        joined_on: date,
    ) -> Employee:
        employee = self.get_employee(employee_number)
        department = self._require_department(department_id)
        employee.name = name
        employee.department = department
        employee.email = email
        employee.joined_on = joined_on
        try:
            self.session.commit()
            return self.get_employee(employee_number)
        except Exception:
            self.session.rollback()
            raise

    def deactivate_employee(self, employee_number: str) -> None:
        employee = self.get_employee(employee_number)
        employee.is_active = False
        try:
            self.session.commit()
        except Exception:
            self.session.rollback()
            raise
