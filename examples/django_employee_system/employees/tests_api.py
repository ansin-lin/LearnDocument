from datetime import date
from tempfile import TemporaryDirectory

from django.contrib.auth.models import Permission, User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APITestCase

from .models import Department, Employee


class EmployeeApiTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        cls.department = Department.objects.create(name='开发部')
        cls.employee = Employee.objects.create(
            employee_number='E001',
            name='山田太郎',
            department=cls.department,
            joined_on=date(2026, 4, 1),
        )
        Employee.objects.create(
            employee_number='E002',
            name='佐藤花子',
            department=cls.department,
            joined_on=date(2025, 10, 1),
        )
        cls.viewer = User.objects.create_user('api-viewer', password='test-password-123')
        cls.maintainer = User.objects.create_user('api-maintainer', password='test-password-123')
        permissions = Permission.objects.filter(
            content_type__app_label='employees',
            codename__in=[
                'view_employee',
                'add_employee',
                'change_employee',
                'delete_employee',
                'add_employeeattachment',
            ],
        )
        cls.viewer.user_permissions.add(permissions.get(codename='view_employee'))
        cls.maintainer.user_permissions.add(*permissions)

    def test_anonymous_user_cannot_list(self):
        response = self.client.get(reverse('employee-list'))
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_viewer_can_list_but_cannot_create(self):
        self.client.force_authenticate(self.viewer)
        self.assertEqual(self.client.get(reverse('employee-list')).status_code, 200)
        response = self.client.post(reverse('employee-list'), {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_maintainer_can_create_and_number_is_normalized(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.post(
            reverse('employee-list'),
            {
                'employee_number': 'e010',
                'name': '田中一郎',
                'department': self.department.pk,
                'joined_on': '2026-05-01',
                'is_active': True,
            },
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['employee_number'], 'E010')

    def test_employee_number_cannot_be_changed(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.patch(
            reverse('employee-detail', args=[self.employee.pk]),
            {'employee_number': 'E999'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.employee.refresh_from_db()
        self.assertEqual(self.employee.employee_number, 'E001')

    def test_filters_search_ordering_and_pagination_can_be_combined(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(
            reverse('employee-list'),
            {
                'department': self.department.pk,
                'joined_from': '2026-01-01',
                'search': '山田',
                'ordering': '-joined_on',
                'page': 1,
            },
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 1)
        self.assertEqual(response.data['results'][0]['employee_number'], 'E001')

    def test_invalid_date_range_returns_400(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(
            reverse('employee-list'),
            {'joined_from': '2026-12-31', 'joined_to': '2026-01-01'},
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_delete_is_logical(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.delete(reverse('employee-detail', args=[self.employee.pk]))
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.employee.refresh_from_db()
        self.assertFalse(self.employee.is_active)

    def test_pdf_can_be_uploaded(self):
        self.client.force_authenticate(self.maintainer)
        pdf = SimpleUploadedFile('resume.pdf', b'%PDF-1.4\n% test\n', content_type='application/pdf')
        with TemporaryDirectory() as media_root:
            with override_settings(MEDIA_ROOT=media_root):
                response = self.client.post(
                    reverse('employee-attachments', args=[self.employee.pk]),
                    {'file': pdf},
                    format='multipart',
                )
                self.assertEqual(response.status_code, status.HTTP_201_CREATED)
