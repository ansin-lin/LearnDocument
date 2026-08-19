import logging
import uuid
from contextvars import ContextVar


request_id_context = ContextVar('request_id', default='-')


class RequestIdFilter(logging.Filter):
    def filter(self, record):
        record.request_id = request_id_context.get()
        record.employee_id = getattr(record, 'employee_id', '-')
        record.user_id = getattr(record, 'user_id', '-')
        return True


class RequestIdMiddleware:
    def __init__(self, get_response):
        self.get_response = get_response

    def __call__(self, request):
        request.request_id = uuid.uuid4().hex
        token = request_id_context.set(request.request_id)
        try:
            response = self.get_response(request)
            response['X-Request-ID'] = request.request_id
            return response
        finally:
            request_id_context.reset(token)
