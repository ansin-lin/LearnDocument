from rest_framework.permissions import BasePermission, SAFE_METHODS


class EmployeePermission(BasePermission):
    """Map ViewSet actions to Django's built-in model permissions."""

    def has_permission(self, request, view):
        user = request.user
        if not user or not user.is_authenticated:
            return False
        if request.method in SAFE_METHODS:
            return user.has_perm('employees.view_employee')
        if view.action == 'create':
            return user.has_perm('employees.add_employee')
        if view.action in {'update', 'partial_update'}:
            return user.has_perm('employees.change_employee')
        if view.action == 'destroy':
            return user.has_perm('employees.delete_employee')
        if view.action == 'attachments':
            return user.has_perm('employees.add_employeeattachment')
        return False
