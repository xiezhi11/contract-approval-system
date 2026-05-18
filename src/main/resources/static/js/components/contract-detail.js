Vue.component('contract-detail', {
    props: {
        visible: Boolean,
        contractId: [Number, null]
    },
    data() {
        return {
            dialogVisible: false,
            loading: false,
            contract: null,
            attachments: [],
            approvalRecords: []
        };
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
            if (val && this.contractId) {
                this.loadDetail();
            }
        },
        dialogVisible(val) {
            if (!val) {
                this.$emit('close');
            }
        }
    },
    methods: {
        loadDetail() {
            this.loading = true;
            api.contract.get(this.contractId).then(res => {
                if (res.data.code === 200) {
                    this.contract = res.data.data;
                    this.attachments = res.data.data.attachments || [];
                    this.approvalRecords = res.data.data.approvalRecords || [];
                }
            }).finally(() => {
                this.loading = false;
            });
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
        getStatusText,
        getStatusTagType,
        formatAmount,
        formatFileSize
    },
    template: `
        <el-dialog
            title="合同详情"
            :visible.sync="dialogVisible"
            width="900px"
            :close-on-click-modal="false"
            v-loading="loading">
            <div v-if="contract">
                <div class="detail-section">
                    <div class="section-title">基本信息</div>
                    <el-descriptions :column="2" border>
                        <el-descriptions-item label="合同编号">{{ contract.contractNo }}</el-descriptions-item>
                        <el-descriptions-item label="合同名称">{{ contract.contractName }}</el-descriptions-item>
                        <el-descriptions-item label="客户名称">{{ contract.customerName }}</el-descriptions-item>
                        <el-descriptions-item label="合同金额">{{ formatAmount(contract.amount) }}</el-descriptions-item>
                        <el-descriptions-item label="甲方">{{ contract.partyA || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="乙方">{{ contract.partyB || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="合同类型">{{ contract.contractType || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="负责人">{{ contract.responsiblePerson || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="所属部门">{{ contract.department || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="合同状态">
                            <el-tag :type="getStatusTagType(contract.status)">{{ getStatusText(contract.status) }}</el-tag>
                        </el-descriptions-item>
                        <el-descriptions-item label="签订日期">{{ contract.signDate || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="生效日期">{{ contract.effectiveDate || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="到期日期">{{ contract.expiryDate || '-' }}</el-descriptions-item>
                        <el-descriptions-item label="创建人">{{ contract.creator }}</el-descriptions-item>
                        <el-descriptions-item label="创建时间" :span="2">{{ contract.createTime }}</el-descriptions-item>
                        <el-descriptions-item label="合同内容" :span="2">{{ contract.content || '-' }}</el-descriptions-item>
                    </el-descriptions>
                </div>
                <div class="detail-section">
                    <div class="section-title">附件信息</div>
                    <div v-if="attachments.length === 0" style="color: #909399; text-align: center; padding: 20px;">暂无附件</div>
                    <div v-for="attachment in attachments" :key="attachment.id" class="attachment-item">
                        <i :class="getFileIcon(attachment.fileType)" class="file-icon"></i>
                        <div class="file-info">
                            <div class="file-name">{{ attachment.fileName }}</div>
                            <div class="file-meta">{{ formatFileSize(attachment.fileSize) }} · 上传人：{{ attachment.uploader }} · {{ attachment.createTime }}</div>
                        </div>
                        <div class="file-actions">
                            <el-button type="text" size="small" @click="handleDownload(attachment)">下载</el-button>
                        </div>
                    </div>
                </div>
                <div class="detail-section">
                    <div class="section-title">审批记录</div>
                    <el-timeline>
                        <el-timeline-item
                            v-for="(record, index) in approvalRecords"
                            :key="record.id"
                            :timestamp="record.createTime"
                            placement="top"
                            :type="index === 0 ? 'primary' : ''">
                            <h4>
                                <span>{{ record.actionDescription }}</span>
                                <el-tag size="mini" style="margin-left: 10px;">操作人：{{ record.operator }}</el-tag>
                            </h4>
                            <p>{{ record.remark }}</p>
                        </el-timeline-item>
                    </el-timeline>
                    <div v-if="approvalRecords.length === 0" style="color: #909399; text-align: center; padding: 20px;">暂无审批记录</div>
                </div>
            </div>
            <div slot="footer" class="dialog-footer">
                <el-button @click="dialogVisible = false">关闭</el-button>
            </div>
        </el-dialog>
    `
});
