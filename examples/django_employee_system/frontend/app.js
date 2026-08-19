import {
  ApiError,
  apiDownload,
  apiFetch,
  clearTokens,
  login,
} from './api.js';

const loginForm = document.querySelector('#login-form');
const statusElement = document.querySelector('#status');
const listElement = document.querySelector('#employee-list');
const pageLabel = document.querySelector('#page-label');
const previousButton = document.querySelector('#previous-button');
const nextButton = document.querySelector('#next-button');
const logoutButton = document.querySelector('#logout-button');
const attachmentForm = document.querySelector('#attachment-form');
const attachmentList = document.querySelector('#attachment-list');
const loadAttachmentsButton =
  document.querySelector('#load-attachments-button');

let currentPage = 1;

function showStatus(message) {
  statusElement.textContent = message;
}

function renderEmployees(employees) {
  listElement.replaceChildren();
  for (const employee of employees) {
    const item = document.createElement('li');
    item.textContent =
      `${employee.employee_number} ${employee.name}`
      + ` / ${employee.department_detail.name}`;
    listElement.append(item);
  }
}

async function loadEmployees(page = 1) {
  showStatus('读取中……');
  listElement.replaceChildren();
  try {
    const data = await apiFetch(`/employees/?page=${page}`);
    currentPage = page;
    pageLabel.textContent = `第 ${page} 页 / 共 ${data.count} 件`;
    previousButton.disabled = !data.previous;
    nextButton.disabled = !data.next;
    if (data.results.length === 0) {
      showStatus('没有符合条件的员工');
      return;
    }
    renderEmployees(data.results);
    showStatus('读取成功');
  } catch (error) {
    if (error instanceof ApiError && error.status === 401) {
      showStatus('登录已失效，请重新登录');
    } else if (error instanceof ApiError && error.status === 403) {
      showStatus('当前账号没有查看权限');
    } else {
      showStatus('员工列表读取失败，请检查Network和请求ID');
    }
  }
}

loginForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  try {
    await login(
      document.querySelector('#username').value,
      document.querySelector('#password').value,
    );
    loginForm.reset();
    await loadEmployees(1);
  } catch {
    showStatus('登录失败，请确认账号、密码和用户状态');
  }
});

previousButton.addEventListener('click', () => {
  if (currentPage > 1) loadEmployees(currentPage - 1);
});

nextButton.addEventListener('click', () => {
  loadEmployees(currentPage + 1);
});

logoutButton.addEventListener('click', () => {
  clearTokens();
  listElement.replaceChildren();
  showStatus('已清除本地token');
});

attachmentForm.addEventListener('submit', async (event) => {
  event.preventDefault();
  const employeeId =
    document.querySelector('#attachment-employee-id').value;
  const file = document.querySelector('#attachment-file').files[0];
  const formData = new FormData();
  formData.append('file', file);

  try {
    const attachment = await apiFetch(
      `/employees/${employeeId}/attachments/`,
      {method: 'POST', body: formData},
    );
    showStatus(`附件上传成功：${attachment.original_name}`);
    attachmentForm.reset();
  } catch (error) {
    if (error instanceof ApiError && error.status === 400) {
      showStatus(`附件校验失败：${JSON.stringify(error.body)}`);
    } else if (error instanceof ApiError && error.status === 403) {
      showStatus('当前账号没有上传权限');
    } else {
      showStatus('附件上传失败，请检查Network和请求ID');
    }
  }
});

loadAttachmentsButton.addEventListener('click', async () => {
  const employeeId =
    document.querySelector('#attachment-employee-id').value;
  if (!employeeId) {
    showStatus('请先输入员工ID');
    return;
  }

  try {
    const attachments = await apiFetch(
      `/employees/${employeeId}/attachments/`,
    );
    attachmentList.replaceChildren();
    for (const attachment of attachments) {
      const item = document.createElement('li');
      const button = document.createElement('button');
      button.type = 'button';
      button.textContent = `下载 ${attachment.original_name}`;
      button.addEventListener('click', async () => {
        button.disabled = true;
        showStatus(`正在下载 ${attachment.original_name}`);
        try {
          const blob = await apiDownload(
            `/attachments/${attachment.id}/download/`,
          );
          const url = URL.createObjectURL(blob);
          try {
            const link = document.createElement('a');
            link.href = url;
            link.download = attachment.original_name;
            link.click();
          } finally {
            URL.revokeObjectURL(url);
          }
          showStatus(`已开始下载 ${attachment.original_name}`);
        } catch (error) {
          if (error instanceof ApiError && error.status === 403) {
            showStatus('当前账号没有附件下载权限');
          } else if (error instanceof ApiError && error.status === 404) {
            showStatus('附件不存在或不在当前账号的数据范围内');
          } else {
            showStatus('附件下载失败，请检查Network和请求ID');
          }
        } finally {
          button.disabled = false;
        }
      });
      item.append(button);
      attachmentList.append(item);
    }
    showStatus(`读取到 ${attachments.length} 个附件`);
  } catch {
    showStatus('附件列表读取失败，请检查权限和Network');
  }
});
