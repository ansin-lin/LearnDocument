from django.urls import path

from . import views

app_name = 'employees'

urlpatterns = [
    path('', views.employee_list, name='list'),
    path('new/', views.employee_create, name='create'),
    path('<int:employee_id>/', views.employee_detail, name='detail'),
    path('<int:employee_id>/edit/', views.employee_update, name='update'),
    path('<int:employee_id>/delete/', views.employee_delete, name='delete'),
    path('<int:employee_id>/attachments/new/', views.attachment_upload, name='attachment_upload'),
    path('attachments/<int:attachment_id>/download/', views.attachment_download, name='attachment_download'),
]
