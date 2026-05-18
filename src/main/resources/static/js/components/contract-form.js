Vue.component('contract-form', {
    props: {
        visible: Boolean,
        contractId: [Number, null],
        readonly: Boolean
    },
    data() {
        return {
            dialogVisible: false,
            loading: false,
            form: {
                id: null,
                contractNo: '',
                contractName: '',
                customerName: '',
                partyA: '',
                partyB: '',
                amount: null,
                signDate: '',
                effectiveDate: '',
                expiryDate: '',
                responsiblePerson: '',
                department: '',
                contractType: '',
                content: '',
                creator: 'admin'
            },
            contractStatus: '',
            attachments: [],
            rules: {
                contractNo: [{ required: true, message: '请输入合同编号', trigger: 'blur' }],
                contractName: [{ required: true, message: '请输入合同名称', trigger: 'blur' }],
                customerName: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
                amount: [{ required: true, message: '请输入合同金额', trigger: 'blur' }]
            }
        };
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
            if (val) {
                if (this.contractId) {
                    this.loadContract();
                } else {
                    this.resetForm();
                }
            }
        },
        dialogVisible(val) {
            if (!val) {
                this.$emit('close');
            }
        }
    },
    methods: {
        resetForm() {
            this.form = {
                id: null,
                contractNo: '',
                contractName: '',
                customerName: '',
                partyA: '',
                partyB: '',
                amount: null,
                signDate: '',
                effectiveDate: '',
                expiryDate: '',
                responsiblePerson: '',
                department: '',
                contractType: '',
                content: '',
                creator: 'admin'
            };
            this.contractStatus = 'DRAFT';
            this.attachments = [];
        },
        loadContract() {
            this.loading = true;
            api.contract.get(this.contractId).then(res => {
                if (res.data.code === 200) {
                    const data = res.data.data;
                    this.form = {
                        id: data.id,
                        contractNo: data.contractNo,
                        contractName: data.contractName,
                        customerName: data.customerName,
                        partyA: data.partyA,
                        partyB: data.partyB,
                        amount: data.amount,
                        signDate: data.signDate,
                        effectiveDate: data.effectiveDate,
                        expiryDate: data.expiryDate,
                        responsiblePerson: data.responsiblePerson,
                        department: data.department,
                        contractType: data.contractType,
                        content: data.content,
                        creator: data.creator
                    };
                    this.contractStatus = data.status;
                    this.attachments = data.attachments || [];
                }
            }).finally(() => {
                this.loading = false;
            });
        },
        handleSubmit() {
            this.$refs.form.validate(valid => {
                if (!valid) return;
                this.loading = true;
                const request = this.contractId ? api.contract.update(this.form) : api.contract.create(this.form);
                request.then(res => {
                    if (res.data.code === 200) {
                        this.$message.success(this.contractId ? '更新成功' : '创建成功');
                        this.$emit('success', res.data.data);
                        this.dialogVisible = false;
                    }
                }).finally(() => {
                    this.loading = false;
                });
            });
        },
        handleUpload(file) {
            if (!this.contractId && !this.form.id) {
                this.$message.warning('请先保存合同基本信息后再上传附件');
                return false;
            }
            const contractId = this.form.id || this.contractId;
            this.loading = true;
            api.attachment.upload(contractId, file).then(res => {
                if (res.data.code === 200) {
                    this.$message.success('上传成功');
                    this.attachments.push(res.data.data);
                    if (!this.form.id) {
                        this.form.id = this.contractId;
                    }
                }
            }).finally(() => {
                this.loading = false;
            });
            return false;
        },
        handleDeleteAttachment(attachment) {
            this.$confirm('确认删除该附件吗？', '提示', {
                type: 'warning'
            }).then(() => {
                this.loading = true;
                api.attachment.delete(attachment.id).then(res => {
                    if (res.data.code === 200) {
                        this.$message.success('删除成功');
                        this.attachments = this.attachments.filter(a => a.id !== attachment.id);
                    }
                }).finally(() => {
                    this.loading = false;
                });
            }).catch(() => {});
        },
        handleDownload(attachment) {
            window.open(api.attachment.download(attachment.id));
        },
        getFileIcon(fileType) {
            const imageTypes = ['jpg', 'jpeg', 'png', 'gif', 'bmp', 'webp'];
            const docTypes = ['doc', 'docx', 'txt', 'pdf'];
            const excelTypes = ['xls', 'xlsx'];
            if (imageTypes.includes(fileType?.toLowerCase())) return 'el-icon-picture';
            if (docTypes.includes(fileType?.toLowerCase())) return 'el-icon-document';
            if (excelTypes.includes(fileType?.toLowerCase())) return 'el-icon-goods';
            return 'el-icon-folder';
        },
        isEditable() {
            return canEdit(this.contractStatus);
        },
        formatFileSize
    },
    template: `
        <el-dialog
            :title="contractId ? (isEditable() ? '编辑合同' : '查看合同') : '新增合同'"
            :visible.sync="dialogVisible"
            width="800px"
            :close-on-click-modal="false"
            v-loading="loading">
            <el-form ref="form" :model="form" :rules="rules" label-width="100px">
                <el-row :gutter="20">
                    <el-col :span="12">
                        <el-form-item label="合同编号" prop="contractNo">
                            <el-input v-model="form.contractNo" :disabled="!isEditable() && contractId"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同名称" prop="contractName">
                            <el-input v-model="form.contractName" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="客户名称" prop="customerName">
                            <el-input v-model="form.customerName" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同金额" prop="amount">
                            <el-input-number v-model="form.amount" :min="0" :precision="2" style="width: 100%;" :disabled="!isEditable()"></el-input-number>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="甲方">
                            <el-input v-model="form.partyA" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="乙方">
                            <el-input v-model="form.partyB" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="签订日期">
                            <el-date-picker v-model="form.signDate" type="date" value-format="yyyy-MM-dd" style="width: 100%;" :disabled="!isEditable()"></el-date-picker>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="合同类型">
                            <el-select v-model="form.contractType" placeholder="请选择" style="width: 100%;" :disabled="!isEditable()">
                                <el-option label="采购合同" value="采购合同"></el-option>
                                <el-option label="销售合同" value="销售合同"></el-option>
                                <el-option label="服务合同" value="服务合同"></el-option>
                                <el-option label="租赁合同" value="租赁合同"></el-option>
                                <el-option label="其他" value="其他"></el-option>
                            </el-select>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="生效日期">
                            <el-date-picker v-model="form.effectiveDate" type="date" value-format="yyyy-MM-dd" style="width: 100%;" :disabled="!isEditable()"></el-date-picker>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="到期日期">
                            <el-date-picker v-model="form.expiryDate" type="date" value-format="yyyy-MM-dd" style="width: 100%;" :disabled="!isEditable()"></el-date-picker>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="负责人">
                            <el-input v-model="form.responsiblePerson" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="12">
                        <el-form-item label="所属部门">
                            <el-input v-model="form.department" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                    <el-col :span="24">
                        <el-form-item label="合同内容">
                            <el-input type="textarea" v-model="form.content" :rows="4" :disabled="!isEditable()"></el-input>
                        </el-form-item>
                    </el-col>
                </el-row>
            </el-form>
            <div class="attachment-upload">
                <el-divider content-position="left">附件管理</el-divider>
                <el-upload
                    class="upload-demo"
                    action="#"
                    :http-request="handleUpload"
                    :show-file-list="false"
                    :disabled="!isEditable()">
                    <el-button type="primary" icon="el-icon-upload" :disabled="!isEditable()">上传附件</el-button>
                </el-upload>
                <div class="upload-tip">提交审批前请至少上传一个附件</div>
                <div class="attachment-list">
                    <div v-if="attachments.length === 0" style="color: #909399; text-align: center; padding: 20px;">暂无附件</div>
                    <div v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
                        <i :class="getFileIcon(attachment.fileType)" class="file-icon"></i>
                        <div class="file-info">
                            <div class="file-name">{{ attachment.fileName }}</div>
                            <div class="file-meta">{{ formatFileSize(attachment.fileSize) }} · 上传人：{{ attachment.uploader }} · {{ attachment.createTime }}</div>
                        </div>
                        <div class="file-actions">
                            <el-button type="text" size="small" @click="handleDownload(attachment)">下载</el-button>
                            <el-button type="text" size="small" @click="handleDeleteAttachment(attachment)" :disabled="!isEditable()">删除</el-button>
                        </div>
                    </div>
                </div>
            </div>
            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false">取消</el-button>
                <el-button v-if="isEditable() || !contractId" type="primary" @click="handleSubmit" :loading="loading">保存</el-button>
            </div>
        </el-dialog>
    `
});
