import logging
from pathlib import Path

from django.contrib import messages
from django.contrib.auth.decorators import login_required, permission_required
from django.core.paginator import Paginator
from django.db.models import Q
from django.http import FileResponse, HttpRequest, HttpResponse
from django.shortcuts import get_object_or_404, redirect, render

from .forms import AttachmentForm, EmployeeForm, EmployeeSearchForm
from .models import Employee, EmployeeAttachment

logger = logging.getLogger(__name__)


def home(request: HttpRequest) -> HttpResponse:
    if request.user.is_authenticated:
        return redirect('employees:list')
    return redirect('login')


def health(request: HttpRequest) -> HttpResponse:
    return HttpResponse('OK')


@login_required
@permission_required('employees.view_employee', raise_exception=True)
def employee_list(request: HttpRequest) -> HttpResponse:
    form = EmployeeSearchForm(request.GET or None)
    employees = Employee.objects.filter(is_active=True).select_related('department')

    if form.is_valid():
        keyword = form.cleaned_data['q'].strip()
        if keyword:
            employees = employees.filter(
                Q(employee_number__icontains=keyword)
                | Q(name__icontains=keyword)
                | Q(department__name__icontains=keyword)
            )
        if form.cleaned_data['joined_from']:
            employees = employees.filter(joined_on__gte=form.cleaned_data['joined_from'])
        if form.cleaned_data['joined_to']:
            employees = employees.filter(joined_on__lte=form.cleaned_data['joined_to'])

    page_obj = Paginator(employees, 10).get_page(request.GET.get('page'))
    query = request.GET.copy()
    query.pop('page', None)
    return render(
        request,
        'employees/list.html',
        {'form': form, 'page_obj': page_obj, 'query_string': query.urlencode()},
    )


@login_required
@permission_required('employees.view_employee', raise_exception=True)
def employee_detail(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(
        Employee.objects.select_related('department').prefetch_related('attachments'),
        pk=employee_id,
        is_active=True,
    )
    return render(request, 'employees/detail.html', {'employee': employee})


@login_required
@permission_required('employees.add_employee', raise_exception=True)
def employee_create(request: HttpRequest) -> HttpResponse:
    if request.method == 'POST':
        form = EmployeeForm(request.POST)
        if form.is_valid():
            employee = form.save()
            messages.success(request, '员工已新增。')
            logger.info('employee_created', extra={'employee_id': employee.pk, 'user_id': request.user.pk})
            return redirect('employees:detail', employee_id=employee.pk)
    else:
        form = EmployeeForm()
    return render(request, 'employees/form.html', {'form': form, 'page_title': '新增员工'})


@login_required
@permission_required('employees.change_employee', raise_exception=True)
def employee_update(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(Employee, pk=employee_id, is_active=True)
    if request.method == 'POST':
        form = EmployeeForm(request.POST, instance=employee)
        if form.is_valid():
            employee = form.save()
            messages.success(request, '员工信息已更新。')
            logger.info('employee_updated', extra={'employee_id': employee.pk, 'user_id': request.user.pk})
            return redirect('employees:detail', employee_id=employee.pk)
    else:
        form = EmployeeForm(instance=employee)
    return render(
        request,
        'employees/form.html',
        {'form': form, 'employee': employee, 'page_title': '编辑员工'},
    )


@login_required
@permission_required('employees.delete_employee', raise_exception=True)
def employee_delete(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(Employee, pk=employee_id, is_active=True)
    if request.method == 'POST':
        employee.is_active = False
        employee.save(update_fields=['is_active'])
        messages.success(request, '员工已设为离职。')
        logger.info('employee_deactivated', extra={'employee_id': employee.pk, 'user_id': request.user.pk})
        return redirect('employees:list')
    return render(request, 'employees/confirm_delete.html', {'employee': employee})


@login_required
@permission_required('employees.add_employeeattachment', raise_exception=True)
def attachment_upload(request: HttpRequest, employee_id: int) -> HttpResponse:
    employee = get_object_or_404(Employee, pk=employee_id, is_active=True)
    if request.method == 'POST':
        form = AttachmentForm(request.POST, request.FILES)
        if form.is_valid():
            attachment = form.save(commit=False)
            attachment.employee = employee
            attachment.uploaded_by = request.user
            attachment.original_name = Path(attachment.file.name.replace('\\', '/')).name
            attachment.save()
            messages.success(request, '附件已上传。')
            return redirect('employees:detail', employee_id=employee.pk)
    else:
        form = AttachmentForm()
    return render(request, 'employees/attachment_form.html', {'form': form, 'employee': employee})


@login_required
@permission_required('employees.view_employee', raise_exception=True)
def attachment_download(request: HttpRequest, attachment_id: int) -> FileResponse:
    attachment = get_object_or_404(EmployeeAttachment, pk=attachment_id, employee__is_active=True)
    safe_name = Path(attachment.original_name.replace('\\', '/')).name
    return FileResponse(attachment.file.open('rb'), as_attachment=True, filename=safe_name)


def permission_denied(request: HttpRequest, exception) -> HttpResponse:
    return render(request, '403.html', status=403)


def page_not_found(request: HttpRequest, exception) -> HttpResponse:
    return render(request, '404.html', status=404)


def server_error(request: HttpRequest) -> HttpResponse:
    return render(request, '500.html', status=500)
