from pathlib import Path

from django.http import FileResponse, JsonResponse
from django.shortcuts import get_object_or_404
from rest_framework import status, viewsets
from rest_framework.decorators import action
from rest_framework.parsers import FormParser, MultiPartParser
from rest_framework.permissions import IsAuthenticated
from rest_framework.response import Response
from rest_framework.views import APIView
from drf_spectacular.types import OpenApiTypes
from drf_spectacular.utils import extend_schema

from .filters import EmployeeFilter
from .models import Employee, EmployeeAttachment
from .permissions import EmployeePermission
from .serializers import AttachmentSerializer, EmployeeSerializer


def api_health(request):
    if request.method != 'GET':
        return JsonResponse({'detail': 'Method not allowed.'}, status=405)
    return JsonResponse({'status': 'ok', 'service': 'employee-api'})


class EmployeeViewSet(viewsets.ModelViewSet):
    serializer_class = EmployeeSerializer
    permission_classes = [EmployeePermission]
    filterset_class = EmployeeFilter
    search_fields = ['employee_number', 'name', 'department__name']
    ordering_fields = ['employee_number', 'name', 'joined_on']
    ordering = ['employee_number', 'pk']

    def get_queryset(self):
        return Employee.objects.select_related('department').order_by('employee_number', 'pk')

    def destroy(self, request, *args, **kwargs):
        employee = self.get_object()
        employee.is_active = False
        employee.save(update_fields=['is_active'])
        return Response(status=status.HTTP_204_NO_CONTENT)

    @action(
        detail=True,
        methods=['get', 'post'],
        parser_classes=[MultiPartParser, FormParser],
        url_path='attachments',
    )
    def attachments(self, request, pk=None):
        employee = self.get_object()
        if request.method == 'GET':
            if not request.user.has_perm('employees.view_employee'):
                self.permission_denied(request)
            serializer = AttachmentSerializer(employee.attachments.all(), many=True)
            return Response(serializer.data)

        serializer = AttachmentSerializer(data=request.data)
        serializer.is_valid(raise_exception=True)
        uploaded_file = serializer.validated_data['file']
        attachment = serializer.save(
            employee=employee,
            uploaded_by=request.user,
            original_name=Path(uploaded_file.name.replace('\\', '/')).name,
        )
        return Response(AttachmentSerializer(attachment).data, status=status.HTTP_201_CREATED)


class AttachmentDownloadView(APIView):
    permission_classes = [IsAuthenticated]

    @extend_schema(responses={(200, 'application/pdf'): OpenApiTypes.BINARY})
    def get(self, request, attachment_id):
        attachment = get_object_or_404(
            EmployeeAttachment.objects.select_related('employee'),
            pk=attachment_id,
            employee__is_active=True,
        )
        if not request.user.has_perm('employees.view_employee'):
            self.permission_denied(request)
        safe_name = Path(attachment.original_name.replace('\\', '/')).name
        return FileResponse(
            attachment.file.open('rb'),
            as_attachment=True,
            filename=safe_name,
            content_type='application/pdf',
        )
