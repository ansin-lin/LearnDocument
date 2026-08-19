from fastapi import APIRouter, Depends

from app.dependencies import get_current_user, SessionDep
from app.schemas import DepartmentSummary
from app.services.employee_service import EmployeeService


router = APIRouter(prefix="/api/departments", tags=["departments"])


@router.get(
    "",
    response_model=list[DepartmentSummary],
    dependencies=[Depends(get_current_user)],
)
def list_departments(
    db: SessionDep,
):
    return EmployeeService(db).list_departments()
