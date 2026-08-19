from django.contrib.auth import get_user_model
from django.conf import settings
from django.core.exceptions import ValidationError
from django.core.validators import FileExtensionValidator
from django.db import models


def validate_file_size(file) -> None:
    if file.size > 5 * 1024 * 1024:
        raise ValidationError('文件不能超过 5 MB。')


class Department(models.Model):
    name = models.CharField('部门名', max_length=100, unique=True)
    description = models.TextField('说明', blank=True)

    def __str__(self) -> str:
        return self.name


class Employee(models.Model):
    employee_number = models.CharField('员工编号', max_length=20, unique=True)
    name = models.CharField('姓名', max_length=100)
    department = models.ForeignKey(
        Department,
        on_delete=models.PROTECT,
        related_name='employees',
        verbose_name='部门',
    )
    email = models.EmailField('邮箱', blank=True)
    joined_on = models.DateField('入职日期')
    is_active = models.BooleanField('在职', default=True)

    class Meta:
        ordering = ['employee_number']
        permissions = [
            ('view_inactive_employee', 'Can view inactive employees'),
        ]

    def __str__(self) -> str:
        return f'{self.employee_number} {self.name}'


class EmployeeAttachment(models.Model):
    employee = models.ForeignKey(Employee, on_delete=models.CASCADE, related_name='attachments')
    file = models.FileField(
        upload_to='employee_attachments/%Y/%m/',
        validators=[FileExtensionValidator(['pdf']), validate_file_size],
    )
    original_name = models.CharField(max_length=255)
    uploaded_by = models.ForeignKey(get_user_model(), on_delete=models.PROTECT)
    uploaded_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        ordering = ['-uploaded_at']

    def __str__(self) -> str:
        return self.original_name


class UserDepartmentAccess(models.Model):
    user = models.ForeignKey(
        settings.AUTH_USER_MODEL,
        on_delete=models.CASCADE,
        related_name='department_accesses',
    )
    department = models.ForeignKey(
        Department,
        on_delete=models.CASCADE,
        related_name='user_accesses',
    )

    class Meta:
        constraints = [
            models.UniqueConstraint(
                fields=['user', 'department'],
                name='unique_user_department_access',
            ),
        ]

    def __str__(self) -> str:
        return f'{self.user} → {self.department}'
