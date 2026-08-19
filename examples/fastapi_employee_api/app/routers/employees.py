from fastapi import APIRouter, Depends, Response

from app.dependencies import (
    get_current_user,
    PaginationDep,
    require_admin,
    require_editor,
    SessionDep,
)
from app.schemas import (
    EmployeeCreate,
    EmployeeListResponse,
    EmployeeResponse,
    EmployeeUpdate,
)
from app.services.employee_service import EmployeeService


router = APIRouter(prefix="/api/employees", tags=["employees"])


@router.get(
    "",
    response_model=EmployeeListResponse,
    dependencies=[Depends(get_current_user)],
)
def list_employees(
    pagination: PaginationDep,
    db: SessionDep,
    keyword: str | None = None,
):
    items, total = EmployeeService(db).list_employees(
        keyword,
        pagination.page,
        pagination.size,
    )
    return EmployeeListResponse(
        items=items,
        page=pagination.page,
        size=pagination.size,
        total=total,
    )


@router.get(
    "/{employee_number}",
    response_model=EmployeeResponse,
    dependencies=[Depends(get_current_user)],
)
def get_employee(
    employee_number: str,
    db: SessionDep,
):
    return EmployeeService(db).get_employee(employee_number)


@router.post(
    "",
    response_model=EmployeeResponse,
    status_code=201,
    dependencies=[Depends(require_editor)],
)
def create_employee(
    request: EmployeeCreate,
    db: SessionDep,
):
    return EmployeeService(db).create_employee(
        employee_number=request.employee_number,
        name=request.name,
        department_id=request.department_id,
        email=request.email,
        joined_on=request.joined_on,
    )


@router.put(
    "/{employee_number}",
    response_model=EmployeeResponse,
    dependencies=[Depends(require_editor)],
)
def update_employee(
    employee_number: str,
    request: EmployeeUpdate,
    db: SessionDep,
):
    return EmployeeService(db).update_employee(
        employee_number=employee_number,
        name=request.name,
        department_id=request.department_id,
        email=request.email,
        joined_on=request.joined_on,
    )


@router.delete(
    "/{employee_number}",
    status_code=204,
    dependencies=[Depends(require_admin)],
)
def deactivate_employee(
    employee_number: str,
    db: SessionDep,
):
    EmployeeService(db).deactivate_employee(employee_number)
    return Response(status_code=204)
