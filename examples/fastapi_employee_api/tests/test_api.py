from sqlalchemy import select

from app.models import Employee, UserAccount


def employee_payload(employee_number: str = "E010") -> dict:
    return {
        "employee_number": employee_number,
        "name": "Suzuki",
        "department_id": 1,
        "email": "suzuki@example.test",
        "joined_on": "2026-04-01",
    }


def test_health(client):
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_employee_list_requires_authentication(client):
    response = client.get("/api/employees")

    assert response.status_code == 401


def test_login_rejects_wrong_password(client):
    response = client.post(
        "/api/auth/token",
        data={"username": "admin", "password": "wrong-password"},
    )

    assert response.status_code == 401


def test_disabled_account_cannot_login(client, db_session):
    viewer = db_session.execute(
        select(UserAccount).where(UserAccount.login_id == "viewer")
    ).scalar_one()
    viewer.is_active = False
    db_session.commit()

    response = client.post(
        "/api/auth/token",
        data={"username": "viewer", "password": "test-password"},
    )

    assert response.status_code == 401


def test_department_list_uses_database(client, admin_headers):
    response = client.get(
        "/api/departments",
        headers=admin_headers,
    )

    assert response.status_code == 200
    assert [item["name"] for item in response.json()] == [
        "开发部",
        "营业部",
    ]


def test_create_and_list_employee(
    client,
    db_session,
    admin_headers,
):
    created = client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )
    listed = client.get(
        "/api/employees?keyword=Suzuki&page=1&size=20",
        headers=admin_headers,
    )

    assert created.status_code == 201
    assert created.json()["employee_number"] == "E010"
    assert created.json()["department_id"] == 1
    assert created.json()["department"]["name"] == "开发部"
    assert listed.status_code == 200
    assert listed.json()["total"] == 1
    assert listed.json()["items"][0]["employee_number"] == "E010"

    saved = db_session.execute(
        select(Employee).where(Employee.employee_number == "E010")
    ).scalar_one()
    assert saved.is_active is True


def test_duplicate_employee_number_returns_conflict(
    client,
    admin_headers,
):
    first = client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )
    second = client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )

    assert first.status_code == 201
    assert second.status_code == 409


def test_invalid_department_does_not_create_employee(
    client,
    db_session,
    admin_headers,
):
    payload = employee_payload()
    payload["department_id"] = 999

    response = client.post(
        "/api/employees",
        json=payload,
        headers=admin_headers,
    )

    assert response.status_code == 400
    saved = db_session.execute(
        select(Employee).where(Employee.employee_number == "E010")
    ).scalar_one_or_none()
    assert saved is None


def test_update_employee_changes_database(
    client,
    db_session,
    admin_headers,
):
    client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )
    update_payload = {
        "name": "Suzuki Updated",
        "department_id": 2,
        "email": "suzuki.updated@example.test",
        "joined_on": "2026-05-01",
    }

    response = client.put(
        "/api/employees/E010",
        json=update_payload,
        headers=admin_headers,
    )

    assert response.status_code == 200
    assert response.json()["name"] == "Suzuki Updated"
    assert response.json()["department"]["name"] == "营业部"
    saved = db_session.execute(
        select(Employee).where(Employee.employee_number == "E010")
    ).scalar_one()
    assert saved.department_id == 2
    assert saved.email == "suzuki.updated@example.test"


def test_viewer_cannot_delete_employee(
    client,
    admin_headers,
    viewer_headers,
):
    client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )

    response = client.delete(
        "/api/employees/E010",
        headers=viewer_headers,
    )

    assert response.status_code == 403


def test_hr_staff_can_create_and_update_but_cannot_delete(
    client,
    hr_staff_headers,
):
    created = client.post(
        "/api/employees",
        json=employee_payload(),
        headers=hr_staff_headers,
    )
    updated = client.put(
        "/api/employees/E010",
        json={
            "name": "HR Updated",
            "department_id": 1,
            "email": "hr.updated@example.test",
            "joined_on": "2026-04-02",
        },
        headers=hr_staff_headers,
    )
    deleted = client.delete(
        "/api/employees/E010",
        headers=hr_staff_headers,
    )

    assert created.status_code == 201
    assert updated.status_code == 200
    assert deleted.status_code == 403


def test_delete_is_logical(
    client,
    db_session,
    admin_headers,
):
    client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )

    deleted = client.delete(
        "/api/employees/E010",
        headers=admin_headers,
    )
    listed = client.get(
        "/api/employees",
        headers=admin_headers,
    )

    assert deleted.status_code == 204
    assert listed.json()["total"] == 0
    detail = client.get(
        "/api/employees/E010",
        headers=admin_headers,
    )
    second_delete = client.delete(
        "/api/employees/E010",
        headers=admin_headers,
    )
    assert detail.status_code == 404
    assert second_delete.status_code == 404
    employee = db_session.execute(
        select(Employee).where(Employee.employee_number == "E010")
    ).scalar_one()
    assert employee.is_active is False

    reused = client.post(
        "/api/employees",
        json=employee_payload(),
        headers=admin_headers,
    )
    assert reused.status_code == 409


def test_invalid_page_returns_422(client, admin_headers):
    response = client.get(
        "/api/employees?page=0",
        headers=admin_headers,
    )

    assert response.status_code == 422


def test_invalid_page_size_returns_422(client, admin_headers):
    response = client.get(
        "/api/employees?size=101",
        headers=admin_headers,
    )

    assert response.status_code == 422


def test_invalid_token_returns_401(client):
    response = client.get(
        "/api/employees",
        headers={"Authorization": "Bearer invalid-token"},
    )

    assert response.status_code == 401


def test_business_error_contains_request_id(client, admin_headers):
    response = client.get(
        "/api/employees/UNKNOWN",
        headers=admin_headers,
    )

    assert response.status_code == 404
    assert response.json()["request_id"]
    assert response.headers["X-Request-ID"] == response.json()["request_id"]
