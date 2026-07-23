from django import forms
import django_filters

from .models import Employee


class EmployeeFilterForm(forms.Form):
    def clean(self):
        cleaned_data = super().clean()
        joined_from = cleaned_data.get('joined_from')
        joined_to = cleaned_data.get('joined_to')
        if joined_from and joined_to and joined_from > joined_to:
            raise forms.ValidationError('From 不能晚于 To。')
        return cleaned_data


class EmployeeFilter(django_filters.FilterSet):
    joined_from = django_filters.DateFilter(field_name='joined_on', lookup_expr='gte')
    joined_to = django_filters.DateFilter(field_name='joined_on', lookup_expr='lte')

    class Meta:
        model = Employee
        fields = ['department', 'is_active']
        form = EmployeeFilterForm
