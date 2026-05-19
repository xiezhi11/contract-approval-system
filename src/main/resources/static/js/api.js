const API_BASE = '/api';

const TOKEN_KEY = 'contract_token';
const USER_KEY = 'contract_user';

const auth = {
    login: (data) => axios.post(`${API_BASE}/auth/login`, data),
    logout: () => {
        localStorage.removeItem(TOKEN_KEY);
        localStorage.removeItem(USER_KEY);
    },
    getToken: () => localStorage.getItem(TOKEN_KEY),
    setToken: (token) => localStorage.setItem(TOKEN_KEY, token),
    getCurrentUser: () => {
        const user = localStorage.getItem(USER_KEY);
        return user ? JSON.parse(user) : null;
    },
    setCurrentUser: (user) => localStorage.setItem(USER_KEY, JSON.stringify(user)),
    isAuthenticated: () => !!localStorage.getItem(TOKEN_KEY),
    hasRole: (role) => {
        const user = auth.getCurrentUser();
        if (!user || !user.roles) return false;
        return user.roles.includes(role);
    }
};

const api = {
    contract: {
        query: (params) => axios.post(`${API_BASE}/contract/query`, params),
        get: (id) => axios.get(`${API_BASE}/contract/${id}`),
        create: (data) => axios.post(`${API_BASE}/contract`, data),
        update: (data) => axios.put(`${API_BASE}/contract`, data),
        delete: (id) => axios.delete(`${API_BASE}/contract/${id}`),
        submit: (id) => axios.post(`${API_BASE}/contract/${id}/submit`),
        approve: (id, remark = '') => axios.post(`${API_BASE}/contract/${id}/approve`, { remark }),
        reject: (id, remark = '') => axios.post(`${API_BASE}/contract/${id}/reject`, { remark }),
        withdraw: (id) => axios.post(`${API_BASE}/contract/${id}/withdraw`),
        archive: (id) => axios.post(`${API_BASE}/contract/${id}/archive`)
    },
    attachment: {
        upload: (contractId, file) => {
            const formData = new FormData();
            formData.append('contractId', contractId);
            formData.append('file', file);
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

const ROLE_MAP = {
    ADMIN: '系统管理员',
    CREATOR: '合同创建人',
    APPROVER: '审批人',
    ARCHIVIST: '归档管理员'
};

function getStatusText(status) {
    return STATUS_MAP[status] || status;
}

function getStatusTagType(status) {
    return STATUS_TAG_TYPE[status] || 'info';
}

function getRoleText(role) {
    return ROLE_MAP[role] || role;
}

function canEdit(status, contract) {
    if (!['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(status)) return false;
    const user = auth.getCurrentUser();
    if (!user) return false;
    if (auth.hasRole('ADMIN')) return true;
    if (contract && (contract.creator === user.username || contract.responsiblePerson === user.username)) return true;
    return false;
}

function canSubmit(status, contract) {
    if (!['DRAFT', 'REJECTED', 'WITHDRAWN'].includes(status)) return false;
    const user = auth.getCurrentUser();
    if (!user) return false;
    if (auth.hasRole('ADMIN')) return true;
    if (contract && (contract.creator === user.username || contract.responsiblePerson === user.username)) return true;
    return false;
}

function canApprove(status) {
    if (status !== 'PENDING_APPROVAL') return false;
    return auth.hasRole('ADMIN') || auth.hasRole('APPROVER');
}

function canWithdraw(status, contract) {
    if (status !== 'PENDING_APPROVAL') return false;
    const user = auth.getCurrentUser();
    if (!user) return false;
    if (auth.hasRole('ADMIN')) return true;
    if (contract && contract.creator === user.username) return true;
    return false;
}

function canArchive(status) {
    if (status !== 'APPROVED') return false;
    return auth.hasRole('ADMIN') || auth.hasRole('ARCHIVIST');
}

function canDelete(status, contract) {
    if (status !== 'DRAFT') return false;
    const user = auth.getCurrentUser();
    if (!user) return false;
    if (auth.hasRole('ADMIN')) return true;
    if (contract && contract.creator === user.username) return true;
    return false;
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

axios.interceptors.request.use(
    config => {
        const token = auth.getToken();
        if (token) {
            config.headers['Authorization'] = 'Bearer ' + token;
        }
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

axios.interceptors.response.use(
    response => {
        return response;
    },
    error => {
        if (error.response) {
            if (error.response.status === 401) {
                auth.logout();
                ELEMENT.Message.error('登录已过期，请重新登录');
                if (window.mainApp) {
                    window.mainApp.showLogin = true;
                    window.mainApp.loggedIn = false;
                    window.mainApp.currentUser = null;
                }
                return Promise.reject(error);
            }
            if (error.response.status === 403) {
                ELEMENT.Message.error('没有权限执行此操作');
                return Promise.reject(error);
            }
            if (error.response.data) {
                ELEMENT.Message.error(error.response.data.message || '操作失败');
            } else {
                ELEMENT.Message.error('网络错误，请稍后重试');
            }
        } else {
            ELEMENT.Message.error('网络错误，请稍后重试');
        }
        return Promise.reject(error);
    }
);
