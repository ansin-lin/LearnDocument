from django import forms

from .models import Employee, EmployeeAttachment


class EmployeeForm(forms.ModelForm):
    class Meta:
        model = Employee
        fields = ['employee_number', 'name', 'department', 'email', 'joined_on']
        widgets = {'joined_on': forms.DateInput(attrs={'type': 'date'})}

    def clean_employee_number(self) -> str:
        value = self.cleaned_data['employee_number'].strip().upper()
        if not value.startswith('E'):
            raise forms.ValidationError('员工编号必须以 E 开头。')
        return value


class EmployeeSearchForm(forms.Form):
    q = forms.CharField(label='关键字', required=False)
    joined_from = forms.DateField(
        label='入职日期 From', required=False, widget=forms.DateInput(attrs={'type': 'date'})
    )
    joined_to = forms.DateField(
        label='入职日期 To', required=False, widget=forms.DateInput(attrs={'type': 'date'})
    )

    def clean(self):
        cleaned_data = super().clean()
        joined_from = cleaned_data.get('joined_from')
        joined_to = cleaned_data.get('joined_to')
        if joined_from and joined_to and joined_from > joined_to:
            raise forms.ValidationError('From 不能晚于 To。')
        return cleaned_data


class AttachmentForm(forms.ModelForm):
    class Meta:
        model = EmployeeAttachment
        fields = ['file']
