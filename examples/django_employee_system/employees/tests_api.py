from datetime import date
from tempfile import TemporaryDirectory

from django.contrib.auth.models import Permission, User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import override_settings
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIRequestFactory, APITestCase

from company_portal.exceptions import api_exception_handler
from .models import (
    Department,
    Employee,
    EmployeeAttachment,
    UserDepartmentAccess,
)


class EmployeeApiTests(APITestCase):
    @classmethod
    def setUpTestData(cls):
        cls.development = Department.objects.create(name='开发部')
        cls.sales = Department.objects.create(name='营业部')
        cls.employee = Employee.objects.create(
            employee_number='E001',
            name='山田太郎',
            department=cls.development,
            joined_on=date(2026, 4, 1),
        )
        cls.sales_employee = Employee.objects.create(
            employee_number='E002',
            name='佐藤花子',
            department=cls.sales,
            joined_on=date(2025, 10, 1),
        )
        cls.inactive_employee = Employee.objects.create(
            employee_number='E003',
            name='鈴木次郎',
            department=cls.development,
            joined_on=date(2024, 4, 1),
            is_active=False,
        )
        cls.viewer = User.objects.create_user(
            'api-viewer',
            password='test-password-123',
        )
        cls.maintainer = User.objects.create_user(
            'api-maintainer',
            password='test-password-123',
        )
        UserDepartmentAccess.objects.create(
            user=cls.viewer,
            department=cls.development,
        )
        UserDepartmentAccess.objects.create(
            user=cls.maintainer,
            department=cls.development,
        )

        permissions = Permission.objects.filter(
            content_type__app_label='employees',
        )
        cls.viewer.user_permissions.add(
            permissions.get(codename='view_employee'),
        )
        cls.maintainer.user_permissions.add(
            *permissions.filter(
                codename__in=[
                    'view_employee',
                    'add_employee',
                    'change_employee',
                    'delete_employee',
                    'add_employeeattachment',
                ],
            )
        )

        cls.list_url = reverse('employee-list')
        cls.detail_url = reverse(
            'employee-detail',
            args=[cls.employee.pk],
        )
        cls.upload_url = reverse(
            'employee-attachments',
            args=[cls.employee.pk],
        )

    def test_token_can_be_obtained_and_refreshed(self):
        token_response = self.client.post(
            reverse('token-obtain-pair'),
            {
                'username': 'api-viewer',
                'password': 'test-password-123',
            },
            format='json',
        )
        self.assertEqual(token_response.status_code, status.HTTP_200_OK)
        refresh_response = self.client.post(
            reverse('token-refresh'),
            {'refresh': token_response.data['refresh']},
            format='json',
        )
        self.assertEqual(refresh_response.status_code, status.HTTP_200_OK)
        self.assertIn('access', refresh_response.data)

    def test_anonymous_user_cannot_list(self):
        response = self.client.get(self.list_url)
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_unhandled_api_exception_returns_generic_json(self):
        request = APIRequestFactory().get('/api/employees/')
        request.request_id = 'test-request-id'
        response = api_exception_handler(
            RuntimeError('sensitive internal detail'),
            {'request': request},
        )
        self.assertEqual(
            response.status_code,
            status.HTTP_500_INTERNAL_SERVER_ERROR,
        )
        self.assertEqual(response.data['request_id'], 'test-request-id')
        self.assertNotIn('sensitive internal detail', str(response.data))

    def test_viewer_only_sees_assigned_department(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(self.list_url)
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        numbers = {
            item['employee_number']
            for item in response.data['results']
        }
        self.assertEqual(numbers, {'E001'})

    def test_other_department_detail_is_hidden(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(
            reverse('employee-detail', args=[self.sales_employee.pk]),
        )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_viewer_can_list_but_cannot_create(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.post(self.list_url, {}, format='json')
        self.assertEqual(response.status_code, status.HTTP_403_FORBIDDEN)

    def test_maintainer_can_create_and_number_is_normalized(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.post(
            self.list_url,
            {
                'employee_number': 'e010',
                'name': '田中一郎',
                'department': self.development.pk,
                'joined_on': '2026-05-01',
                'is_active': True,
            },
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertEqual(response.data['employee_number'], 'E010')

    def test_maintainer_cannot_create_in_other_department(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.post(
            self.list_url,
            {
                'employee_number': 'E011',
                'name': '越权测试',
                'department': self.sales.pk,
                'joined_on': '2026-05-01',
                'is_active': True,
            },
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(
            Employee.objects.filter(employee_number='E011').exists(),
        )

    def test_employee_number_cannot_be_changed(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.patch(
            self.detail_url,
            {'employee_number': 'E999'},
            format='json',
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.employee.refresh_from_db()
        self.assertEqual(self.employee.employee_number, 'E001')

    def test_filters_keep_department_scope(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(
            self.list_url,
            {
                'department': self.sales.pk,
                'search': '佐藤',
                'ordering': '-joined_on',
                'page': 1,
            },
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 0)
        self.assertEqual(response.data['results'], [])

    def test_user_with_permission_can_filter_inactive_employee(self):
        inactive_permission = Permission.objects.get(
            content_type__app_label='employees',
            codename='view_inactive_employee',
        )
        self.viewer.user_permissions.add(inactive_permission)
        self.client.force_authenticate(self.viewer)
        response = self.client.get(
            self.list_url,
            {'is_active': 'false'},
        )
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['count'], 1)
        self.assertEqual(
            response.data['results'][0]['employee_number'],
            'E003',
        )

    def test_invalid_date_range_returns_400(self):
        self.client.force_authenticate(self.viewer)
        response = self.client.get(
            self.list_url,
            {
                'joined_from': '2026-12-31',
                'joined_to': '2026-01-01',
            },
        )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_delete_is_logical(self):
        self.client.force_authenticate(self.maintainer)
        response = self.client.delete(self.detail_url)
        self.assertEqual(response.status_code, status.HTTP_204_NO_CONTENT)
        self.employee.refresh_from_db()
        self.assertFalse(self.employee.is_active)

    def test_pdf_can_be_uploaded(self):
        self.client.force_authenticate(self.maintainer)
        pdf = SimpleUploadedFile(
            'resume.pdf',
            b'%PDF-1.4\n% test\n',
            content_type='application/pdf',
        )
        with TemporaryDirectory() as media_root:
            with override_settings(MEDIA_ROOT=media_root):
                response = self.client.post(
                    self.upload_url,
                    {'file': pdf},
                    format='multipart',
                )
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)

    def test_fake_pdf_is_rejected(self):
        self.client.force_authenticate(self.maintainer)
        fake_pdf = SimpleUploadedFile(
            'resume.pdf',
            b'plain text',
            content_type='application/pdf',
        )
        with TemporaryDirectory() as media_root:
            with override_settings(MEDIA_ROOT=media_root):
                response = self.client.post(
                    self.upload_url,
                    {'file': fake_pdf},
                    format='multipart',
                )
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_other_department_attachment_download_is_hidden(self):
        self.client.force_authenticate(self.viewer)
        with TemporaryDirectory() as media_root:
            with override_settings(MEDIA_ROOT=media_root):
                attachment = EmployeeAttachment.objects.create(
                    employee=self.sales_employee,
                    file=SimpleUploadedFile(
                        'sales.pdf',
                        b'%PDF-1.4\n% test\n',
                        content_type='application/pdf',
                    ),
                    original_name='sales.pdf',
                    uploaded_by=self.maintainer,
                )
                response = self.client.get(
                    reverse(
                        'api-attachment-download',
                        args=[attachment.pk],
                    )
                )
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)
