from pathlib import Path

from rest_framework import serializers

from .models import Department, Employee, EmployeeAttachment


class DepartmentSummarySerializer(serializers.ModelSerializer):
    class Meta:
        model = Department
        fields = ['id', 'name']


class EmployeeSerializer(serializers.ModelSerializer):
    department_detail = DepartmentSummarySerializer(source='department', read_only=True)

    class Meta:
        model = Employee
        fields = [
            'id',
            'employee_number',
            'name',
            'department',
            'department_detail',
            'email',
            'joined_on',
            'is_active',
        ]
        read_only_fields = ['id']

    def validate_employee_number(self, value: str) -> str:
        value = value.strip().upper()
        if not value.startswith('E'):
            raise serializers.ValidationError('员工编号必须以 E 开头。')
        if self.instance and value != self.instance.employee_number:
            raise serializers.ValidationError('员工编号创建后不能修改。')
        return value


class AttachmentSerializer(serializers.ModelSerializer):
    original_name = serializers.CharField(read_only=True)
    uploaded_by = serializers.CharField(source='uploaded_by.username', read_only=True)

    class Meta:
        model = EmployeeAttachment
        fields = ['id', 'file', 'original_name', 'uploaded_by', 'uploaded_at']
        extra_kwargs = {'file': {'write_only': True}}

    def validate_file(self, value):
        if value.size == 0:
            raise serializers.ValidationError('文件不能为空。')
        if value.size > 5 * 1024 * 1024:
            raise serializers.ValidationError('文件不能超过 5 MB。')
        if Path(value.name).suffix.lower() != '.pdf':
            raise serializers.ValidationError('只允许上传 PDF。')
        return value
