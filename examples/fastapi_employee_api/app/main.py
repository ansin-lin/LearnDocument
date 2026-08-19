import logging
from time import perf_counter
from uuid import uuid4

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse
from sqlalchemy.exc import IntegrityError

from app.config import settings
from app.lifespan import lifespan
from app.logging_config import configure_logging
from app.routers.auth import router as auth_router
from app.routers.departments import router as departments_router
from app.routers.employees import router as employees_router
from app.services.employee_service import (
    DepartmentNotFoundError,
    EmployeeAlreadyExistsError,
    EmployeeNotFoundError,
)


configure_logging()
logger = logging.getLogger(__name__)

app = FastAPI(
    title=settings.app_name,
    lifespan=lifespan,
)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["http://localhost:5173"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
    expose_headers=["X-Request-ID"],
)
app.include_router(auth_router)
app.include_router(departments_router)
app.include_router(employees_router)


@app.middleware("http")
async def add_request_context(
    request: Request,
    call_next,
):
    request_id = uuid4().hex
    request.state.request_id = request_id
    started_at = perf_counter()

    response = await call_next(request)

    elapsed_ms = (perf_counter() - started_at) * 1000
    response.headers["X-Request-ID"] = request_id
    logger.info(
        "request completed request_id=%s method=%s "
        "path=%s status=%s elapsed_ms=%.2f",
        request_id,
        request.method,
        request.url.path,
        response.status_code,
        elapsed_ms,
    )
    return response


def get_request_id(request: Request) -> str:
    return getattr(request.state, "request_id", "unavailable")


@app.get("/health")
def health_check():
    return {"status": "ok"}


@app.exception_handler(EmployeeNotFoundError)
async def handle_employee_not_found(
    request: Request,
    exc: EmployeeNotFoundError,
):
    return JSONResponse(
        status_code=404,
        content={
            "detail": "员工不存在",
            "request_id": get_request_id(request),
        },
    )


@app.exception_handler(EmployeeAlreadyExistsError)
async def handle_employee_conflict(
    request: Request,
    exc: EmployeeAlreadyExistsError,
):
    return JSONResponse(
        status_code=409,
        content={
            "detail": "员工编号已经存在",
            "request_id": get_request_id(request),
        },
    )


@app.exception_handler(DepartmentNotFoundError)
async def handle_department_not_found(
    request: Request,
    exc: DepartmentNotFoundError,
):
    return JSONResponse(
        status_code=400,
        content={
            "detail": "部门不存在",
            "request_id": get_request_id(request),
        },
    )


@app.exception_handler(IntegrityError)
async def handle_integrity_error(
    request: Request,
    exc: IntegrityError,
):
    request_id = get_request_id(request)
    logger.warning(
        "database constraint conflict request_id=%s path=%s",
        request_id,
        request.url.path,
    )
    return JSONResponse(
        status_code=409,
        content={
            "detail": "数据与现有记录冲突",
            "request_id": request_id,
        },
    )


@app.exception_handler(Exception)
async def handle_unexpected_error(
    request: Request,
    exc: Exception,
):
    request_id = get_request_id(request)
    logger.exception(
        "unexpected error request_id=%s path=%s",
        request_id,
        request.url.path,
    )
    return JSONResponse(
        status_code=500,
        content={
            "detail": "系统错误",
            "request_id": request_id,
        },
    )
