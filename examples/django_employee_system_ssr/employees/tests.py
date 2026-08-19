from datetime import date
from pathlib import Path
from tempfile import TemporaryDirectory

from django.contrib.auth.models import Permission, User
from django.core.files.uploadedfile import SimpleUploadedFile
from django.test import TestCase, override_settings
from django.urls import reverse

from .models import Department, Employee, EmployeeAttachment


class EmployeeViewTests(TestCase):
    @classmethod
    def setUpTestData(cls):
        cls.department = Department.objects.create(name='开发部')
        cls.employee = Employee.objects.create(
            employee_number='E001',
            name='山田太郎',
            department=cls.department,
            email='yamada@example.com',
            joined_on=date(2026, 4, 1),
        )
        Employee.objects.create(
            employee_number='E002',
            name='佐藤花子',
            department=cls.department,
            joined_on=date(2025, 10, 1),
        )
        cls.viewer = User.objects.create_user('viewer', password='test-password-123')
        cls.editor = User.objects.create_user('editor', password='test-password-123')
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
        cls.editor.user_permissions.add(*permissions)

    def test_anonymous_user_is_redirected_to_login(self):
        response = self.client.get(reverse('employees:list'))
        self.assertRedirects(response, f"{reverse('login')}?next={reverse('employees:list')}")

    def test_viewer_can_open_list(self):
        self.client.force_login(self.viewer)
        response = self.client.get(reverse('employees:list'))
        self.assertEqual(response.status_code, 200)
        self.assertContains(response, '山田太郎')
        self.assertRegex(response['X-Request-ID'], r'^[0-9a-f]{32}$')

    def test_keyword_and_date_range_filter_can_be_combined(self):
        self.client.force_login(self.viewer)
        response = self.client.get(
            reverse('employees:list'),
            {'q': '山田', 'joined_from': '2026-01-01', 'joined_to': '2026-12-31'},
        )
        self.assertContains(response, '山田太郎')
        self.assertNotContains(response, '佐藤花子')

    def test_invalid_date_range_shows_error(self):
        self.client.force_login(self.viewer)
        response = self.client.get(
            reverse('employees:list'),
            {'joined_from': '2026-12-31', 'joined_to': '2026-01-01'},
        )
        self.assertContains(response, 'From 不能晚于 To。')

    def test_viewer_cannot_open_create_page(self):
        self.client.force_login(self.viewer)
        response = self.client.get(reverse('employees:create'))
        self.assertEqual(response.status_code, 403)

    def test_editor_can_create_employee(self):
        self.client.force_login(self.editor)
        response = self.client.post(
            reverse('employees:create'),
            {
                'employee_number': 'e003',
                'name': '铃木一郎',
                'department': self.department.pk,
                'email': '',
                'joined_on': '2026-07-01',
            },
        )
        employee = Employee.objects.get(employee_number='E003')
        self.assertRedirects(response, reverse('employees:detail', args=[employee.pk]))

    def test_get_delete_page_does_not_change_employee(self):
        self.client.force_login(self.editor)
        response = self.client.get(reverse('employees:delete', args=[self.employee.pk]))
        self.employee.refresh_from_db()
        self.assertEqual(response.status_code, 200)
        self.assertTrue(self.employee.is_active)

    def test_post_delete_deactivates_employee(self):
        self.client.force_login(self.editor)
        response = self.client.post(reverse('employees:delete', args=[self.employee.pk]))
        self.employee.refresh_from_db()
        self.assertRedirects(response, reverse('employees:list'))
        self.assertFalse(self.employee.is_active)

    def test_inactive_employee_detail_returns_404(self):
        self.client.force_login(self.viewer)
        self.employee.is_active = False
        self.employee.save(update_fields=['is_active'])
        response = self.client.get(reverse('employees:detail', args=[self.employee.pk]))
        self.assertEqual(response.status_code, 404)


class AttachmentTests(TestCase):
    def setUp(self):
        self.media_directory = TemporaryDirectory()
        self.override = override_settings(MEDIA_ROOT=Path(self.media_directory.name))
        self.override.enable()
        department = Department.objects.create(name='开发部')
        self.employee = Employee.objects.create(
            employee_number='E001',
            name='山田太郎',
            department=department,
            joined_on=date(2026, 4, 1),
        )
        self.user = User.objects.create_user('maintainer', password='test-password-123')
        permissions = Permission.objects.filter(
            content_type__app_label='employees',
            codename__in=['view_employee', 'add_employeeattachment'],
        )
        self.user.user_permissions.add(*permissions)
        self.client.force_login(self.user)

    def tearDown(self):
        self.override.disable()
        self.media_directory.cleanup()

    def test_pdf_can_be_uploaded_and_downloaded(self):
        response = self.client.post(
            reverse('employees:attachment_upload', args=[self.employee.pk]),
            {'file': SimpleUploadedFile('resume.pdf', b'%PDF-1.4 test', content_type='application/pdf')},
        )
        attachment = EmployeeAttachment.objects.get()
        self.assertRedirects(response, reverse('employees:detail', args=[self.employee.pk]))
        download = self.client.get(reverse('employees:attachment_download', args=[attachment.pk]))
        self.assertEqual(download.status_code, 200)
        self.assertIn('attachment;', download['Content-Disposition'])
        download.close()

    def test_non_pdf_is_rejected(self):
        response = self.client.post(
            reverse('employees:attachment_upload', args=[self.employee.pk]),
            {'file': SimpleUploadedFile('note.txt', b'test', content_type='text/plain')},
        )
        self.assertEqual(response.status_code, 200)
        self.assertFalse(EmployeeAttachment.objects.exists())
