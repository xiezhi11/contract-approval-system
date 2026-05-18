const API_BASE = '/api';

const api = {
    contract: {
        query: (params) => axios.post(`${API_BASE}/contract/query`, params),
        get: (id) => axios.get(`${API_BASE}/contract/${id}`),
        create: (data) => axios.post(`${API_BASE}/contract`, data),
        update: (data) => axios.put(`${API_BASE}/contract`, data),
        delete: (id) => axios.delete(`${API_BASE}/contract/${id}`),
        submit: (id, operator = 'admin') => axios.post(`${API_BASE}/contract/${id}/submit`, { operator }),
        approve: (id, operator = 'admin', remark = '') => axios.post(`${API_BASE}/contract/${id}/approve`, { operator, remark }),
        reject: (id, operator = 'admin', remark = '') => axios.post(`${API_BASE}/contract/${id}/reject`, { operator, remark }),
        withdraw: (id, operator = 'admin') => axios.post(`${API_BASE}/contract/${id}/withdraw`, { operator }),
        archive: (id, operator = 'admin') => axios.post(`${API_BASE}/contract/${id}/archive`, { operator })
    },
    attachment: {
        upload: (contractId, file, uploader = 'admin') => {
            const formData = new FormData();
            formData.append('contractId', contractId);
            formData.append('file', file);
            formData.append('uploader', uploader);
            return axios.post(`${API_BASE}/attachment/upload`, formData, {
                headers: { 'Content-Type': 'multipart/form-data' }
            });
        },
        delete: (id) => axios.delete(`${API_BASE}/attachment/${id}`),
        getByContract: (contractId) => axios.get(`${API_BASE}/attachment/contract/${contractId}`),
        download: (id) => `${API_BASE}/attachment/download/${id}`
    }
};

const STATUS_MAP = {
    DRAFT: '草稿',
    PENDING_APPROVAL: '待审批',
    APPROVED: '审批通过',
    REJECTED: '已驳回',
    WITHDRAWN: '已撤回',
    ARCHIVED: '已归档'
};

const STATUS_TAG_TYPE = {
    DRAFT: 'info',
    PENDING_APPROVAL: 'warning',
    APPROVED: 'success',
    REJECTED: 'danger',
    WITHDRAWN: 'info',
    ARCHIVED: 'info'
};

function getStatusText(status) {
    return STATUS_MAP[status] || status;
}

function getStatusTagType(status) {
    return STATUS_TAG_TYPE[status] || 'info';
}

function canEdit(status) {
    return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(status);
}

function canSubmit(status) {
    return ['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(status);
}

function canApprove(status) {
    return status === 'PENDING_APPROVAL';
}

function canWithdraw(status) {
    return status === 'PENDING_APPROVAL';
}

function canArchive(status) {
    return status === 'APPROVED';
}

function canDelete(status) {
    return status === 'DRAFT';
}

function formatAmount(amount) {
    if (amount === null || amount === undefined) return '-';
    return '¥' + Number(amount).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatFileSize(size) {
    if (size === null || size === undefined) return '-';
    if (size < 1024) return size + ' B';
    if (size < 1024 * 1024) return (size / 1024).toFixed(2) + ' KB';
    return (size / (1024 * 1024)).toFixed(2) + ' MB';
}

axios.interceptors.response.use(
    response => {
        return response;
    },
    error => {
        if (error.response && error.response.data) {
            ELEMENT.Message.error(error.response.data.message || '操作失败');
        } else {
            ELEMENT.Message.error('网络错误，请稍后重试');
        }
        return Promise.reject(error);
    }
);
