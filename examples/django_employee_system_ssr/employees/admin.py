from django.contrib import admin

from .models import Department, Employee, EmployeeAttachment


@admin.register(Department)
class DepartmentAdmin(admin.ModelAdmin):
    search_fields = ['name']


@admin.register(Employee)
class EmployeeAdmin(admin.ModelAdmin):
    list_display = ['employee_number', 'name', 'department', 'joined_on', 'is_active']
    list_filter = ['department', 'is_active']
    search_fields = ['employee_number', 'name', 'email']
    autocomplete_fields = ['department']


@admin.register(EmployeeAttachment)
class EmployeeAttachmentAdmin(admin.ModelAdmin):
    list_display = ['original_name', 'employee', 'uploaded_by', 'uploaded_at']
    search_fields = ['original_name', 'employee__employee_number', 'employee__name']
    readonly_fields = ['uploaded_by', 'uploaded_at']
