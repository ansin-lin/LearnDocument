import logging

from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import exception_handler


logger = logging.getLogger('employees.api')


def api_exception_handler(exc, context):
    response = exception_handler(exc, context)
    request = context.get('request')
    request_id = getattr(request, 'request_id', None)

    if response is None:
        logger.error(
            'unhandled_api_exception',
            exc_info=(type(exc), exc, exc.__traceback__),
        )
        return Response(
            {
                'detail': '服务器内部错误。',
                'request_id': request_id,
            },
            status=status.HTTP_500_INTERNAL_SERVER_ERROR,
        )

    if isinstance(response.data, dict):
        response.data.setdefault('request_id', request_id)
    else:
        response.data = {
            'errors': response.data,
            'request_id': request_id,
        }
    return response
