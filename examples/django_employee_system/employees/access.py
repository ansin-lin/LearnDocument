from django.db.models import QuerySet

from .models import Employee, UserDepartmentAccess


def scope_employee_queryset(
    queryset: QuerySet[Employee],
    user,
) -> QuerySet[Employee]:
    if not user or not user.is_authenticated:
        return queryset.none()
    if user.is_superuser:
        return queryset

    department_ids = UserDepartmentAccess.objects.filter(
        user=user,
    ).values_list('department_id', flat=True)
    return queryset.filter(department_id__in=department_ids)
