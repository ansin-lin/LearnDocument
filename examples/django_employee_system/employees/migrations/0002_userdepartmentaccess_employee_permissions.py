import django.db.models.deletion
from django.conf import settings
from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('employees', '0001_initial'),
        migrations.swappable_dependency(settings.AUTH_USER_MODEL),
    ]

    operations = [
        migrations.AlterModelOptions(
            name='employee',
            options={
                'ordering': ['employee_number'],
                'permissions': [
                    (
                        'view_inactive_employee',
                        'Can view inactive employees',
                    ),
                ],
            },
        ),
        migrations.CreateModel(
            name='UserDepartmentAccess',
            fields=[
                (
                    'id',
                    models.BigAutoField(
                        auto_created=True,
                        primary_key=True,
                        serialize=False,
                        verbose_name='ID',
                    ),
                ),
                (
                    'department',
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name='user_accesses',
                        to='employees.department',
                    ),
                ),
                (
                    'user',
                    models.ForeignKey(
                        on_delete=django.db.models.deletion.CASCADE,
                        related_name='department_accesses',
                        to=settings.AUTH_USER_MODEL,
                    ),
                ),
            ],
        ),
        migrations.AddConstraint(
            model_name='userdepartmentaccess',
            constraint=models.UniqueConstraint(
                fields=('user', 'department'),
                name='unique_user_department_access',
            ),
        ),
    ]
