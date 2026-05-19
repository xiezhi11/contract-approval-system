const mainApp = new Vue({
    el: '#app',
    data() {
        return {
            showLogin: true,
            loggedIn: false,
            currentUser: null,
            loginLoading: false,
            loginForm: {
                username: '',
                password: ''
            },
            loginRules: {
                username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
                password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
            },
            loading: false,
            tableData: [],
            total: 0,
            queryForm: {
                contractNo: '',
                customerName: '',
                status: '',
                responsiblePerson: '',
                minAmount: '',
                maxAmount: '',
                startDate: '',
                endDate: '',
                pageNum: 1,
                pageSize: 10
            },
            formVisible: false,
            formReadonly: false,
            detailVisible: false,
            approvalVisible: false,
            currentContractId: null
        };
    },
    created() {
        window.mainApp = this;
        if (auth.isAuthenticated()) {
            const user = auth.getCurrentUser();
            if (user) {
                this.currentUser = user;
                this.loggedIn = true;
                this.showLogin = false;
                this.loadData();
            }
        }
    },
    methods: {
        handleLogin() {
            this.$refs.loginForm.validate(valid => {
                if (!valid) return;
                this.loginLoading = true;
                auth.login(this.loginForm).then(res => {
                    if (res.data.code === 200) {
                        const { token, user } = res.data.data;
                        auth.setToken(token);
                        auth.setCurrentUser(user);
                        this.currentUser = user;
                        this.loggedIn = true;
                        this.showLogin = false;
                        this.loginForm = { username: '', password: '' };
                        this.$message.success('登录成功');
                        this.loadData();
                    }
                }).finally(() => {
                    this.loginLoading = false;
                });
            });
        },
        handleLogout() {
            this.$confirm('确认退出登录吗？', '提示', {
                type: 'warning'
            }).then(() => {
                auth.logout();
                this.loggedIn = false;
                this.currentUser = null;
                this.showLogin = true;
                this.tableData = [];
                this.$message.success('已退出登录');
            }).catch(() => {});
        },
        loadData() {
            if (!this.loggedIn) return;
            this.loading = true;
            const params = { ...this.queryForm };
            if (params.minAmount !== '') {
                params.minAmount = Number(params.minAmount);
            } else {
                delete params.minAmount;
            }
            if (params.maxAmount !== '') {
                params.maxAmount = Number(params.maxAmount);
            } else {
                delete params.maxAmount;
            }
            api.contract.query(params).then(res => {
                if (res.data.code === 200) {
                    this.tableData = res.data.data.content;
                    this.total = res.data.data.totalElements;
                }
            }).finally(() => {
                this.loading = false;
            });
        },
        handleQuery() {
            this.queryForm.pageNum = 1;
            this.loadData();
        },
        handleReset() {
            this.queryForm = {
                contractNo: '',
                customerName: '',
                status: '',
                responsiblePerson: '',
                minAmount: '',
                maxAmount: '',
                startDate: '',
                endDate: '',
                pageNum: 1,
                pageSize: 10
            };
            this.loadData();
        },
        handleSizeChange(val) {
            this.queryForm.pageSize = val;
            this.loadData();
        },
        handleCurrentChange(val) {
            this.queryForm.pageNum = val;
            this.loadData();
        },
        handleAdd() {
            this.currentContractId = null;
            this.formReadonly = false;
            this.formVisible = true;
        },
        handleEdit(row) {
            this.currentContractId = row.id;
            this.formReadonly = false;
            this.formVisible = true;
        },
        handleView(row) {
            this.currentContractId = row.id;
            this.detailVisible = true;
        },
        handleSubmit(row) {
            this.$confirm('确认提交该合同进入审批流程吗？', '提示', {
                type: 'warning'
            }).then(() => {
                api.contract.submit(row.id).then(res => {
                    if (res.data.code === 200) {
                        this.$message.success('提交成功');
                        this.loadData();
                    }
                });
            }).catch(() => {});
        },
        handleApprove(row) {
            this.currentContractId = row.id;
            this.approvalVisible = true;
        },
        handleWithdraw(row) {
            this.$confirm('确认撤回该合同的审批申请吗？', '提示', {
                type: 'warning'
            }).then(() => {
                api.contract.withdraw(row.id).then(res => {
                    if (res.data.code === 200) {
                        this.$message.success('撤回成功');
                        this.loadData();
                    }
                });
            }).catch(() => {});
        },
        handleArchive(row) {
            this.$confirm('确认归档该合同吗？归档后将无法修改。', '提示', {
                type: 'warning'
            }).then(() => {
                api.contract.archive(row.id).then(res => {
                    if (res.data.code === 200) {
                        this.$message.success('归档成功');
                        this.loadData();
                    }
                });
            }).catch(() => {});
        },
        handleDelete(row) {
            this.$confirm('确认删除该合同吗？删除后无法恢复。', '提示', {
                type: 'warning'
            }).then(() => {
                api.contract.delete(row.id).then(res => {
                    if (res.data.code === 200) {
                        this.$message.success('删除成功');
                        this.loadData();
                    }
                });
            }).catch(() => {});
        },
        handleFormSuccess() {
            this.formVisible = false;
            this.loadData();
        },
        handleApprovalSuccess() {
            this.approvalVisible = false;
            this.loadData();
        },
        getStatusText,
        getStatusTagType,
        getRoleText,
        canEdit,
        canSubmit,
        canApprove,
        canWithdraw,
        canArchive,
        canDelete,
        formatAmount
    }
});
