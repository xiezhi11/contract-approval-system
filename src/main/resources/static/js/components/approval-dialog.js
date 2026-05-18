Vue.component('approval-dialog', {
    props: {
        visible: Boolean,
        contractId: [Number, null]
    },
    data() {
        return {
            dialogVisible: false,
            loading: false,
            contract: null,
            approvalType: '',
            remark: '',
            rules: {
                remark: [
                    { required: true, message: '驳回时请填写审批意见', trigger: 'blur' }
                ]
            }
        };
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
            if (val && this.contractId) {
                this.loadContract();
            } else {
                this.reset();
            }
        },
        dialogVisible(val) {
            if (!val) {
                this.$emit('close');
            }
        }
    },
    methods: {
        reset() {
            this.contract = null;
            this.approvalType = '';
            this.remark = '';
        },
        loadContract() {
            this.loading = true;
            api.contract.get(this.contractId).then(res => {
                if (res.data.code === 200) {
                    this.contract = res.data.data;
                }
            }).finally(() => {
                this.loading = false;
            });
        },
        handleApprove() {
            if (this.approvalType === 'reject' && !this.remark.trim()) {
                this.$message.warning('驳回时请填写审批意见');
                return;
            }
            this.$confirm(`确认${this.approvalType === 'approve' ? '通过' : '驳回'}该合同吗？`, '提示', {
                type: 'warning'
            }).then(() => {
                this.loading = true;
                const request = this.approvalType === 'approve'
                    ? api.contract.approve(this.contractId, 'admin', this.remark)
                    : api.contract.reject(this.contractId, 'admin', this.remark);
                request.then(res => {
                    if (res.data.code === 200) {
                        this.$message.success('操作成功');
                        this.$emit('success');
                        this.dialogVisible = false;
                    }
                }).finally(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        getStatusText,
        getStatusTagType,
        formatAmount
    },
    template: `
        <el-dialog
            title="合同审批"
            :visible.sync="dialogVisible"
            width="600px"
            :close-on-click-modal="false"
            v-loading="loading">
            <div v-if="contract">
                <el-descriptions :column="1" size="small">
                    <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
                    <el-descriptions-item label="合同名称">{{ contract.contractName }}</el-descriptions-item>
                    <el-descriptions-item label="客户名称">{{ contract.customerName }}</el-descriptions-item>
                    <el-descriptions-item label="合同金额">{{ formatAmount(contract.amount) }}</el-descriptions-item>
                    <el-descriptions-item label="当前状态">
                        <el-tag :type="getStatusTagType(contract.status)">{{ getStatusText(contract.status) }}</el-tag>
                    </el-descriptions-item>
                </el-descriptions>
                <el-form label-width="100px" style="margin-top: 20px;">
                    <el-form-item label="审批结果" required>
                        <el-radio-group v-model="approvalType">
                            <el-radio label="approve">通过</el-radio>
                            <el-radio label="reject">驳回</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="审批意见" :required="approvalType === 'reject'" prop="remark">
                        <el-input
                            type="textarea"
                            v-model="remark"
                            :rows="4"
                            :placeholder="approvalType === 'reject' ? '请填写驳回原因' : '请填写审批意见（选填）'">
                        </el-input>
                    </el-form-item>
                </el-form>
            </div>
            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button type="primary" @click="handleApprove" :loading="loading" :disabled="!approvalType">确认</el-button>
            </div>
        </el-dialog>
    `
});
