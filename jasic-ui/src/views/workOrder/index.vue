<template>
  <div class="app-container">
    <el-card shadow="never" class="search-card">
      <div class="scope-tabs">
        <el-radio-group v-model="queryParams.viewScope" size="small" @change="handleScopeChange">
          <el-radio-button label="CURRENT">当前处理</el-radio-button>
          <el-radio-button label="HISTORY">历史转出/只读</el-radio-button>
        </el-radio-group>
      </div>
      <div class="status-tabs">
        <el-radio-group v-model="activeMainStatus" size="small" @change="handleMainStatusChange">
          <el-radio-button v-for="item in statusTabOptions" :key="item.value" :label="item.value">
            {{ item.label }}（{{ item.count }}）
          </el-radio-button>
        </el-radio-group>
      </div>
      <el-form ref="queryForm" :model="queryParams" :inline="true" size="small">
        <el-form-item label="工单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" placeholder="请输入工单号" clearable />
        </el-form-item>
        <el-form-item label="客户姓名" prop="customerName">
          <el-input v-model="queryParams.customerName" placeholder="请输入客户姓名" clearable />
        </el-form-item>
        <el-form-item label="客户手机号" prop="customerMobile">
          <el-input v-model="queryParams.customerMobile" placeholder="请输入客户手机号" clearable />
        </el-form-item>
        <el-form-item label="条码" prop="barcode">
          <el-input v-model="queryParams.barcode" placeholder="请输入条码" clearable />
        </el-form-item>
        <el-form-item label="是否转单" prop="hasTransfer">
          <el-select v-model="queryParams.hasTransfer" placeholder="全部" clearable>
            <el-option v-for="item in hasTransferOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card shadow="never" class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" icon="el-icon-plus" size="small" v-hasPerms="['workorder:add']" @click="handleAdd">新增工单</el-button>
      </div>
      <el-table v-loading="loading" :data="workOrderList" border stripe>
        <el-table-column label="工单号" prop="orderNo" min-width="160" show-overflow-tooltip />
        <el-table-column label="客户姓名" prop="customerName" min-width="110" />
        <el-table-column label="客户手机号" prop="customerMobile" min-width="130" />
        <el-table-column label="条码" prop="barcode" min-width="150" show-overflow-tooltip />
        <el-table-column label="机型" prop="productModel" min-width="130" show-overflow-tooltip />
        <el-table-column label="状态" min-width="110" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="statusTag(row.mainStatus)" size="mini">
              {{ row.mainStatusLabel || statusLabel(row.mainStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前受理公司" prop="currentAcceptCompanyName" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前维修员" prop="assignedUserName" min-width="110" />
        <el-table-column label="转单" min-width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.hasTransfer === 1 ? 'warning' : 'info'" size="mini">
              {{ row.hasTransfer === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" fixed="right" width="90">
          <template slot-scope="{ row }">
            <el-button type="text" size="mini" @click="handleView(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        class="pagination"
        :current-page="queryParams.pageNum"
        :page-size="queryParams.pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>

    <el-dialog title="新增工单" :visible.sync="createDialogVisible" width="720px" append-to-body>
      <div class="create-entry-tabs">
        <el-radio-group v-model="createForm.entryMode" size="small" @change="handleCreateEntryModeChange">
          <el-radio-button v-for="item in createEntryOptions" :key="item.value" :label="item.value">
            {{ item.label }}
          </el-radio-button>
        </el-radio-group>
      </div>
      <el-alert
        title="代客户填写支持有码/无码；上级报修客户信息由当前登录账号兜底，无码时会加载默认总部建单信息。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="96px">
        <el-row :gutter="16">
          <el-col v-if="showCreateCustomerFields" :span="12">
            <el-form-item label="客户姓名" prop="customerName">
              <el-input v-model="createForm.customerName" placeholder="请输入客户姓名，不填则回退客户手机号" />
            </el-form-item>
          </el-col>
          <el-col v-if="showCreateCustomerFields" :span="12">
            <el-form-item label="客户手机号" prop="customerMobile">
              <el-input v-model="createForm.customerMobile" placeholder="请输入客户手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="机器条码" prop="barcode">
              <el-input
                v-model="createForm.barcode"
                placeholder="有码请输入机器条码；无码可直接点右侧加载信息"
                @keyup.enter.native="queryCreateBarcodeInfo"
              >
                <el-button
                  slot="append"
                  icon="el-icon-search"
                  :loading="createBarcodeLoading"
                  @click="queryCreateBarcodeInfo"
                >
                  加载信息
                </el-button>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col v-if="showCreateTargetCompany" :span="24">
            <el-form-item :label="createTargetCompanyLabel" prop="targetCompanyId">
              <el-select
                v-model="createForm.targetCompanyId"
                placeholder="请选择"
                filterable
                :disabled="isCreateTargetAutoFilled"
                @change="handleCreateTargetCompanyChange"
              >
                <el-option
                  v-for="item in createForm.targetCompanyOptions"
                  :key="item.id"
                  :label="item.companyName"
                  :value="item.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <template v-if="hasCreateBarcodeResult">
            <el-col :span="12">
              <el-form-item label="归属总部">
                <el-input :value="createForm.hqCompanyName" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="质保状态">
                <el-input :value="createForm.warrantyStatus" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="物料编码">
                <el-input :value="createForm.productCode" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="商品名称">
                <el-input :value="createForm.productName" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="机型">
                <el-input :value="createForm.productModel" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="机器小号">
                <el-input :value="createForm.machineNo" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="品牌编码">
                <el-input :value="createForm.brandCode" disabled />
              </el-form-item>
            </el-col>
          </template>
          <el-col :span="12">
            <el-form-item label="服务方式" prop="serviceMode">
              <el-select v-model="createForm.serviceMode" placeholder="请选择服务方式">
                <el-option v-for="item in serviceModeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="故障描述" prop="faultItems">
              <el-select
                v-model="createForm.faultItems"
                multiple
                collapse-tags
                :placeholder="createFaultPlaceholder"
                :disabled="!hasCreateBarcodeResult || !createForm.faultOptions.length"
              >
                <el-option
                  v-for="item in createForm.faultOptions"
                  :key="item"
                  :label="item"
                  :value="item"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col v-if="showCreateFaultRemark" :span="24">
            <el-form-item label="故障备注" prop="faultRemark">
              <el-input v-model="createForm.faultRemark" type="textarea" :rows="3" placeholder="请输入故障备注" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="故障图片">
              <file-upload-field
                v-model="createForm.faultImageFiles"
                accept=".jpg,.jpeg,.png,.webp"
                :size-limit-mb="10"
                button-text="上传故障图片"
                tip="支持 jpg/jpeg/png/webp，单文件 10MB"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="故障视频">
              <file-upload-field
                v-model="createForm.faultVideoFiles"
                accept=".mp4,.mov"
                :size-limit-mb="50"
                button-text="上传故障视频"
                tip="支持 mp4/mov，单文件 50MB"
              />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="故障语音">
              <file-upload-field
                v-model="createForm.faultVoiceFiles"
                accept=".mp3,.wav,.amr,.aac"
                :size-limit-mb="10"
                button-text="上传故障语音"
                tip="支持 mp3/wav/amr/aac，单文件 10MB"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="isCreateMailMode" class="section-title">寄修信息</div>
        <el-row v-if="isCreateMailMode" :gutter="16">
          <el-col :span="24">
            <el-form-item label="公司地址簿">
              <div class="inline-actions">
                <el-select
                  v-model="createForm.companyAddressId"
                  placeholder="请选择公司地址"
                  filterable
                  :loading="companyAddressLoading"
                  @change="handleCreateCompanyAddressChange"
                >
                  <el-option
                    v-for="item in companyAddressList"
                    :key="item.id"
                    :label="formatCompanyAddressLabel(item)"
                    :value="item.id"
                  />
                </el-select>
                <el-button icon="el-icon-refresh" @click="loadCompanyAddressList({ preserveSelection: true })">刷新</el-button>
                <el-button
                  type="primary"
                  plain
                  @click="openCompanyAddressDialog"
                >
                  管理地址簿
                </el-button>
              </div>
            </el-form-item>
            <el-alert
              v-if="!companyAddressLoading && !companyAddressList.length"
              title="当前公司还没有可用地址，请先维护公司地址簿后再提交寄修工单。"
              type="warning"
              :closable="false"
              show-icon
            />
          </el-col>
          <el-col :span="12">
            <el-form-item label="寄件人" prop="senderName">
              <el-input v-model="createForm.senderName" disabled placeholder="请选择公司地址簿" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="寄件手机号" prop="senderMobile">
              <el-input v-model="createForm.senderMobile" disabled placeholder="请选择公司地址簿" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="寄件地址" prop="senderAddress">
              <el-input v-model="createForm.senderAddress" disabled placeholder="请选择公司地址簿" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="寄件单号" prop="sendExpressNo">
              <el-input v-model="createForm.sendExpressNo" placeholder="首次建单可不填，后续可补录" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="寄件凭证">
              <file-upload-field
                v-model="createForm.senderVoucherFiles"
                accept=".jpg,.jpeg,.png,.webp"
                :size-limit-mb="10"
                button-text="上传寄件凭证"
                tip="支持 jpg/jpeg/png/webp，单文件 10MB"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog title="公司地址簿" :visible.sync="companyAddressDialogVisible" width="760px" append-to-body>
      <div class="table-toolbar">
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openCompanyAddressForm()">新增地址</el-button>
      </div>
      <el-table :data="companyAddressList" border stripe v-loading="companyAddressLoading">
        <el-table-column label="默认" width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag v-if="row.isDefault === 1" type="success" size="mini">默认</el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="联系人" prop="contactName" min-width="120" />
        <el-table-column label="联系电话" prop="contactPhone" min-width="140" />
        <el-table-column label="详细地址" prop="address" min-width="280" show-overflow-tooltip />
        <el-table-column label="操作" width="220" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              v-if="row.isDefault !== 1"
              type="text"
              size="mini"
              @click="handleSetDefaultCompanyAddress(row)"
            >
              设默认
            </el-button>
            <el-button type="text" size="mini" @click="openCompanyAddressForm(row)">编辑</el-button>
            <el-button type="text" size="mini" @click="handleDeleteCompanyAddress(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer">
        <el-button @click="companyAddressDialogVisible = false">关闭</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="companyAddressFormTitle" :visible.sync="companyAddressFormVisible" width="520px" append-to-body>
      <el-form :model="companyAddressForm" label-width="96px">
        <el-form-item label="联系人">
          <el-input v-model="companyAddressForm.contactName" placeholder="请输入联系人" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="companyAddressForm.contactPhone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="详细地址">
          <el-input v-model="companyAddressForm.address" type="textarea" :rows="3" placeholder="请输入详细地址" />
        </el-form-item>
        <el-form-item label="默认地址">
          <el-switch v-model="companyAddressForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="companyAddressFormVisible = false">取消</el-button>
        <el-button type="primary" :loading="companyAddressSubmitting" @click="submitCompanyAddress">保存</el-button>
      </div>
    </el-dialog>

    <el-drawer title="工单详情" :visible.sync="detailVisible" size="70%" direction="rtl">
      <div v-loading="detailLoading" class="drawer-body">
        <template v-if="detail">
          <div class="drawer-actions">
            <el-button
              v-for="item in detailActionButtons"
              :key="item.action"
              :type="item.type"
              size="mini"
              @click="handleAction(item.action)"
            >
              {{ item.label }}
            </el-button>
          </div>

          <div class="section-title">基础信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="工单号">{{ textValue(detail.orderNo) }}</el-descriptions-item>
            <el-descriptions-item label="主状态">{{ textValue(detail.mainStatusLabel || statusLabel(detail.mainStatus)) }}</el-descriptions-item>
            <el-descriptions-item label="客户姓名">{{ textValue(detail.customerName) }}</el-descriptions-item>
            <el-descriptions-item label="客户手机号">{{ textValue(detail.customerMobile) }}</el-descriptions-item>
            <el-descriptions-item label="品牌类型">{{ textValue(detail.brandTypeLabel || brandTypeLabel(detail.brandType)) }}</el-descriptions-item>
            <el-descriptions-item label="条码">{{ textValue(detail.barcode) }}</el-descriptions-item>
            <el-descriptions-item label="物料编码">{{ textValue(detail.productCode) }}</el-descriptions-item>
            <el-descriptions-item label="机型">{{ textValue(detail.productModel) }}</el-descriptions-item>
            <el-descriptions-item label="品牌编码">{{ textValue(detail.brandCode) }}</el-descriptions-item>
            <el-descriptions-item label="品牌名称">{{ textValue(detail.brandName) }}</el-descriptions-item>
            <el-descriptions-item label="服务方式">{{ textValue(detail.serviceModeLabel || serviceModeLabel(detail.serviceMode)) }}</el-descriptions-item>
            <el-descriptions-item label="质保状态">{{ textValue(detail.warrantyStatus) }}</el-descriptions-item>
            <el-descriptions-item label="当前受理公司">{{ textValue(detail.currentAcceptCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="当前维修员">{{ textValue(detail.assignedUserName) }}</el-descriptions-item>
            <el-descriptions-item label="建单公司">{{ textValue(detail.createCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="建单入口">{{ textValue(createEntryTypeLabel(detail.createEntryType)) }}</el-descriptions-item>
            <el-descriptions-item label="归属总部">{{ textValue(detail.hqCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="转单次数">{{ detail.transferCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="评价状态">{{ textValue(detail.evaluateStatusLabel || evaluateStatusLabel(detail.evaluateStatus)) }}</el-descriptions-item>
            <el-descriptions-item label="故障描述" :span="2">{{ textValue(detail.faultDesc) }}</el-descriptions-item>
            <el-descriptions-item label="故障备注" :span="2">{{ textValue(detail.faultRemark) }}</el-descriptions-item>
          </el-descriptions>

          <div v-if="detail.serviceMode === serviceModeMail" class="section-title">寄修信息</div>
          <el-descriptions v-if="detail.serviceMode === serviceModeMail" :column="2" border size="small">
            <el-descriptions-item label="寄件人">{{ textValue(detail.senderName) }}</el-descriptions-item>
            <el-descriptions-item label="寄件手机号">{{ textValue(detail.senderMobile) }}</el-descriptions-item>
            <el-descriptions-item label="寄件单号">{{ textValue(detail.sendExpressNo) }}</el-descriptions-item>
            <el-descriptions-item label="寄件地址" :span="2">{{ textValue(detail.senderAddress) }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">关闭信息</div>
          <el-descriptions :column="2" border size="small">
            <el-descriptions-item label="返回方式">{{ textValue(detail.returnMethod) }}</el-descriptions-item>
            <el-descriptions-item label="回寄单号">{{ textValue(detail.returnExpressNo) }}</el-descriptions-item>
            <el-descriptions-item label="完成时间">{{ textValue(detail.completedTime) }}</el-descriptions-item>
            <el-descriptions-item label="关闭时间">{{ textValue(detail.closedTime) }}</el-descriptions-item>
            <el-descriptions-item label="关闭原因" :span="2">{{ textValue(detail.closeReason) }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">附件信息</div>
          <el-descriptions :column="1" border size="small">
            <el-descriptions-item label="故障图片">
              <div class="file-link-list">
                <span v-if="!(detail.faultImageFiles || []).length">-</span>
                <el-link
                  v-for="item in detail.faultImageFiles || []"
                  :key="`fault-image-${item.fileId}`"
                  type="primary"
                  :underline="false"
                  @click="openFilePreview(item)"
                >
                  {{ fileDisplayName(item) }}<span v-if="item.fileSize">（{{ formatFileSize(item.fileSize) }}）</span>
                </el-link>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="故障视频">
              <div class="file-link-list">
                <span v-if="!(detail.faultVideoFiles || []).length">-</span>
                <el-link
                  v-for="item in detail.faultVideoFiles || []"
                  :key="`fault-video-${item.fileId}`"
                  type="primary"
                  :underline="false"
                  @click="openFilePreview(item)"
                >
                  {{ fileDisplayName(item) }}<span v-if="item.fileSize">（{{ formatFileSize(item.fileSize) }}）</span>
                </el-link>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="故障语音">
              <div class="file-link-list">
                <span v-if="!(detail.faultVoiceFiles || []).length">-</span>
                <el-link
                  v-for="item in detail.faultVoiceFiles || []"
                  :key="`fault-voice-${item.fileId}`"
                  type="primary"
                  :underline="false"
                  @click="openFilePreview(item)"
                >
                  {{ fileDisplayName(item) }}<span v-if="item.fileSize">（{{ formatFileSize(item.fileSize) }}）</span>
                </el-link>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="寄件凭证">
              <div class="file-link-list">
                <span v-if="!(detail.senderVoucherFiles || []).length">-</span>
                <el-link
                  v-for="item in detail.senderVoucherFiles || []"
                  :key="`sender-voucher-${item.fileId}`"
                  type="primary"
                  :underline="false"
                  @click="openFilePreview(item)"
                >
                  {{ fileDisplayName(item) }}<span v-if="item.fileSize">（{{ formatFileSize(item.fileSize) }}）</span>
                </el-link>
              </div>
            </el-descriptions-item>
            <el-descriptions-item label="回寄凭证">
              <div class="file-link-list">
                <span v-if="!(detail.returnVoucherFiles || []).length">-</span>
                <el-link
                  v-for="item in detail.returnVoucherFiles || []"
                  :key="`return-voucher-${item.fileId}`"
                  type="primary"
                  :underline="false"
                  @click="openFilePreview(item)"
                >
                  {{ fileDisplayName(item) }}<span v-if="item.fileSize">（{{ formatFileSize(item.fileSize) }}）</span>
                </el-link>
              </div>
            </el-descriptions-item>
          </el-descriptions>

          <div class="section-title">参与方</div>
          <el-table :data="detail.participants || []" border size="small">
            <el-table-column label="公司" prop="companyName" min-width="160" />
            <el-table-column label="主体类型" prop="subjectType" min-width="100" />
            <el-table-column label="参与类型" prop="participateType" min-width="120" />
            <el-table-column label="当前处理方" min-width="100">
              <template slot-scope="{ row }">{{ yesNoText(row.isCurrentHandler) }}</template>
            </el-table-column>
            <el-table-column label="首次参与时间" prop="firstParticipateTime" min-width="160" />
            <el-table-column label="最后参与时间" prop="lastParticipateTime" min-width="160" />
          </el-table>

          <div class="section-title">流转历史</div>
          <el-table :data="detail.flows || []" border size="small">
            <el-table-column label="动作" prop="actionName" min-width="120" />
            <el-table-column label="前状态" prop="beforeStatusName" min-width="110" />
            <el-table-column label="后状态" prop="afterStatusName" min-width="110" />
            <el-table-column label="来源公司" prop="fromCompanyName" min-width="150" show-overflow-tooltip />
            <el-table-column label="目标公司" prop="toCompanyName" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作公司" prop="operatorCompanyName" min-width="150" show-overflow-tooltip />
            <el-table-column label="操作人" prop="operatorUserName" min-width="110" />
            <el-table-column label="备注" prop="remark" min-width="220" show-overflow-tooltip />
            <el-table-column label="创建时间" prop="createTime" min-width="160" />
          </el-table>

          <div class="section-title">报价记录</div>
          <el-table :data="detail.quotes || []" border size="small">
            <el-table-column label="报价公司" prop="companyName" min-width="160" />
            <el-table-column label="报价人" prop="quotedByName" min-width="110" />
            <el-table-column label="故障判断" prop="faultJudge" min-width="180" show-overflow-tooltip />
            <el-table-column label="报价金额" prop="quoteAmount" min-width="110" />
            <el-table-column label="报价说明" prop="quoteDesc" min-width="220" show-overflow-tooltip />
            <el-table-column label="当前有效" min-width="90">
              <template slot-scope="{ row }">{{ yesNoText(row.isCurrentValid) }}</template>
            </el-table-column>
            <el-table-column label="创建时间" prop="createTime" min-width="160" />
          </el-table>

          <div class="section-title">维修记录</div>
          <div v-for="repair in detail.repairs || []" :key="repair.id" class="repair-card">
            <el-descriptions :column="2" border size="small">
              <el-descriptions-item label="维修公司">{{ textValue(repair.companyName) }}</el-descriptions-item>
              <el-descriptions-item label="维修人">{{ textValue(repair.repairUserName) }}</el-descriptions-item>
              <el-descriptions-item label="维修摘要">{{ textValue(repair.repairSummary) }}</el-descriptions-item>
              <el-descriptions-item label="维修完成">{{ yesNoText(repair.isFinished) }}</el-descriptions-item>
              <el-descriptions-item label="维修说明" :span="2">{{ textValue(repair.repairDesc) }}</el-descriptions-item>
              <el-descriptions-item label="其他说明" :span="2">{{ textValue(repair.otherDesc) }}</el-descriptions-item>
            </el-descriptions>
            <el-table v-if="repair.faults && repair.faults.length" :data="repair.faults" border size="small" class="inner-table">
              <el-table-column label="故障点" prop="faultDesc" min-width="160" />
              <el-table-column label="维修说明" prop="repairDesc" min-width="180" show-overflow-tooltip />
              <el-table-column label="其他维修说明" prop="otherDesc" min-width="180" show-overflow-tooltip />
              <el-table-column label="配件信息" prop="partDesc" min-width="180" show-overflow-tooltip />
              <el-table-column label="图片地址" prop="imageUrls" min-width="200" show-overflow-tooltip />
              <el-table-column label="登记人" prop="createdByName" min-width="110" />
              <el-table-column label="创建时间" prop="createTime" min-width="160" />
            </el-table>
          </div>

          <div class="section-title">复检记录</div>
          <el-table :data="detail.reviews || []" border size="small">
            <el-table-column label="复检公司" prop="companyName" min-width="160" />
            <el-table-column label="复检人" prop="reviewUserName" min-width="110" />
            <el-table-column label="复检结果" prop="reviewResult" min-width="120" />
            <el-table-column label="继续维修" min-width="90">
              <template slot-scope="{ row }">{{ yesNoText(row.isContinueRepair) }}</template>
            </el-table-column>
            <el-table-column label="复检说明" prop="reviewDesc" min-width="220" show-overflow-tooltip />
            <el-table-column label="创建时间" prop="createTime" min-width="160" />
          </el-table>

          <div v-if="detail.evaluation" class="section-title">客户评价</div>
          <el-descriptions v-if="detail.evaluation" :column="2" border size="small">
            <el-descriptions-item label="服务时效">{{ textValue(detail.evaluation.timelinessScore) }}</el-descriptions-item>
            <el-descriptions-item label="维修质量">{{ textValue(detail.evaluation.qualityScore) }}</el-descriptions-item>
            <el-descriptions-item label="服务满意度">{{ textValue(detail.evaluation.satisfactionScore) }}</el-descriptions-item>
            <el-descriptions-item label="评价时间">{{ textValue(detail.evaluation.createTime) }}</el-descriptions-item>
            <el-descriptions-item label="标签" :span="2">{{ textValue(detail.evaluation.tags) }}</el-descriptions-item>
            <el-descriptions-item label="评价内容" :span="2">{{ textValue(detail.evaluation.content) }}</el-descriptions-item>
          </el-descriptions>

          <div class="section-title">通知事件</div>
          <el-table :data="detail.notifyEvents || []" border size="small">
            <el-table-column label="归属公司" prop="companyName" min-width="150" />
            <el-table-column label="事件类型" prop="eventType" min-width="140" />
            <el-table-column label="触发节点" prop="triggerNode" min-width="140" />
            <el-table-column label="接收对象" prop="receiverType" min-width="120" />
            <el-table-column label="发送状态" prop="sendStatus" min-width="120" />
            <el-table-column label="标题快照" prop="titleSnapshot" min-width="180" show-overflow-tooltip />
            <el-table-column label="内容快照" prop="contentSnapshot" min-width="220" show-overflow-tooltip />
            <el-table-column label="失败原因" prop="failReason" min-width="180" show-overflow-tooltip />
            <el-table-column label="发送时间" prop="sendTime" min-width="160" />
          </el-table>
        </template>
      </div>
    </el-drawer>

    <el-dialog :title="actionDialogTitle" :visible.sync="actionDialogVisible" width="640px" append-to-body @close="closeActionDialog">
      <el-form ref="actionForm" :model="actionForm" label-width="100px">
        <template v-if="actionDialogAction === 'ASSIGN'">
          <el-form-item label="维修员" required>
            <el-select v-model="actionForm.assignedUserId" placeholder="请选择维修员" filterable>
              <el-option v-for="item in assignUserOptions" :key="item.id" :label="item.realName" :value="item.id" />
            </el-select>
          </el-form-item>
        </template>

        <template v-else-if="actionDialogAction === 'TRANSFER'">
          <el-form-item label="目标公司" required>
            <el-select v-model="actionForm.targetCompanyId" placeholder="请选择目标公司" filterable>
              <el-option v-for="item in transferTargetOptions" :key="item.id" :label="item.companyName" :value="item.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="转单备注">
            <el-input v-model="actionForm.remark" type="textarea" :rows="3" placeholder="请输入转单备注" />
          </el-form-item>
        </template>

        <template v-else-if="actionDialogAction === 'TECH_ACCEPT'">
          <el-form-item label="故障判断" required>
            <el-select v-model="actionForm.faultJudge" placeholder="请选择故障判断" style="width: 100%;" @change="handleTechAcceptFaultJudgeChange">
              <el-option v-for="item in faultJudgeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <template v-if="actionForm.faultJudge === faultJudgeHasFault">
            <el-form-item label="报价金额">
              <el-input-number v-model="actionForm.quoteAmount" :min="0" :precision="2" :controls="false" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="报价说明">
              <el-input v-model="actionForm.quoteDesc" type="textarea" :rows="3" placeholder="请输入报价说明" />
            </el-form-item>
          </template>
          <el-alert
            v-else-if="actionForm.faultJudge === faultJudgeNoFault"
            title="选择无故障后，下一步将进入关闭工单弹窗填写返回方式和关闭原因"
            type="warning"
            :closable="false"
            show-icon
          />
        </template>

        <template v-else-if="actionDialogAction === 'QUOTE'">
          <el-form-item label="故障判断">
            <el-select v-model="actionForm.faultJudge" placeholder="请选择故障判断" style="width: 100%;">
              <el-option v-for="item in faultJudgeOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="报价金额">
            <el-input-number v-model="actionForm.quoteAmount" :min="0" :precision="2" :controls="false" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="报价说明">
            <el-input v-model="actionForm.quoteDesc" type="textarea" :rows="3" placeholder="请输入报价说明" />
          </el-form-item>
        </template>

        <template v-else-if="actionDialogAction === 'REPAIR_SAVE' || actionDialogAction === 'REPAIR_FINISH'">
          <el-alert
            v-if="!actionForm.faultJudge"
            title="当前暂无有效报价；如需调整报价，请先通过“报价”动作确认故障判断"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-form-item label="故障判断">
            <el-input :value="actionForm.faultJudge || '请先在报价动作中确认故障判断'" disabled />
          </el-form-item>
          <el-form-item label="报价金额">
            <el-input-number v-model="actionForm.quoteAmount" :min="0" :precision="2" :controls="false" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="报价说明">
            <el-input v-model="actionForm.quoteDesc" type="textarea" :rows="3" placeholder="请输入报价说明" />
          </el-form-item>
          <el-form-item label="维修摘要">
            <el-input v-model="actionForm.repairSummary" placeholder="请输入维修摘要" />
          </el-form-item>
          <el-form-item label="维修说明">
            <el-input v-model="actionForm.repairDesc" type="textarea" :rows="3" placeholder="请输入维修说明" />
          </el-form-item>
          <el-form-item label="其他说明">
            <el-input v-model="actionForm.otherDesc" type="textarea" :rows="3" placeholder="请输入其他说明" />
          </el-form-item>
          <el-alert
            v-if="actionRepairConfigLoading"
            title="正在加载故障与维修配置"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-alert
            v-else-if="!actionRepairFaultOptions.length"
            title="当前产品未匹配故障与维修配置，可手工填写故障描述和维修说明"
            type="warning"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-alert
            v-else
            title="当前产品已匹配故障与维修配置，可直接选择故障描述和维修说明"
            type="success"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <div class="fault-header">
            <span>故障点</span>
            <el-button type="text" @click="addFaultItem">新增故障点</el-button>
          </div>
          <div v-for="(item, index) in actionForm.faults" :key="index" class="fault-item">
            <div class="fault-item__toolbar">
              <span>故障点 {{ index + 1 }}</span>
              <el-button v-if="actionForm.faults.length > 1" type="text" style="color: #f56c6c;" @click="removeFaultItem(index)">删除</el-button>
            </div>
            <el-form-item label="故障描述">
              <el-select
                v-model="item.faultDesc"
                placeholder="请选择或输入故障描述"
                filterable
                clearable
                :allow-create="!hasRepairFaultConfig"
                default-first-option
                style="width: 100%;"
                @change="handleFaultDescChange(item)"
              >
                <el-option
                  v-for="option in actionRepairFaultOptions"
                  :key="option.faultDesc"
                  :label="option.faultDesc"
                  :value="option.faultDesc"
                />
              </el-select>
            </el-form-item>
            <el-form-item v-if="getRepairOptions(item).length" label="维修说明">
              <el-select
                v-model="item.repairItems"
                multiple
                collapse-tags
                placeholder="请选择维修说明"
                filterable
                style="width: 100%;"
              >
                <el-option
                  v-for="repairOption in getRepairOptions(item)"
                  :key="`${item.faultDesc}-${repairOption}`"
                  :label="repairOption"
                  :value="repairOption"
                />
                <el-option :label="otherRepairOption" :value="otherRepairOption" />
              </el-select>
            </el-form-item>
            <el-form-item v-else-if="!hasRepairFaultConfig" label="维修说明">
              <el-input v-model="item.repairDesc" type="textarea" :rows="2" placeholder="请输入维修说明" />
            </el-form-item>
            <el-form-item v-else label="维修说明">
              <el-input value="请先选择配置内的故障描述" disabled />
            </el-form-item>
            <el-form-item v-if="isOtherRepairSelected(item)" label="其他维修说明">
              <el-input v-model="item.otherDesc" type="textarea" :rows="2" placeholder="请输入其他维修说明" />
            </el-form-item>
            <el-form-item label="配件信息">
              <el-input v-model="item.partDesc" placeholder="请输入配件信息" />
            </el-form-item>
            <el-form-item label="图片地址">
              <el-input v-model="item.imageUrls" placeholder="请输入图片地址，多个地址用逗号分隔" />
            </el-form-item>
          </div>
        </template>

        <template v-else-if="actionDialogAction === 'REVIEW'">
          <el-form-item label="复检结果" required>
            <el-select v-model="actionForm.reviewResult" placeholder="请选择复检结果">
              <el-option label="通过" value="通过" />
              <el-option label="继续维修" value="继续维修" />
            </el-select>
          </el-form-item>
          <el-form-item label="复检说明">
            <el-input v-model="actionForm.reviewDesc" type="textarea" :rows="3" placeholder="请输入复检说明" />
          </el-form-item>
        </template>

        <template v-else-if="actionDialogAction === 'UPLOAD_SEND_EXPRESS'">
          <el-form-item label="寄件单号" required>
            <el-input v-model="actionForm.sendExpressNo" placeholder="请输入寄件快递单号" />
          </el-form-item>
          <el-form-item label="寄件凭证">
            <file-upload-field
              v-model="actionForm.senderVoucherFiles"
              accept=".jpg,.jpeg,.png,.webp"
              :size-limit-mb="10"
              button-text="上传寄件凭证"
              tip="支持 jpg/jpeg/png/webp，单文件 10MB"
            />
          </el-form-item>
        </template>

        <template v-else-if="actionDialogAction === 'CLOSE'">
          <el-form-item label="返回方式" required>
            <el-select v-model="actionForm.returnMethod" placeholder="请选择返回方式">
              <el-option label="自提" value="自提" />
              <el-option label="回寄" value="回寄" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="actionForm.returnMethod === '回寄'" label="回寄单号" required>
            <el-input v-model="actionForm.returnExpressNo" placeholder="请输入回寄快递单号" />
          </el-form-item>
          <el-form-item v-if="actionForm.returnMethod === '回寄'" label="回寄凭证">
            <file-upload-field
              v-model="actionForm.returnVoucherFiles"
              accept=".jpg,.jpeg,.png,.webp"
              :size-limit-mb="10"
              button-text="上传回寄凭证"
              tip="支持 jpg/jpeg/png/webp，单文件 10MB"
            />
          </el-form-item>
          <el-form-item label="关闭原因" required>
            <el-input v-model="actionForm.closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
          </el-form-item>
        </template>
      </el-form>
      <div slot="footer">
        <el-button @click="closeActionDialog">取消</el-button>
        <el-button type="primary" :loading="actionSubmitting" @click="submitAction">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  assignWorkOrder,
  countWorkOrderStatus,
  closeWorkOrder,
  createProxyWorkOrder,
  createUpstreamFirstWorkOrder,
  createUpstreamHqWorkOrder,
  getWorkOrder,
  getProxyCreateBarcodeInfo,
  getUpstreamFirstCreateBarcodeInfo,
  getUpstreamHqCreateBarcodeInfo,
  listAssignUserOptions,
  listRepairFaultOptions,
  listTransferTargetOptions,
  listWorkOrder,
  quoteWorkOrder,
  repairWorkOrder,
  reviewWorkOrder,
  techAcceptWorkOrder,
  transferWorkOrder,
  updateWorkOrderSendExpress
} from '@/api/workOrder'
import {
  createCompanyAddress,
  deleteCompanyAddress,
  listCompanyAddress,
  setDefaultCompanyAddress,
  updateCompanyAddress
} from '@/api/companyAddress'
import FileUploadField from '@/components/Upload/FileUploadField.vue'

const SERVICE_MODE_MAIL = 'MAIL'
const SERVICE_MODE_STORE = 'STORE'
const SERVICE_MODE_LABELS = {
  [SERVICE_MODE_MAIL]: '寄修',
  [SERVICE_MODE_STORE]: '到店维修'
}
const SERVICE_MODE_OPTIONS = [
  { label: SERVICE_MODE_LABELS[SERVICE_MODE_STORE], value: SERVICE_MODE_STORE },
  { label: SERVICE_MODE_LABELS[SERVICE_MODE_MAIL], value: SERVICE_MODE_MAIL }
]
const RETURN_METHOD_MAIL = '回寄'
const RETURN_METHOD_PICKUP = '自提'
const FAULT_JUDGE_HAS_FAULT = '有故障'
const FAULT_JUDGE_NO_FAULT = '无故障'
const REVIEW_RESULT_PASS = '通过'
const REVIEW_RESULT_CONTINUE = '继续维修'
const OTHER_REPAIR_OPTION = '其它维修说明'
const DEFAULT_OTHER_FAULT_LABEL = '其它故障'
const CREATE_ENTRY_PROXY = 'PROXY_SELF'
const CREATE_ENTRY_UPSTREAM_FIRST = 'UPSTREAM_FIRST'
const CREATE_ENTRY_UPSTREAM_HQ = 'UPSTREAM_HQ'
const BRAND_TYPE_JASIC = 'JASIC'
const BRAND_TYPE_NON_JASIC = 'NON_JASIC'

const STATUS_LABELS = {
  WAIT_ACCEPT: '待接单',
  PENDING_ASSIGN: '待派单',
  PENDING_TECH_ACCEPT: '待接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
}

const EVALUATE_STATUS_LABELS = {
  NOT_OPEN: '未开启评价',
  PENDING_EVALUATE: '待评价',
  EVALUATED: '已评价'
}

const ACTION_META = {
  ASSIGN: { label: '派单', title: '派单', type: 'primary' },
  TECH_ACCEPT: { label: '维修员接单', title: '维修员接单', type: 'primary' },
  TRANSFER: { label: '转单', title: '转单', type: 'warning' },
  QUOTE: { label: '报价', title: '报价', type: 'primary' },
  REPAIR_SAVE: { label: '保存维修', title: '保存维修登记', type: 'primary' },
  REPAIR_FINISH: { label: '维修完成', title: '提交维修完成', type: 'success' },
  REVIEW: { label: '复检', title: '复检登记', type: 'warning' },
  UPLOAD_SEND_EXPRESS: { label: '上传寄件单号', title: '上传寄件单号', type: 'primary' },
  CLOSE: { label: '关闭工单', title: '关闭工单', type: 'danger' }
}

function createFaultItem() {
  return {
    faultDesc: '',
    repairDesc: '',
    repairItems: [],
    otherDesc: '',
    partDesc: '',
    imageUrls: ''
  }
}

function buildDefaultQuery() {
  return {
    pageNum: 1,
    pageSize: 10,
    viewScope: 'CURRENT',
    orderNo: '',
    customerName: '',
    customerMobile: '',
    barcode: '',
    mainStatus: '',
    hasTransfer: undefined
  }
}

function buildDefaultCreateForm() {
  return {
    entryMode: CREATE_ENTRY_PROXY,
    customerName: '',
    customerMobile: '',
    barcode: '',
    queriedBarcode: '',
    barcodeQueried: false,
    productCode: '',
    productName: '',
    productModel: '',
    machineNo: '',
    brandCode: '',
    serviceMode: SERVICE_MODE_STORE,
    warrantyStatus: '',
    hqCompanyId: undefined,
    hqCompanyName: '',
    targetCompanyId: undefined,
    targetCompanyOptions: [],
    faultOptions: [],
    otherFaultLabel: DEFAULT_OTHER_FAULT_LABEL,
    faultItems: [],
    faultRemark: '',
    faultImageFiles: [],
    faultVideoFiles: [],
    faultVoiceFiles: [],
    companyAddressId: undefined,
    senderName: '',
    senderMobile: '',
    senderAddress: '',
    sendExpressNo: '',
    senderVoucherFiles: []
  }
}

function buildDefaultCompanyAddressForm() {
  return {
    id: undefined,
    contactName: '',
    contactPhone: '',
    address: '',
    isDefault: 0
  }
}

function formatCompanyAddressLabel(address) {
  if (!address) {
    return ''
  }
  const tags = []
  if (address.isDefault === 1) {
    tags.push('默认')
  }
  tags.push(address.contactName || '-')
  tags.push(address.contactPhone || '-')
  tags.push(address.address || '-')
  return tags.join(' / ')
}

function buildFileIdList(fileList) {
  return (fileList || [])
    .map(item => item && item.fileId)
    .filter(item => item !== null && item !== undefined && item !== '')
}

function cloneFileItems(fileList) {
  return (fileList || []).map(item => ({ ...item }))
}

function formatFileSizeText(size) {
  if (size === null || size === undefined || size === '') {
    return ''
  }
  const numericSize = Number(size)
  if (Number.isNaN(numericSize)) {
    return ''
  }
  if (numericSize < 1024) {
    return `${numericSize} B`
  }
  if (numericSize < 1024 * 1024) {
    return `${(numericSize / 1024).toFixed(1)} KB`
  }
  return `${(numericSize / (1024 * 1024)).toFixed(1)} MB`
}

function normalizeText(value) {
  return value === null || value === undefined ? '' : String(value).trim()
}

function buildDefaultActionForm() {
  return {
    workOrderId: undefined,
    assignedUserId: undefined,
    targetCompanyId: undefined,
    remark: '',
    faultJudge: '',
    quoteAmount: undefined,
    quoteDesc: '',
    repairSummary: '',
    repairDesc: '',
    otherDesc: '',
    isFinished: 0,
    faults: [createFaultItem()],
    reviewResult: REVIEW_RESULT_PASS,
    reviewDesc: '',
    isContinueRepair: 0,
    sendExpressNo: '',
    senderVoucherFiles: [],
    returnMethod: RETURN_METHOD_PICKUP,
    returnExpressNo: '',
    returnVoucherFiles: [],
    closeReason: ''
  }
}

export default {
  name: 'WorkOrderManage',
  components: {
    FileUploadField
  },
  data() {
    return {
      serviceModeMail: SERVICE_MODE_MAIL,
      faultJudgeHasFault: FAULT_JUDGE_HAS_FAULT,
      faultJudgeNoFault: FAULT_JUDGE_NO_FAULT,
      otherRepairOption: OTHER_REPAIR_OPTION,
      loading: false,
      workOrderList: [],
      total: 0,
      queryParams: buildDefaultQuery(),
      statusCountMap: {
        ALL: 0,
        PENDING_ASSIGN: 0,
        PENDING_TECH_ACCEPT: 0,
        IN_PROGRESS: 0,
        COMPLETED: 0,
        CLOSED: 0
      },
      mainStatusOptions: [
        { label: '待派单', value: 'PENDING_ASSIGN' },
        { label: '待接单', value: 'PENDING_TECH_ACCEPT' },
        { label: '维修中', value: 'IN_PROGRESS' },
        { label: '已完成', value: 'COMPLETED' },
        { label: '已关闭', value: 'CLOSED' }
      ],
      hasTransferOptions: [
        { label: '是', value: 1 },
        { label: '否', value: 0 }
      ],
      faultJudgeOptions: [FAULT_JUDGE_HAS_FAULT, FAULT_JUDGE_NO_FAULT],
      serviceModeOptions: SERVICE_MODE_OPTIONS,
      createDialogVisible: false,
      createSubmitting: false,
      createBarcodeLoading: false,
      companyAddressLoading: false,
      companyAddressList: [],
      companyAddressDialogVisible: false,
      companyAddressSubmitting: false,
      companyAddressFormVisible: false,
      companyAddressFormTitle: '新增地址',
      companyAddressForm: buildDefaultCompanyAddressForm(),
      createForm: buildDefaultCreateForm(),
      createRules: {
        customerMobile: [{
          validator: (rule, value, callback) => {
            if (this.createForm.entryMode === CREATE_ENTRY_PROXY && !normalizeText(value)) {
              callback(new Error('请输入客户手机号'))
              return
            }
            callback()
          },
          trigger: 'blur'
        }],
        serviceMode: [{ required: true, message: '请选择服务方式', trigger: 'change' }]
      },
      detailVisible: false,
      detailLoading: false,
      detail: null,
      actionDialogVisible: false,
      actionDialogAction: '',
      actionDialogTitle: '',
      actionSubmitting: false,
      actionForm: buildDefaultActionForm(),
      pendingTechAcceptPayload: null,
      assignUserOptions: [],
      transferTargetOptions: [],
      actionRepairConfigLoading: false,
      actionRepairFaultOptions: []
    }
  },
  computed: {
    hasRepairFaultConfig() {
      return (this.actionRepairFaultOptions || []).length > 0
    },
    currentUserInfo() {
      return this.$store.getters.userInfo || {}
    },
    currentTypeCode() {
      return this.currentUserInfo.currentTypeCode || ''
    },
    isCreateUpstreamEntry() {
      return this.createForm.entryMode !== CREATE_ENTRY_PROXY
    },
    createEntryOptions() {
      const options = [{ value: CREATE_ENTRY_PROXY, label: '代客户填写' }]
      if (this.currentTypeCode === 'SITE_SECOND') {
        options.push({ value: CREATE_ENTRY_UPSTREAM_FIRST, label: '报修一级' })
      } else if (this.currentTypeCode === 'SITE_FIRST') {
        options.push({ value: CREATE_ENTRY_UPSTREAM_HQ, label: '报修佳士' })
      }
      return options
    },
    showCreateCustomerFields() {
      return !this.isCreateUpstreamEntry
    },
    isCreateMailMode() {
      return this.createForm.serviceMode === SERVICE_MODE_MAIL
    },
    hasCreateBarcodeResult() {
      return !!this.createForm.barcodeQueried
    },
    showCreateTargetCompany() {
      return this.createForm.entryMode !== CREATE_ENTRY_PROXY && (this.createForm.targetCompanyOptions || []).length > 0
    },
    createTargetCompanyLabel() {
      return this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST ? '目标一级' : '目标总部'
    },
    isCreateTargetAutoFilled() {
      return (this.createForm.targetCompanyOptions || []).length <= 1
    },
    showCreateFaultRemark() {
      return (this.createForm.faultItems || []).includes(this.createForm.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL)
    },
    createFaultPlaceholder() {
      if (!this.hasCreateBarcodeResult) {
        return '请先完成条码查询并确认目标公司'
      }
      if (!(this.createForm.faultOptions || []).length) {
        return '无码场景可不选故障描述'
      }
      return '请选择故障描述'
    },
    activeMainStatus: {
      get() {
        return this.queryParams.mainStatus || 'ALL'
      },
      set(value) {
        this.queryParams.mainStatus = value === 'ALL' ? '' : value
      }
    },
    statusTabOptions() {
      return [
        { value: 'ALL', label: '全部', count: this.statusCountMap.ALL || 0 },
        { value: 'PENDING_ASSIGN', label: '待派单', count: this.statusCountMap.PENDING_ASSIGN || 0 },
        { value: 'PENDING_TECH_ACCEPT', label: '待接单', count: this.statusCountMap.PENDING_TECH_ACCEPT || 0 },
        { value: 'IN_PROGRESS', label: '维修中', count: this.statusCountMap.IN_PROGRESS || 0 },
        { value: 'COMPLETED', label: '已完成', count: this.statusCountMap.COMPLETED || 0 },
        { value: 'CLOSED', label: '已关闭', count: this.statusCountMap.CLOSED || 0 }
      ]
    },
    detailActionButtons() {
      const actions = new Set((this.detail && this.detail.availableActions) || [])
      if (this.shouldShowAssignFallback()) {
        actions.add('ASSIGN')
      }
      return Array.from(actions)
        .filter(action => action !== 'RETURN_METHOD' && ACTION_META[action])
        .map(action => ({
          action,
          label: ACTION_META[action].label,
          title: ACTION_META[action].title,
          type: ACTION_META[action].type
        }))
    }
  },
  watch: {
    'createForm.barcode'(value) {
      const normalizedBarcode = normalizeText(value)
      if (!normalizedBarcode) {
        this.resetCreateQueryState()
        return
      }
      if (this.createForm.barcodeQueried && normalizedBarcode !== this.createForm.queriedBarcode) {
        this.resetCreateQueryState()
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    formatCompanyAddressLabel(address) {
      return formatCompanyAddressLabel(address)
    },
    getList() {
      this.loading = true
      return Promise.all([
        listWorkOrder(this.queryParams),
        countWorkOrderStatus(this.buildStatusCountParams())
      ]).then(([listRes, countRes]) => {
        if (listRes) {
          const page = listRes.data || {}
          this.workOrderList = page.records || []
          this.total = page.total || 0
        }
        if (countRes) {
          this.syncStatusCountMap(countRes.data || [])
        }
      }).finally(() => {
        this.loading = false
      })
    },
    buildStatusCountParams() {
      return {
        viewScope: this.queryParams.viewScope,
        orderNo: this.queryParams.orderNo,
        customerName: this.queryParams.customerName,
        customerMobile: this.queryParams.customerMobile,
        barcode: this.queryParams.barcode,
        hasTransfer: this.queryParams.hasTransfer
      }
    },
    syncStatusCountMap(list) {
      const nextMap = {
        ALL: 0,
        PENDING_ASSIGN: 0,
        PENDING_TECH_ACCEPT: 0,
        IN_PROGRESS: 0,
        COMPLETED: 0,
        CLOSED: 0
      }
      ;(list || []).forEach(item => {
        if (!item || !item.mainStatus) {
          return
        }
        nextMap[item.mainStatus] = item.countNum || 0
      })
      this.statusCountMap = nextMap
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleScopeChange() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleMainStatusChange() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      if (this.$refs.queryForm) {
        this.$refs.queryForm.resetFields()
      }
      this.queryParams = buildDefaultQuery()
      this.getList()
    },
    handleSizeChange(value) {
      this.queryParams.pageSize = value
      this.getList()
    },
    handleCurrentChange(value) {
      this.queryParams.pageNum = value
      this.getList()
    },
    handleAdd() {
      const defaultEntryMode = (this.createEntryOptions[0] && this.createEntryOptions[0].value) || CREATE_ENTRY_PROXY
      this.createForm = buildDefaultCreateForm()
      this.createForm.entryMode = defaultEntryMode
      this.createDialogVisible = true
      this.loadCompanyAddressList()
      this.$nextTick(() => {
        if (this.$refs.createForm) {
          this.$refs.createForm.clearValidate()
        }
      })
    },
    handleCreateEntryModeChange(entryMode) {
      const nextForm = buildDefaultCreateForm()
      nextForm.entryMode = entryMode || CREATE_ENTRY_PROXY
      nextForm.customerName = this.createForm.customerName
      nextForm.customerMobile = this.createForm.customerMobile
      nextForm.barcode = this.createForm.barcode
      nextForm.serviceMode = this.createForm.serviceMode
      nextForm.companyAddressId = this.createForm.companyAddressId
      nextForm.senderName = this.createForm.senderName
      nextForm.senderMobile = this.createForm.senderMobile
      nextForm.senderAddress = this.createForm.senderAddress
      nextForm.sendExpressNo = this.createForm.sendExpressNo
      nextForm.senderVoucherFiles = cloneFileItems(this.createForm.senderVoucherFiles)
      this.createForm = nextForm
      this.$nextTick(() => {
        if (this.$refs.createForm) {
          this.$refs.createForm.clearValidate()
        }
      })
    },
    handleCreateTargetCompanyChange() {
      if (!this.createForm.barcodeQueried) {
        return
      }
      this.queryCreateBarcodeInfo({ preserveTargetSelection: true, silentSuccess: true })
    },
    queryCreateBarcodeInfo(options = {}) {
      const barcode = normalizeText(this.createForm.barcode)
      const request = this.resolveCreateBarcodeInfoRequest(barcode)
      if (!request) {
        this.$message.error('当前建单入口不支持查条码')
        return
      }
      this.createBarcodeLoading = true
      this.resetCreateQueryState({ preserveTargetSelection: !!options.preserveTargetSelection })
      request.then(res => {
        if (!res) {
          return
        }
        this.applyCreateBarcodeInfo(res.data || {}, barcode)
        if (!options.silentSuccess) {
          this.$message.success(barcode ? '条码查询成功' : '建单信息加载成功')
        }
      }).finally(() => {
        this.createBarcodeLoading = false
      })
    },
    resolveCreateBarcodeInfoRequest(barcode) {
      if (this.createForm.entryMode === CREATE_ENTRY_PROXY) {
        return getProxyCreateBarcodeInfo({ barcode })
      }
      if (this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST) {
        return getUpstreamFirstCreateBarcodeInfo({ barcode })
      }
      if (this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_HQ) {
        const params = { barcode }
        if (this.createForm.targetCompanyId) {
          params.targetCompanyId = this.createForm.targetCompanyId
        }
        return getUpstreamHqCreateBarcodeInfo(params)
      }
      return null
    },
    resetCreateQueryState(options = {}) {
      const preserveTargetSelection = !!options.preserveTargetSelection
      Object.assign(this.createForm, {
        queriedBarcode: '',
        barcodeQueried: false,
        productCode: '',
        productName: '',
        productModel: '',
        machineNo: '',
        brandCode: '',
        warrantyStatus: '',
        hqCompanyId: undefined,
        hqCompanyName: '',
        targetCompanyId: preserveTargetSelection ? this.createForm.targetCompanyId : undefined,
        targetCompanyOptions: preserveTargetSelection ? (this.createForm.targetCompanyOptions || []) : [],
        faultOptions: [],
        otherFaultLabel: DEFAULT_OTHER_FAULT_LABEL,
        faultItems: [],
        faultRemark: ''
      })
    },
    applyCreateBarcodeInfo(data, queriedBarcode) {
      const targetCompanyOptions = Array.isArray(data.targetCompanyOptions) ? data.targetCompanyOptions : []
      const currentTargetCompanyId = this.createForm.targetCompanyId
      const matchedCurrentTarget = targetCompanyOptions.some(item => String(item.id) === String(currentTargetCompanyId))
      let targetCompanyId = matchedCurrentTarget ? currentTargetCompanyId : undefined
      if (!targetCompanyId && data.defaultTargetCompanyId !== null && data.defaultTargetCompanyId !== undefined) {
        targetCompanyId = data.defaultTargetCompanyId
      }
      if (!targetCompanyId && targetCompanyOptions.length === 1) {
        targetCompanyId = targetCompanyOptions[0].id
      }
      Object.assign(this.createForm, {
        barcode: data.barcode || queriedBarcode,
        queriedBarcode: data.barcode || queriedBarcode,
        barcodeQueried: true,
        productCode: data.productCode || '',
        productName: data.productName || '',
        productModel: data.productModel || '',
        machineNo: data.machineNo || '',
        brandCode: data.brandCode || '',
        warrantyStatus: data.warrantyStatus || '',
        hqCompanyId: data.hqCompanyId,
        hqCompanyName: data.hqCompanyName || '',
        targetCompanyId,
        targetCompanyOptions,
        faultOptions: data.faultOptions || [],
        otherFaultLabel: data.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL,
        faultItems: [],
        faultRemark: ''
      })
    },
    loadCompanyAddressList(options = {}) {
      this.companyAddressLoading = true
      return listCompanyAddress().then(res => {
        if (!res) {
          return
        }
        this.companyAddressList = res.data || []
        this.syncCreateCompanyAddressSelection(options)
      }).finally(() => {
        this.companyAddressLoading = false
      })
    },
    syncCreateCompanyAddressSelection(options = {}) {
      const preserveSelection = !!options.preserveSelection
      const currentAddressId = this.createForm.companyAddressId
      let selected = null
      if (preserveSelection && currentAddressId !== undefined && currentAddressId !== null) {
        selected = this.companyAddressList.find(item => String(item.id) === String(currentAddressId)) || null
      }
      if (!selected) {
        selected = this.companyAddressList.find(item => item.isDefault === 1) || this.companyAddressList[0] || null
      }
      this.applySelectedCompanyAddress(selected)
    },
    applySelectedCompanyAddress(address) {
      this.createForm.companyAddressId = address ? address.id : undefined
      this.createForm.senderName = address ? (address.contactName || '') : ''
      this.createForm.senderMobile = address ? (address.contactPhone || '') : ''
      this.createForm.senderAddress = address ? (address.address || '') : ''
    },
    handleCreateCompanyAddressChange(value) {
      const selected = this.companyAddressList.find(item => String(item.id) === String(value)) || null
      this.applySelectedCompanyAddress(selected)
    },
    openCompanyAddressDialog() {
      this.companyAddressDialogVisible = true
      this.loadCompanyAddressList({ preserveSelection: true })
    },
    openCompanyAddressForm(address) {
      if (address) {
        this.companyAddressFormTitle = '编辑地址'
        this.companyAddressForm = {
          id: address.id,
          contactName: address.contactName || '',
          contactPhone: address.contactPhone || '',
          address: address.address || '',
          isDefault: address.isDefault === 1 ? 1 : 0
        }
      } else {
        this.companyAddressFormTitle = '新增地址'
        this.companyAddressForm = buildDefaultCompanyAddressForm()
      }
      this.companyAddressFormVisible = true
    },
    submitCompanyAddress() {
      const payload = {
        id: this.companyAddressForm.id,
        contactName: normalizeText(this.companyAddressForm.contactName),
        contactPhone: normalizeText(this.companyAddressForm.contactPhone),
        address: normalizeText(this.companyAddressForm.address),
        isDefault: this.companyAddressForm.isDefault === 1 ? 1 : 0
      }
      if (!payload.contactName) {
        this.$message.error('请输入联系人')
        return
      }
      if (!payload.contactPhone) {
        this.$message.error('请输入联系电话')
        return
      }
      if (!payload.address) {
        this.$message.error('请输入详细地址')
        return
      }
      const request = payload.id ? updateCompanyAddress(payload) : createCompanyAddress(payload)
      this.companyAddressSubmitting = true
      request.then(res => {
        if (!res) {
          return
        }
        this.$message.success(payload.id ? '地址修改成功' : '地址新增成功')
        this.companyAddressFormVisible = false
        return this.loadCompanyAddressList({ preserveSelection: true })
      }).finally(() => {
        this.companyAddressSubmitting = false
      })
    },
    handleDeleteCompanyAddress(address) {
      if (!address || !address.id) {
        return
      }
      this.$confirm('确认删除该地址吗？', '提示', { type: 'warning' }).then(() => {
        return deleteCompanyAddress(address.id)
      }).then(res => {
        if (!res) {
          return
        }
        this.$message.success('地址删除成功')
        return this.loadCompanyAddressList({ preserveSelection: true })
      }).catch(() => {})
    },
    handleSetDefaultCompanyAddress(address) {
      if (!address || !address.id || address.isDefault === 1) {
        return
      }
      setDefaultCompanyAddress(address.id).then(res => {
        if (!res) {
          return
        }
        this.$message.success('默认地址设置成功')
        return this.loadCompanyAddressList({ preserveSelection: true })
      })
    },
    submitCreate() {
      this.$refs.createForm.validate(valid => {
        if (!valid) {
          return
        }
        if (!this.validateCreateBeforeSubmit()) {
          return
        }
        if (this.isCreateMailMode && !this.validateSendInfo(this.createForm)) {
          return
        }
        const payload = this.buildCreatePayload()
        const request = this.resolveCreateRequest(payload)
        if (!request) {
          return
        }
        this.createSubmitting = true
        request.then(res => {
          if (!res) {
            return
          }
          this.$message.success('建单成功')
          this.createDialogVisible = false
          this.getList()
          if (res.data) {
            this.openDetail(res.data)
          }
        }).finally(() => {
          this.createSubmitting = false
        })
      })
    },
    validateCreateBeforeSubmit() {
      const barcode = normalizeText(this.createForm.barcode)
      if (!this.createForm.barcodeQueried || this.createForm.queriedBarcode !== barcode) {
        this.$message.error('请先查询建单信息，再提交建单')
        return false
      }
      if (this.showCreateTargetCompany && !this.createForm.targetCompanyId) {
        this.$message.error(`请选择${this.createTargetCompanyLabel}`)
        return false
      }
      if ((this.createForm.faultOptions || []).length && !(this.createForm.faultItems || []).length) {
        this.$message.error('请选择故障描述')
        return false
      }
      if (this.showCreateFaultRemark && !normalizeText(this.createForm.faultRemark)) {
        this.$message.error('请输入故障备注')
        return false
      }
      return true
    },
    buildCreatePayload() {
      const payload = {
        customerName: normalizeText(this.createForm.customerName),
        customerMobile: normalizeText(this.createForm.customerMobile),
        barcode: normalizeText(this.createForm.barcode),
        serviceMode: this.createForm.serviceMode,
        faultItems: (this.createForm.faultItems || []).map(item => normalizeText(item)).filter(item => item),
        faultRemark: normalizeText(this.createForm.faultRemark) || '',
        faultImageFileIds: buildFileIdList(this.createForm.faultImageFiles),
        faultVideoFileIds: buildFileIdList(this.createForm.faultVideoFiles),
        faultVoiceFileIds: buildFileIdList(this.createForm.faultVoiceFiles),
        senderName: '',
        senderMobile: '',
        senderAddress: '',
        sendExpressNo: '',
        senderVoucherFileIds: []
      }
      if (this.isCreateMailMode) {
        payload.senderName = normalizeText(this.createForm.senderName)
        payload.senderMobile = normalizeText(this.createForm.senderMobile)
        payload.senderAddress = normalizeText(this.createForm.senderAddress)
        payload.sendExpressNo = normalizeText(this.createForm.sendExpressNo)
        payload.senderVoucherFileIds = buildFileIdList(this.createForm.senderVoucherFiles)
      }
      if (this.createForm.entryMode !== CREATE_ENTRY_PROXY) {
        payload.targetCompanyId = this.createForm.targetCompanyId
      }
      return payload
    },
    resolveCreateRequest(payload) {
      if (this.createForm.entryMode === CREATE_ENTRY_PROXY) {
        return createProxyWorkOrder(payload)
      }
      if (this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST) {
        return createUpstreamFirstWorkOrder(payload)
      }
      if (this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_HQ) {
        return createUpstreamHqWorkOrder(payload)
      }
      return null
    },
    validateSendInfo(form) {
      if (!form.companyAddressId) {
        this.$message.error('请选择公司地址簿')
        return false
      }
      if (!form.senderName) {
        this.$message.error('当前地址未配置寄件人')
        return false
      }
      if (!form.senderMobile) {
        this.$message.error('当前地址未配置寄件手机号')
        return false
      }
      if (!form.senderAddress) {
        this.$message.error('当前地址未配置寄件地址')
        return false
      }
      return true
    },
    handleView(row) {
      this.openDetail(row.id)
    },
    openDetail(workOrderId) {
      this.detailVisible = true
      this.detailLoading = true
      return getWorkOrder(workOrderId).then(res => {
        if (!res) {
          return
        }
        this.detail = res.data || null
      }).finally(() => {
        this.detailLoading = false
      })
    },
    handleAction(action) {
      if (!this.detail) {
        return
      }
      const loadOptions = []
      if (action === 'ASSIGN') {
        loadOptions.push(this.loadAssignUserOptions(this.detail.id))
      }
      if (action === 'TRANSFER') {
        loadOptions.push(this.loadTransferTargetOptions(this.detail.id))
      }
      Promise.all(loadOptions).then(() => {
        this.openActionDialog(action, this.detail.id)
      })
    },
    loadAssignUserOptions(workOrderId) {
      return listAssignUserOptions(workOrderId).then(res => {
        if (!res) {
          return
        }
        this.assignUserOptions = res.data || []
      })
    },
    loadTransferTargetOptions(workOrderId) {
      return listTransferTargetOptions(workOrderId).then(res => {
        if (!res) {
          return
        }
        this.transferTargetOptions = res.data || []
      })
    },
    loadRepairFaultOptions(workOrderId) {
      this.actionRepairConfigLoading = true
      this.actionRepairFaultOptions = []
      return listRepairFaultOptions(workOrderId).then(res => {
        if (!res) {
          return
        }
        this.actionRepairFaultOptions = res.data || []
      }).finally(() => {
        this.actionRepairConfigLoading = false
      })
    },
    openActionDialog(action, workOrderId) {
      const currentWorkOrderId = workOrderId || (this.detail && this.detail.id)
      if (!currentWorkOrderId) {
        return
      }
      const form = buildDefaultActionForm()
      const currentQuote = this.getCurrentValidQuote()
      form.workOrderId = currentWorkOrderId
      if (action === 'QUOTE' || action === 'REPAIR_SAVE' || action === 'REPAIR_FINISH') {
        this.fillQuoteForm(form, currentQuote)
      }
      if (action === 'REPAIR_FINISH') {
        form.isFinished = 1
      }
      if (action === 'UPLOAD_SEND_EXPRESS') {
        form.sendExpressNo = this.detail.sendExpressNo || ''
        form.senderVoucherFiles = cloneFileItems(this.detail.senderVoucherFiles)
      }
      if (action === 'CLOSE') {
        form.returnMethod = this.detail.returnMethod || (this.detail.serviceMode === SERVICE_MODE_MAIL ? RETURN_METHOD_MAIL : RETURN_METHOD_PICKUP)
        form.returnExpressNo = this.detail.returnExpressNo || ''
        form.returnVoucherFiles = cloneFileItems(this.detail.returnVoucherFiles)
        form.closeReason = this.detail.closeReason || ''
      }
      let preparePromise = Promise.resolve()
      if (action === 'REPAIR_SAVE' || action === 'REPAIR_FINISH') {
        preparePromise = this.loadRepairFaultOptions(currentWorkOrderId)
      } else {
        this.actionRepairFaultOptions = []
        this.actionRepairConfigLoading = false
      }
      this.actionForm = form
      this.pendingTechAcceptPayload = null
      this.actionDialogAction = action
      this.actionDialogTitle = ACTION_META[action] ? ACTION_META[action].title : '工单操作'
      preparePromise.finally(() => {
        this.actionDialogVisible = true
      })
    },
    closeActionDialog() {
      this.actionDialogVisible = false
      this.pendingTechAcceptPayload = null
    },
    handleTechAcceptFaultJudgeChange(value) {
      if (value === FAULT_JUDGE_NO_FAULT) {
        this.actionForm.quoteAmount = undefined
        this.actionForm.quoteDesc = ''
      }
    },
    openNoFaultTechAcceptCloseDialog(payload) {
      const form = buildDefaultActionForm()
      form.workOrderId = payload.workOrderId
      form.returnMethod = this.detail && this.detail.serviceMode === SERVICE_MODE_MAIL ? RETURN_METHOD_MAIL : RETURN_METHOD_PICKUP
      this.pendingTechAcceptPayload = payload
      this.actionForm = form
      this.actionDialogAction = 'CLOSE'
      this.actionDialogTitle = ACTION_META.CLOSE.title
    },
    submitAction() {
      if (!this.validateAction()) {
        return
      }
      const payload = this.buildActionPayload()
      if (!payload) {
        return
      }
      if (this.actionDialogAction === 'TECH_ACCEPT' && payload.faultJudge === FAULT_JUDGE_NO_FAULT) {
        this.openNoFaultTechAcceptCloseDialog(payload)
        return
      }
      const request = this.resolveActionRequest(payload)
      if (!request) {
        return
      }
      this.actionSubmitting = true
      request.then(res => {
        if (!res) {
          return
        }
        this.$message.success('操作成功')
        this.refreshAfterAction()
      }).finally(() => {
        this.actionSubmitting = false
      })
    },
    validateAction() {
      if (this.actionDialogAction === 'ASSIGN' && !this.actionForm.assignedUserId) {
        this.$message.error('请选择维修员')
        return false
      }
      if (this.actionDialogAction === 'TRANSFER' && !this.actionForm.targetCompanyId) {
        this.$message.error('请选择目标公司')
        return false
      }
      if (this.actionDialogAction === 'TECH_ACCEPT' && !this.actionForm.faultJudge) {
        this.$message.error('请输入故障判定')
        return false
      }
      if (this.actionDialogAction === 'QUOTE' && !this.actionForm.faultJudge) {
        this.$message.error('请输入故障判定')
        return false
      }
      if ((this.actionDialogAction === 'REPAIR_SAVE' || this.actionDialogAction === 'REPAIR_FINISH')
        && !this.actionForm.repairSummary
        && !this.actionForm.repairDesc
        && !this.actionForm.otherDesc
        && this.buildRepairFaults().length === 0) {
        this.$message.error('请至少填写一项维修内容')
        return false
      }
      if ((this.actionDialogAction === 'REPAIR_SAVE' || this.actionDialogAction === 'REPAIR_FINISH')
        && !this.validateRepairFaultItems()) {
        return false
      }
      if (this.actionDialogAction === 'REVIEW' && !this.actionForm.reviewResult) {
        this.$message.error('请选择复检结果')
        return false
      }
      if (this.actionDialogAction === 'UPLOAD_SEND_EXPRESS' && !this.actionForm.sendExpressNo) {
        this.$message.error('请输入寄件快递单号')
        return false
      }
      if (this.actionDialogAction === 'CLOSE') {
        if (!this.actionForm.returnMethod) {
          this.$message.error('请选择返回方式')
          return false
        }
        if (this.actionForm.returnMethod === RETURN_METHOD_MAIL && !this.actionForm.returnExpressNo) {
          this.$message.error('请输入回寄快递单号')
          return false
        }
        if (!this.actionForm.closeReason) {
          this.$message.error('请输入关闭原因')
          return false
        }
      }
      return true
    },
    buildActionPayload() {
      const workOrderId = this.resolveActionWorkOrderId()
      if (!workOrderId) {
        return null
      }
      switch (this.actionDialogAction) {
        case 'ASSIGN':
          return { workOrderId, assignedUserId: this.actionForm.assignedUserId }
        case 'TRANSFER':
          return { workOrderId, targetCompanyId: this.actionForm.targetCompanyId, remark: this.actionForm.remark }
        case 'TECH_ACCEPT':
          return {
            workOrderId,
            faultJudge: this.actionForm.faultJudge,
            quoteAmount: this.actionForm.faultJudge === FAULT_JUDGE_HAS_FAULT ? this.actionForm.quoteAmount : undefined,
            quoteDesc: this.actionForm.faultJudge === FAULT_JUDGE_HAS_FAULT ? this.actionForm.quoteDesc : ''
          }
        case 'QUOTE':
          return {
            workOrderId,
            faultJudge: this.actionForm.faultJudge,
            quoteAmount: this.actionForm.quoteAmount,
            quoteDesc: this.actionForm.quoteDesc
          }
        case 'REPAIR_SAVE':
        case 'REPAIR_FINISH':
          return {
            workOrderId,
            quoteAmount: this.actionForm.quoteAmount,
            quoteDesc: this.actionForm.quoteDesc,
            repairSummary: this.actionForm.repairSummary,
            repairDesc: this.actionForm.repairDesc,
            otherDesc: this.actionForm.otherDesc,
            isFinished: this.actionDialogAction === 'REPAIR_FINISH' ? 1 : 0,
            faults: this.buildRepairFaults()
          }
        case 'REVIEW':
          return {
            workOrderId,
            reviewResult: this.actionForm.reviewResult,
            reviewDesc: this.actionForm.reviewDesc,
            isContinueRepair: this.actionForm.reviewResult === REVIEW_RESULT_CONTINUE ? 1 : 0
          }
        case 'UPLOAD_SEND_EXPRESS':
          return {
            workOrderId,
            sendExpressNo: this.actionForm.sendExpressNo,
            senderVoucherFileIds: buildFileIdList(this.actionForm.senderVoucherFiles)
          }
        case 'CLOSE':
          return {
            workOrderId,
            returnMethod: this.actionForm.returnMethod,
            returnExpressNo: this.actionForm.returnMethod === RETURN_METHOD_MAIL ? this.actionForm.returnExpressNo : '',
            returnVoucherFileIds: this.actionForm.returnMethod === RETURN_METHOD_MAIL
              ? buildFileIdList(this.actionForm.returnVoucherFiles)
              : [],
            closeReason: this.actionForm.closeReason
          }
        default:
          return null
      }
    },
    resolveActionRequest(payload) {
      switch (this.actionDialogAction) {
        case 'ASSIGN':
          return assignWorkOrder(payload)
        case 'TECH_ACCEPT':
          return techAcceptWorkOrder(payload)
        case 'TRANSFER':
          return transferWorkOrder(payload)
        case 'QUOTE':
          return quoteWorkOrder(payload)
        case 'REPAIR_SAVE':
        case 'REPAIR_FINISH':
          return repairWorkOrder(payload)
        case 'REVIEW':
          return reviewWorkOrder(payload)
        case 'UPLOAD_SEND_EXPRESS':
          return updateWorkOrderSendExpress(payload)
        case 'CLOSE':
          if (this.pendingTechAcceptPayload) {
            return techAcceptWorkOrder({
              ...this.pendingTechAcceptPayload,
              returnMethod: payload.returnMethod,
              returnExpressNo: payload.returnExpressNo,
              returnVoucherFileIds: payload.returnVoucherFileIds,
              closeReason: payload.closeReason
            })
          }
          return closeWorkOrder(payload)
        default:
          return null
      }
    },
    buildRepairFaults() {
      return (this.actionForm.faults || []).map(item => ({
        faultDesc: item.faultDesc ? item.faultDesc.trim() : '',
        repairDesc: item.repairDesc ? item.repairDesc.trim() : '',
        repairItems: (item.repairItems || [])
          .map(option => (option || '').trim())
          .filter(option => option),
        otherDesc: item.otherDesc ? item.otherDesc.trim() : '',
        partDesc: item.partDesc ? item.partDesc.trim() : '',
        imageUrls: item.imageUrls ? item.imageUrls.trim() : ''
      })).filter(item => item.faultDesc || item.repairDesc || (item.repairItems && item.repairItems.length) || item.otherDesc || item.partDesc || item.imageUrls)
    },
    addFaultItem() {
      this.actionForm.faults.push(createFaultItem())
    },
    removeFaultItem(index) {
      this.actionForm.faults.splice(index, 1)
    },
    handleFaultDescChange(item) {
      const repairOptions = this.getRepairOptions(item)
      if (!repairOptions.length) {
        item.repairItems = []
        item.otherDesc = ''
        if (this.hasRepairFaultConfig) {
          item.repairDesc = ''
        }
        return
      }
      item.repairDesc = ''
      item.repairItems = (item.repairItems || []).filter(option => repairOptions.includes(option) || option === OTHER_REPAIR_OPTION)
      if (!item.repairItems.includes(OTHER_REPAIR_OPTION)) {
        item.otherDesc = ''
      }
    },
    getRepairOptions(item) {
      if (!item || !item.faultDesc) {
        return []
      }
      const matched = (this.actionRepairFaultOptions || []).find(option => option.faultDesc === item.faultDesc)
      return matched && matched.repairOptions ? matched.repairOptions : []
    },
    hasRepairFaultOption(faultDesc) {
      if (!faultDesc) {
        return false
      }
      return (this.actionRepairFaultOptions || []).some(option => option && option.faultDesc === faultDesc)
    },
    isOtherRepairSelected(item) {
      return !!(item && item.repairItems && item.repairItems.includes(OTHER_REPAIR_OPTION))
    },
    validateRepairFaultItems() {
      const faultItems = this.buildRepairFaults()
      for (const item of faultItems) {
        if (!item.faultDesc) {
          this.$message.error('请输入故障描述')
          return false
        }
        if (this.hasRepairFaultConfig && !this.hasRepairFaultOption(item.faultDesc)) {
          this.$message.error('璇烽€夋嫨閰嶇疆鍐呯殑鏁呴殰鎻忚堪')
          return false
        }
        const repairOptions = this.getRepairOptions(item)
        if (repairOptions.length) {
          if (!item.repairItems || !item.repairItems.length) {
            this.$message.error('请选择维修说明')
            return false
          }
          if (item.repairItems.includes(OTHER_REPAIR_OPTION) && !item.otherDesc) {
            this.$message.error('请选择其它维修说明后，必须填写其他维修说明')
            return false
          }
        } else if (!item.repairDesc) {
          this.$message.error('请输入维修说明')
          return false
        }
        if (!item.partDesc) {
          this.$message.error('请输入配件信息')
          return false
        }
      }
      return true
    },
    refreshAfterAction() {
      const workOrderId = this.resolveActionWorkOrderId()
      const shouldReloadDetail = this.detailVisible && this.detail && String(this.detail.id) === String(workOrderId)
      this.actionDialogVisible = false
      this.pendingTechAcceptPayload = null
      this.actionRepairFaultOptions = []
      this.actionRepairConfigLoading = false
      this.getList()
      if (shouldReloadDetail && workOrderId) {
        this.openDetail(workOrderId)
      }
    },
    statusLabel(status) {
      return STATUS_LABELS[status] || status || '-'
    },
    evaluateStatusLabel(status) {
      return EVALUATE_STATUS_LABELS[status] || status || '-'
    },
    serviceModeLabel(serviceMode) {
      return SERVICE_MODE_LABELS[serviceMode] || serviceMode || '-'
    },
    statusTag(status) {
      if (status === 'COMPLETED') {
        return 'success'
      }
      if (status === 'CLOSED') {
        return 'info'
      }
      if (status === 'IN_PROGRESS') {
        return 'primary'
      }
      return 'warning'
    },
    createEntryTypeLabel(createEntryType) {
      if (createEntryType === CREATE_ENTRY_PROXY) {
        return '代客户填写'
      }
      if (createEntryType === CREATE_ENTRY_UPSTREAM_FIRST) {
        return '报修一级'
      }
      if (createEntryType === CREATE_ENTRY_UPSTREAM_HQ) {
        return '报修佳士'
      }
      return createEntryType || '-'
    },
    brandTypeLabel(brandType) {
      if (brandType === BRAND_TYPE_JASIC) {
        return '佳士品牌'
      }
      if (brandType === BRAND_TYPE_NON_JASIC) {
        return '非佳士品牌'
      }
      return brandType || '-'
    },
    yesNoText(value) {
      if (value === 1) {
        return '是'
      }
      if (value === 0) {
        return '否'
      }
      return '-'
    },
    textValue(value) {
      return value === null || value === undefined || value === '' ? '-' : value
    },
    getCurrentValidQuote() {
      const quotes = (this.detail && this.detail.quotes) || []
      return quotes.find(item => Number(item.isCurrentValid) === 1) || quotes[0] || null
    },
    fillQuoteForm(form, quote) {
      if (!form || !quote) {
        return
      }
      form.faultJudge = quote.faultJudge || ''
      form.quoteAmount = quote.quoteAmount === null || quote.quoteAmount === undefined || quote.quoteAmount === ''
        ? undefined
        : Number(quote.quoteAmount)
      form.quoteDesc = quote.quoteDesc || ''
    },
    shouldShowAssignFallback() {
      if (!this.detail) {
        return false
      }
      if ((this.detail.availableActions || []).includes('ASSIGN')) {
        return false
      }
      if (!this.hasPerm('workorder:assign')) {
        return false
      }
      if (this.detail.mainStatus !== 'PENDING_ASSIGN') {
        return false
      }
      if (this.detail.assignedUserId) {
        return false
      }
      return String(this.detail.currentAcceptCompanyId) === String(this.$store.getters.currentCompanyId)
    },
    openFilePreview(item) {
      if (!item || !item.previewUrl) {
        return
      }
      window.open(item.previewUrl, '_blank')
    },
    fileDisplayName(item) {
      if (!item) {
        return '-'
      }
      return item.originalName || item.fileName || `文件-${item.fileId || ''}`
    },
    formatFileSize(size) {
      return formatFileSizeText(size)
    },
    resolveActionWorkOrderId() {
      return this.actionForm.workOrderId || (this.detail && this.detail.id)
    },
    hasPerm(perm) {
      const perms = this.$store.getters.perms || []
      return perms.includes(perm)
    }
  }
}
</script>

<style lang="scss" scoped>
.app-container {
  padding: 0;
}

.search-card {
  margin-bottom: 12px;
}

.scope-tabs {
  margin-bottom: 12px;
}

.status-tabs {
  margin-bottom: 12px;
}

.table-card {
  margin-top: 12px;
}

.table-toolbar {
  margin-bottom: 12px;
}

.create-entry-tabs {
  margin-bottom: 16px;
}

.inline-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-actions .el-select {
  flex: 1;
}

.pagination {
  margin-top: 16px;
  text-align: right;
}

.drawer-body {
  padding: 0 16px 24px;
}

.drawer-actions {
  margin-bottom: 16px;
}

.section-title {
  margin: 18px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.file-link-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.fault-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.fault-item {
  margin-bottom: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.fault-item__toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
  font-size: 13px;
  color: #606266;
}

.repair-card + .repair-card {
  margin-top: 12px;
}

.inner-table {
  margin-top: 12px;
}
</style>
