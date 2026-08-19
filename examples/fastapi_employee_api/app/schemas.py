from datetime import date

from pydantic import BaseModel, ConfigDict, Field, field_validator


class DepartmentSummary(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    name: str


class EmployeeWrite(BaseModel):
    name: str = Field(min_length=1, max_length=100)
    department_id: int = Field(ge=1)
    email: str = Field(default="", max_length=254)
    joined_on: date


class EmployeeCreate(EmployeeWrite):
    employee_number: str = Field(
        min_length=2,
        max_length=20,
        pattern=r"^E[0-9]+$",
    )

    @field_validator("employee_number", mode="before")
    @classmethod
    def normalize_employee_number(cls, value: str) -> str:
        return value.strip().upper()


class EmployeeUpdate(EmployeeWrite):
    pass


class EmployeeResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    employee_number: str
    name: str
    department_id: int
    email: str
    joined_on: date
    is_active: bool
    department: DepartmentSummary


class EmployeeListResponse(BaseModel):
    items: list[EmployeeResponse]
    page: int
    size: int
    total: int


class TokenResponse(BaseModel):
    access_token: str
    token_type: str = "bearer"
