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
        <el-table-column label="网点电话" prop="currentAcceptCompanyPhone" min-width="130" />
        <el-table-column label="当前维修员" prop="assignedUserName" min-width="110" />
        <el-table-column label="转单" min-width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.hasTransfer === 1 ? 'warning' : 'info'" size="mini">
              {{ row.hasTransfer === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" fixed="right" :width="operationColumnWidth">
          <template slot-scope="{ row }">
            <div class="table-actions-cell">
              <div v-if="isCurrentView" class="table-actions-cell__buttons">
                <el-button
                  v-for="item in getRowPrimaryActions(row)"
                  :key="`${row.id}-${item.action}`"
                  type="text"
                  size="mini"
                  :class="['table-action-link', `is-${item.type || 'primary'}`]"
                  @click="handleListAction(row, item.action)"
                >
                  {{ item.label }}
                </el-button>
                <el-dropdown
                  v-if="getRowMoreActions(row).length"
                  trigger="click"
                  @command="handleListMoreAction(row, $event)"
                >
                  <span class="el-dropdown-link table-action-link is-default">
                    更多<i class="el-icon-arrow-down el-icon--right"></i>
                  </span>
                  <el-dropdown-menu slot="dropdown">
                    <el-dropdown-item
                      v-for="item in getRowMoreActions(row)"
                      :key="`${row.id}-${item.action}-more`"
                      :command="item.action"
                    >
                      {{ item.label }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </el-dropdown>
              </div>
              <div class="table-actions-cell__buttons">
                <el-button type="text" size="mini" class="table-action-link is-default" @click="handleView(row)">详情</el-button>
              </div>
              <div v-if="shouldShowReadonlyReason(row)" class="table-actions-cell__reason">
                {{ row.readonlyReason }}
              </div>
            </div>
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
        title="有码时先查条码；无码可直接提交，系统将按无码工单处理。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 16px;"
      />
      <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="96px">
        <div class="create-section-title">必填信息</div>
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
                placeholder="有条码时请输入并查询；无码可直接提交"
                @keyup.enter.native="queryCreateBarcodeInfo"
              >
                <el-button
                  slot="append"
                  icon="el-icon-search"
                  :loading="createBarcodeLoading"
                  @click="queryCreateBarcodeInfo"
                >
                  查询条码
                </el-button>
              </el-input>
            </el-form-item>
          </el-col>
          <el-col v-if="showCreateAutoTargetCompanyField" :span="24">
            <el-form-item label="目标一级网点">
              <el-input
                :value="createForm.targetCompanyName"
                disabled
                placeholder="系统自动带出；未带出时请联系管理员排查"
              />
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
          <template v-if="hasCreateResolvedBarcodeInfo">
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
                :disabled="isCreateFaultSelectDisabled"
              >
                <el-option
                  v-for="item in effectiveCreateFaultOptions"
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
        </el-row>

        <div v-if="isCreateMailMode" class="create-section-title">寄修信息</div>
        <el-row v-if="isCreateMailMode" :gutter="16">
          <el-col :span="24">
            <el-form-item label="寄件信息">
              <div class="create-address-picker">
                <el-input
                  :value="createShippingAddressSummary"
                  type="textarea"
                  :rows="3"
                  disabled
                  placeholder="请选择寄件信息"
                />
                <div class="create-address-picker__actions">
                  <el-button type="primary" plain @click="openCompanyAddressDialog('select')">选择寄件信息</el-button>
                  <el-button icon="el-icon-refresh" @click="loadCompanyAddressList({ preserveSelection: true })">刷新</el-button>
                </div>
              </div>
            </el-form-item>
            <el-alert
              v-if="!companyAddressLoading && !companyAddressList.length"
              title="当前公司还没有可用地址，请先维护公司地址簿后再提交寄修工单。"
              type="warning"
              :closable="false"
              show-icon
              style="margin-top: 8px;"
            />
          </el-col>
        </el-row>
        <div class="create-section-title create-section-title--toggle" @click="toggleCreateSupplementSection">
          <span>补充说明</span>
          <el-button type="text">{{ createSupplementExpanded ? '收起' : '展开' }}</el-button>
        </div>
        <el-collapse-transition>
          <div v-show="createSupplementExpanded">
            <el-row :gutter="16">
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
              <template v-if="isCreateMailMode">
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
              </template>
            </el-row>
          </div>
        </el-collapse-transition>
      </el-form>
      <div slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定</el-button>
      </div>
    </el-dialog>

    <el-dialog :title="companyAddressDialogTitle" :visible.sync="companyAddressDialogVisible" width="760px" append-to-body>
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
        <el-table-column label="操作" :width="companyAddressDialogMode === 'select' ? 280 : 220" fixed="right">
          <template slot-scope="{ row }">
            <el-button
              v-if="companyAddressDialogMode === 'select'"
              type="text"
              size="mini"
              @click="handleSelectCompanyAddress(row)"
            >
              选择
            </el-button>
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
            <el-descriptions-item label="申请来源">{{ textValue(detail.applicationSourceName) }}</el-descriptions-item>
            <el-descriptions-item label="品牌类型">{{ textValue(detail.brandTypeLabel || brandTypeLabel(detail.brandType)) }}</el-descriptions-item>
            <el-descriptions-item label="条码">{{ textValue(detail.barcode) }}</el-descriptions-item>
            <el-descriptions-item label="物料编码">{{ textValue(detail.productCode) }}</el-descriptions-item>
            <el-descriptions-item label="机型">{{ textValue(detail.productModel) }}</el-descriptions-item>
            <el-descriptions-item label="品牌编码">{{ textValue(detail.brandCode) }}</el-descriptions-item>
            <el-descriptions-item label="品牌名称">{{ textValue(detail.brandName) }}</el-descriptions-item>
            <el-descriptions-item label="服务方式">{{ textValue(detail.serviceModeLabel || serviceModeLabel(detail.serviceMode)) }}</el-descriptions-item>
            <el-descriptions-item label="质保状态">{{ textValue(detail.warrantyStatus) }}</el-descriptions-item>
            <el-descriptions-item label="当前受理公司">{{ textValue(detail.currentAcceptCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="网点电话">{{ textValue(detail.currentAcceptCompanyPhone) }}</el-descriptions-item>
            <el-descriptions-item label="当前维修员">{{ textValue(detail.assignedUserName) }}</el-descriptions-item>
            <el-descriptions-item label="建单公司">{{ textValue(detail.createCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="建单入口">{{ textValue(createEntryTypeLabel(detail.createEntryType)) }}</el-descriptions-item>
            <el-descriptions-item label="归属总部">{{ textValue(detail.hqCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="转单次数">{{ detail.transferCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="评价状态">{{ textValue(detail.evaluateStatusLabel || evaluateStatusLabel(detail.evaluateStatus)) }}</el-descriptions-item>
            <el-descriptions-item label="客户报修故障" :span="2">{{ textValue(detail.faultDesc) }}</el-descriptions-item>
            <el-descriptions-item label="客户故障备注" :span="2">{{ textValue(detail.faultRemark) }}</el-descriptions-item>
            <el-descriptions-item label="首次维修确认故障" :span="2">{{ textValue(firstRepairConfirmedFaultDesc) }}</el-descriptions-item>
            <el-descriptions-item label="其它故障说明" :span="2">{{ textValue(firstRepairConfirmedFaultRemark) }}</el-descriptions-item>
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
              <el-descriptions-item label="登记阶段">{{ textValue(repair.registerStageLabel) }}</el-descriptions-item>
              <el-descriptions-item label="登记时间">{{ textValue(repair.createTime) }}</el-descriptions-item>
              <el-descriptions-item label="维修公司">{{ textValue(repair.companyName) }}</el-descriptions-item>
              <el-descriptions-item label="维修人">{{ textValue(repair.repairUserName) }}</el-descriptions-item>
              <el-descriptions-item label="维修完成">{{ yesNoText(repair.isFinished) }}</el-descriptions-item>
              <el-descriptions-item label="完成时间">{{ textValue(repair.finishedTime) }}</el-descriptions-item>
            </el-descriptions>
            <el-table v-if="repair.faults && repair.faults.length" :data="repair.faults" border size="small" class="inner-table">
              <el-table-column label="维修确认故障" prop="faultDesc" min-width="180" show-overflow-tooltip />
              <el-table-column label="其它故障说明" prop="faultRemark" min-width="180" show-overflow-tooltip />
              <el-table-column label="维修说明" prop="repairDesc" min-width="180" show-overflow-tooltip />
              <el-table-column label="其他维修说明" prop="otherDesc" min-width="180" show-overflow-tooltip />
              <el-table-column label="配件名称" min-width="160">
                <template slot-scope="{ row }">
                  <div v-if="faultPartRows(row).length">
                    <div v-for="(partItem, index) in faultPartRows(row)" :key="`name-${row.id}-${index}`">{{ partItem.partName }}</div>
                  </div>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="配件数量" min-width="100">
                <template slot-scope="{ row }">
                  <div v-if="faultPartRows(row).length">
                    <div v-for="(partItem, index) in faultPartRows(row)" :key="`qty-${row.id}-${index}`">{{ partItem.partQty }}</div>
                  </div>
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="登记人" prop="createdByName" min-width="110" />
              <el-table-column label="创建时间" prop="createTime" min-width="160" />
            </el-table>
            <div v-if="hasRepairAttachmentFiles(repair)" class="repair-files">
              <div v-for="group in repairAttachmentGroups(repair)" :key="group.key" class="repair-files__group">
                <div class="repair-files__label">{{ group.label }}</div>
                <div class="repair-files__items">
                  <span v-if="!group.files.length">-</span>
                  <el-link
                    v-for="item in group.files"
                    :key="item.fileId"
                    type="primary"
                    :underline="false"
                    @click="openFilePreview(item)"
                  >
                    {{ fileDisplayName(item) }}
                  </el-link>
                </div>
              </div>
            </div>
          </div>

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

    <el-dialog
      title="补录机器型号"
      :visible.sync="repairProductModelDialogVisible"
      width="520px"
      append-to-body
      @close="closeRepairProductModelDialog"
    >
      <el-alert
        title="佳士品牌工单在维修登记或复检前必须先补录机器型号，补录后不可再次修改。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 12px;"
      />
      <el-form :model="repairProductModelForm" label-width="88px">
        <el-form-item label="机器型号" required>
          <el-select
            v-model="repairProductModelForm.productModel"
            placeholder="请输入关键字搜索并选择"
            filterable
            remote
            reserve-keyword
            clearable
            style="width: 100%;"
            :remote-method="loadRepairProductModelOptions"
            :loading="repairProductModelOptionsLoading"
          >
            <el-option
              v-for="item in repairProductModelOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="closeRepairProductModelDialog">取消</el-button>
        <el-button type="primary" :loading="repairProductModelSubmitting" @click="submitRepairProductModel">确定</el-button>
      </div>
    </el-dialog>

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
            class="action-form-message"
            v-else-if="actionForm.faultJudge === faultJudgeNoFault"
            title="选择无故障后，下一步将进入关闭工单弹窗填写返回方式和关闭原因"
            type="warning"
            :closable="false"
            show-icon
          />
        </template>

        <template v-else-if="actionDialogAction === 'REPAIR_FINISH' || actionDialogAction === 'REVIEW'">
          <el-alert
            class="action-form-message"
            v-if="actionDialogAction === 'REPAIR_FINISH' && !actionForm.faultJudge"
            title="当前暂无有效报价；如需调整报价，请先确认工单已完成接单首报"
            type="warning"
            :closable="false"
            show-icon
          />
          <template v-if="actionDialogAction === 'REPAIR_FINISH'">
            <el-form-item label="故障判断">
              <el-input :value="actionForm.faultJudge || '当前暂无首次报价，请联系管理员排查工单数据'" disabled />
            </el-form-item>
            <el-form-item label="报价金额">
              <el-input-number v-model="actionForm.quoteAmount" :min="0" :precision="2" :controls="false" style="width: 100%;" />
            </el-form-item>
            <el-form-item label="报价说明">
              <el-input v-model="actionForm.quoteDesc" type="textarea" :rows="3" placeholder="请输入报价说明" />
            </el-form-item>
          </template>
          <el-descriptions v-else :column="2" border size="small" style="margin-bottom: 12px;">
            <el-descriptions-item label="当前有效报价">{{ textValue(getCurrentValidQuote() && getCurrentValidQuote().quoteAmount) }}</el-descriptions-item>
            <el-descriptions-item label="故障判断">{{ textValue(getCurrentValidQuote() && getCurrentValidQuote().faultJudge) }}</el-descriptions-item>
            <el-descriptions-item label="报价说明" :span="2">{{ textValue(getCurrentValidQuote() && getCurrentValidQuote().quoteDesc) }}</el-descriptions-item>
          </el-descriptions>
          <el-alert
            class="action-form-message"
            v-if="actionRepairConfigLoading"
            title="正在加载故障与维修配置"
            type="info"
            :closable="false"
            show-icon
            style="margin-bottom: 12px;"
          />
          <el-alert
            class="action-form-message"
            v-else-if="!actionRepairFaultOptions.length"
            title="当前总部未配置故障与维修配置，请先维护"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-alert
            class="action-form-message"
            v-else-if="hasRepairFaultConfig"
            :title="actionDialogAction === 'REVIEW'
              ? '复检登记沿用首次维修确认故障，当前故障描述只读'
              : '请先选择维修确认故障，再填写维修说明'"
            type="success"
            :closable="false"
            show-icon
          />
          <el-alert
            class="action-form-message"
            v-else
            title="当前总部未命中故障与维修配置，本次维修登记按手工维修说明收口"
            type="warning"
            :closable="false"
            show-icon
          />
          <el-form-item label="客户报修故障">
            <el-input :value="detail && detail.faultDesc ? detail.faultDesc : '当前工单未记录故障描述'" disabled type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item v-if="hasRepairFaultConfig && actionDialogAction === 'REPAIR_FINISH'" label="维修确认故障">
            <el-select
              v-model="actionForm.faultItems"
              multiple
              collapse-tags
              placeholder="请选择维修确认故障"
              filterable
              style="width: 100%;"
              @change="handleRepairFaultItemsChange"
            >
              <el-option
                v-for="faultOption in repairFaultOptionsWithOther"
                :key="faultOption"
                :label="faultOption"
                :value="faultOption"
              />
            </el-select>
          </el-form-item>
          <el-form-item v-else-if="hasRepairFaultConfig" label="维修确认故障">
            <el-input :value="reviewFaultDescDisplay || '首次维修登记未记录故障描述'" disabled type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item v-if="showRepairFaultRemarkInput" label="其它故障说明">
            <el-input v-model="actionForm.faultRemark" type="textarea" :rows="2" placeholder="请输入其它故障说明" />
          </el-form-item>
          <el-form-item v-else-if="showReviewFaultRemark" label="其它故障说明">
            <el-input :value="reviewFaultRemarkDisplay" disabled type="textarea" :rows="2" />
          </el-form-item>
          <el-form-item v-if="hasRepairFaultConfig" label="维修说明">
            <el-select
              v-model="actionForm.repairItems"
              multiple
              collapse-tags
              placeholder="请选择维修说明"
              filterable
              style="width: 100%;"
              @change="handleRepairItemsChange"
            >
              <el-option
                v-for="repairOption in currentRepairOptions"
                :key="repairOption"
                :label="repairOption"
                :value="repairOption"
              />
              <el-option :label="otherRepairOption" :value="otherRepairOption" />
            </el-select>
          </el-form-item>
          <el-form-item v-else label="维修说明">
            <el-input v-model="actionForm.repairDesc" type="textarea" :rows="2" placeholder="请输入维修说明" />
          </el-form-item>
          <el-form-item v-if="isOtherRepairSelected" label="其他维修说明">
            <el-input v-model="actionForm.otherDesc" type="textarea" :rows="2" placeholder="请输入其他维修说明" />
          </el-form-item>
          <el-form-item label="更换配件" required>
            <div class="repair-part-editor">
              <div
                v-for="(partItem, index) in actionForm.partList"
                :key="`repair-part-${index}`"
                class="repair-part-row"
              >
                <el-input
                  v-model="partItem.partName"
                  class="repair-part-row__name"
                  placeholder="请输入配件名称"
                />
                <el-input-number
                  v-model="partItem.partQty"
                  class="repair-part-row__qty"
                  :min="1"
                  :precision="0"
                  :controls="false"
                />
                <el-button
                  type="text"
                  icon="el-icon-plus"
                  @click="addRepairPartRow"
                />
                <el-button
                  type="text"
                  icon="el-icon-delete"
                  :disabled="(actionForm.partList || []).length <= 1"
                  @click="removeRepairPartRow(index)"
                />
              </div>
            </div>
          </el-form-item>
          <el-form-item label="故障处旧图片">
            <file-upload-field v-model="actionForm.faultOldImageFiles" accept=".jpg,.jpeg,.png,.webp" :size-limit-mb="10" button-text="上传故障处旧图片" :tip="REPAIR_FILE_TIP" />
          </el-form-item>
          <el-form-item label="故障处新图片">
            <file-upload-field v-model="actionForm.faultNewImageFiles" accept=".jpg,.jpeg,.png,.webp" :size-limit-mb="10" button-text="上传故障处新图片" :tip="REPAIR_FILE_TIP" />
          </el-form-item>
          <el-form-item label="机器正面照片">
            <file-upload-field v-model="actionForm.machineImageFiles" accept=".jpg,.jpeg,.png,.webp" :size-limit-mb="10" button-text="上传机器正面照片" :tip="REPAIR_FILE_TIP" />
          </el-form-item>
          <el-form-item label="机器条码照片">
            <file-upload-field v-model="actionForm.machineBarcodeImageFiles" accept=".jpg,.jpeg,.png,.webp" :size-limit-mb="10" button-text="上传机器条码照片" :tip="REPAIR_FILE_TIP" />
          </el-form-item>
          <el-form-item label="其他图片">
            <file-upload-field v-model="actionForm.otherImageFiles" accept=".jpg,.jpeg,.png,.webp" :size-limit-mb="10" button-text="上传其他图片" :tip="REPAIR_FILE_TIP" />
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
          <el-form-item v-if="actionForm.returnMethod === '回寄'" label="回寄单号">
            <el-input v-model="actionForm.returnExpressNo" placeholder="请输入回寄快递单号，不填也可提交" />
          </el-form-item>
          <el-form-item v-if="actionForm.returnMethod === '回寄'" label="回寄凭证" required>
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
  listUpstreamFirstCreateTargetOptions,
  listRepairFaultOptions,
  listRepairProductModelOptions,
  listTransferTargetOptions,
  listWorkOrder,
  repairWorkOrder,
  reviewWorkOrder,
  techAcceptWorkOrder,
  transferWorkOrder,
  updateRepairProductModel,
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
const OTHER_REPAIR_OPTION = '其它维修说明'
const DEFAULT_OTHER_FAULT_LABEL = '其它故障'
const CREATE_ENTRY_PROXY = 'PROXY_SELF'
const CREATE_ENTRY_UPSTREAM_FIRST = 'UPSTREAM_FIRST'
const CREATE_ENTRY_UPSTREAM_HQ = 'UPSTREAM_HQ'
const BRAND_TYPE_JASIC = 'JASIC'
const BRAND_TYPE_NON_JASIC = 'NON_JASIC'
const FAULT_DESC_SEPARATOR = '；'
const REPAIR_FILE_TIP = '支持 jpg/jpeg/png/webp，单文件 10MB，每类最多 1 张'

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
  REPAIR_FINISH: { label: '维修登记', title: '维修登记', type: 'primary' },
  REVIEW: { label: '复检', title: '复检登记', type: 'warning' },
  UPLOAD_SEND_EXPRESS: { label: '上传寄件单号', title: '上传寄件单号', type: 'primary' },
  CLOSE: { label: '关闭工单', title: '关闭工单', type: 'danger' }
}

const LIST_MAX_PRIMARY_ACTIONS = 2

const LIST_PRIMARY_ACTION_ORDER = {
  PENDING_ASSIGN: ['ASSIGN', 'UPLOAD_SEND_EXPRESS'],
  PENDING_TECH_ACCEPT: ['TECH_ACCEPT', 'UPLOAD_SEND_EXPRESS'],
  IN_PROGRESS: ['REPAIR_FINISH', 'TRANSFER'],
  COMPLETED: ['REVIEW', 'CLOSE']
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
    barcodeResolved: false,
    barcodeQueryFailed: false,
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
    targetCompanyName: '',
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

function buildDefaultRepairPartItem() {
  return {
    partName: '',
    partQty: undefined
  }
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
    isFinished: 0,
    faultItems: [],
    faultRemark: '',
    repairDesc: '',
    repairItems: [],
    otherDesc: '',
    partList: [buildDefaultRepairPartItem()],
    faultOldImageFiles: [],
    faultNewImageFiles: [],
    machineImageFiles: [],
    machineBarcodeImageFiles: [],
    otherImageFiles: [],
    sendExpressNo: '',
    senderVoucherFiles: [],
    returnMethod: RETURN_METHOD_PICKUP,
    returnExpressNo: '',
    returnVoucherFiles: [],
    closeReason: ''
  }
}

function buildDefaultRepairProductModelForm() {
  return {
    workOrderId: undefined,
    productModel: ''
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
      createSupplementExpanded: false,
      companyAddressLoading: false,
      companyAddressList: [],
      companyAddressDialogVisible: false,
      companyAddressDialogMode: 'manage',
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
      repairProductModelDialogVisible: false,
      repairProductModelSubmitting: false,
      repairProductModelOptionsLoading: false,
      repairProductModelPendingAction: '',
      repairProductModelForm: buildDefaultRepairProductModelForm(),
      repairProductModelOptions: [],
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
    repairFaultOptionsWithOther() {
      const result = (this.actionRepairFaultOptions || [])
        .map(option => normalizeText(option && option.faultDesc))
        .filter(option => option)
      if (!result.includes(DEFAULT_OTHER_FAULT_LABEL)) {
        result.push(DEFAULT_OTHER_FAULT_LABEL)
      }
      return result
    },
    firstRepairFaultRecord() {
      const repairs = this.detail && this.detail.repairs ? this.detail.repairs : []
      for (let i = repairs.length - 1; i >= 0; i -= 1) {
        const repair = repairs[i]
        if (repair && repair.registerStage === 'REPAIR' && repair.faults && repair.faults.length) {
          return repair.faults[0]
        }
      }
      return null
    },
    firstRepairConfirmedFaultDesc() {
      return normalizeText(this.firstRepairFaultRecord && this.firstRepairFaultRecord.faultDesc)
    },
    firstRepairConfirmedFaultRemark() {
      return normalizeText(this.firstRepairFaultRecord && this.firstRepairFaultRecord.faultRemark)
    },
    firstRepairConfirmedFaultItems() {
      return this.splitFaultDescSelections(this.firstRepairConfirmedFaultDesc)
    },
    selectedRepairFaultItems() {
      if (this.actionDialogAction === 'REVIEW') {
        return this.firstRepairConfirmedFaultItems
      }
      return this.normalizeFaultItems(this.actionForm.faultItems)
    },
    currentRepairOptions() {
      const optionSet = new Set()
      this.selectedRepairFaultItems.forEach(faultDesc => {
        const matched = (this.actionRepairFaultOptions || []).find(option => option && option.faultDesc === faultDesc)
        ;((matched && matched.repairOptions) || []).forEach(item => {
          const value = normalizeText(item)
          if (value) {
            optionSet.add(value)
          }
        })
      })
      return Array.from(optionSet)
    },
    isOtherRepairSelected() {
      return (this.actionForm.repairItems || []).includes(OTHER_REPAIR_OPTION)
    },
    showRepairFaultRemarkInput() {
      return this.actionDialogAction === 'REPAIR_FINISH'
        && this.normalizeFaultItems(this.actionForm.faultItems).includes(DEFAULT_OTHER_FAULT_LABEL)
    },
    showReviewFaultRemark() {
      return this.actionDialogAction === 'REVIEW' && !!this.reviewFaultRemarkDisplay
    },
    reviewFaultDescDisplay() {
      return this.firstRepairConfirmedFaultDesc
    },
    reviewFaultRemarkDisplay() {
      return this.firstRepairConfirmedFaultRemark
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
    hasCreateResolvedBarcodeInfo() {
      return !!this.createForm.barcodeResolved
    },
    showCreateTargetCompany() {
      return this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_HQ
        && (this.createForm.targetCompanyOptions || []).length > 1
    },
    showCreateAutoTargetCompanyField() {
      return this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST
    },
    createTargetCompanyLabel() {
      return '目标总部'
    },
    isCreateTargetAutoFilled() {
      return (this.createForm.targetCompanyOptions || []).length <= 1
    },
    effectiveCreateFaultOptions() {
      if (!normalizeText(this.createForm.barcode) || this.createForm.barcodeQueryFailed) {
        return [this.createForm.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL]
      }
      return this.createForm.faultOptions || []
    },
    isCreateFaultSelectDisabled() {
      return !!normalizeText(this.createForm.barcode) && !this.createForm.barcodeQueried
    },
    showCreateFaultRemark() {
      return (this.createForm.faultItems || []).includes(this.createForm.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL)
    },
    createFaultPlaceholder() {
      if (this.isCreateFaultSelectDisabled) {
        return '请先完成条码查询'
      }
      if (!normalizeText(this.createForm.barcode) || this.createForm.barcodeQueryFailed) {
        return '无码或未解析到条码时，只能选择其它故障'
      }
      return '请选择故障描述'
    },
    createShippingAddressSummary() {
      const list = [
        normalizeText(this.createForm.senderName),
        normalizeText(this.createForm.senderMobile),
        normalizeText(this.createForm.senderAddress)
      ].filter(item => item)
      if (!list.length) {
        return ''
      }
      if (list.length === 3) {
        return `${list[0]} / ${list[1]}\n${list[2]}`
      }
      return list.join('\n')
    },
    companyAddressDialogTitle() {
      return this.companyAddressDialogMode === 'select' ? '选择寄件信息' : '公司地址簿'
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
    isCurrentView() {
      return this.queryParams.viewScope === 'CURRENT'
    },
    operationColumnWidth() {
      return this.isCurrentView ? 300 : 90
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
        this.syncCreateEntryDefaults()
        return
      }
      if (this.createForm.barcodeQueried && normalizedBarcode !== this.createForm.queriedBarcode) {
        this.resetCreateQueryState()
      }
    },
    '$route.query.detailId': {
      immediate: true,
      handler(detailId) {
        this.syncDetailFromRoute(detailId)
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
    toggleCreateSupplementSection() {
      this.createSupplementExpanded = !this.createSupplementExpanded
    },
    syncCreateEntryDefaults(options = {}) {
      if (this.createForm.entryMode !== CREATE_ENTRY_UPSTREAM_FIRST || normalizeText(this.createForm.barcode)) {
        return Promise.resolve()
      }
      return listUpstreamFirstCreateTargetOptions().then(res => {
        const list = (res && res.data) || []
        if (!Array.isArray(list) || list.length !== 1) {
          this.createForm.targetCompanyId = undefined
          this.createForm.targetCompanyName = ''
          this.createForm.targetCompanyOptions = Array.isArray(list) ? list : []
          this.$message.error('当前二级网点未带出唯一一级网点，请联系管理员排查')
          return
        }
        const target = list[0]
        this.createForm.targetCompanyId = target.id
        this.createForm.targetCompanyName = target.companyName || ''
        this.createForm.targetCompanyOptions = list
      }).catch(() => {
        this.createForm.targetCompanyId = undefined
        this.createForm.targetCompanyName = ''
        this.createForm.targetCompanyOptions = []
      })
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
      this.createSupplementExpanded = false
      this.companyAddressDialogMode = 'manage'
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
      nextForm.faultImageFiles = cloneFileItems(this.createForm.faultImageFiles)
      nextForm.faultVideoFiles = cloneFileItems(this.createForm.faultVideoFiles)
      nextForm.faultVoiceFiles = cloneFileItems(this.createForm.faultVoiceFiles)
      this.createForm = nextForm
      this.$nextTick(() => {
        if (this.$refs.createForm) {
          this.$refs.createForm.clearValidate()
        }
      })
      this.syncCreateEntryDefaults()
    },
    handleCreateTargetCompanyChange() {
      if (!this.createForm.barcodeQueried) {
        return
      }
      this.queryCreateBarcodeInfo({ preserveTargetSelection: true, silentSuccess: true })
    },
    queryCreateBarcodeInfo(options = {}) {
      const barcode = normalizeText(this.createForm.barcode)
      if (!barcode) {
        this.$message.warning('无码无需查询，可直接提交')
        return
      }
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
          this.$message.success('条码查询成功')
        }
      }).catch(() => {
        Object.assign(this.createForm, {
          queriedBarcode: barcode,
          barcodeQueried: true,
          barcodeResolved: false,
          barcodeQueryFailed: true,
          productCode: '',
          productName: '',
          productModel: '',
          machineNo: '',
          brandCode: '',
          warrantyStatus: '',
          hqCompanyId: undefined,
          hqCompanyName: '',
          faultOptions: [],
          otherFaultLabel: DEFAULT_OTHER_FAULT_LABEL,
          faultItems: [],
          faultRemark: ''
        })
        return this.syncCreateEntryDefaults({ preserveTargetSelection: true })
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
        barcodeResolved: false,
        barcodeQueryFailed: false,
        productCode: '',
        productName: '',
        productModel: '',
        machineNo: '',
        brandCode: '',
        warrantyStatus: '',
        hqCompanyId: undefined,
        hqCompanyName: '',
        targetCompanyId: preserveTargetSelection ? this.createForm.targetCompanyId : undefined,
        targetCompanyName: preserveTargetSelection ? this.createForm.targetCompanyName : '',
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
      const targetCompany = targetCompanyOptions.find(item => String(item.id) === String(targetCompanyId)) || null
      Object.assign(this.createForm, {
        barcode: data.barcode || queriedBarcode,
        queriedBarcode: data.barcode || queriedBarcode,
        barcodeQueried: true,
        barcodeResolved: true,
        barcodeQueryFailed: false,
        productCode: data.productCode || '',
        productName: data.productName || '',
        productModel: data.productModel || '',
        machineNo: data.machineNo || '',
        brandCode: data.brandCode || '',
        warrantyStatus: data.warrantyStatus || '',
        hqCompanyId: data.hqCompanyId,
        hqCompanyName: data.hqCompanyName || '',
        targetCompanyId,
        targetCompanyName: targetCompany ? (targetCompany.companyName || '') : '',
        targetCompanyOptions,
        faultOptions: data.faultOptions || [],
        otherFaultLabel: data.otherFaultLabel || DEFAULT_OTHER_FAULT_LABEL,
        faultItems: [],
        faultRemark: ''
      })
      if (this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && targetCompanyOptions.length !== 1) {
        this.createForm.targetCompanyId = undefined
        this.createForm.targetCompanyName = ''
        this.$message.error('当前二级网点未带出唯一一级网点，请联系管理员排查')
      }
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
    handleSelectCompanyAddress(address) {
      this.applySelectedCompanyAddress(address)
      this.companyAddressDialogVisible = false
      this.$nextTick(() => {
        if (this.$refs.createForm) {
          this.$refs.createForm.clearValidate(['senderName', 'senderMobile', 'senderAddress'])
        }
      })
    },
    openCompanyAddressDialog(mode = 'manage') {
      this.companyAddressDialogMode = mode
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
        const submit = () => {
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
        }
        if (!normalizeText(this.createForm.barcode)) {
          this.$confirm('未填写机器条码，将按无码工单直接提交，是否继续？', '确认提交', {
            type: 'warning'
          }).then(() => {
            submit()
          }).catch(() => {})
          return
        }
        submit()
      })
    },
    validateCreateBeforeSubmit() {
      const barcode = normalizeText(this.createForm.barcode)
      if (barcode && (!this.createForm.barcodeQueried || this.createForm.queriedBarcode !== barcode)) {
        this.$message.error('请先查询条码，再提交建单')
        return false
      }
      if (this.createForm.entryMode === CREATE_ENTRY_UPSTREAM_FIRST && !this.createForm.targetCompanyId) {
        this.$message.error('当前二级网点未带出一级网点，请联系管理员排查')
        return false
      }
      if (this.showCreateTargetCompany && !this.createForm.targetCompanyId) {
        this.$message.error(`请选择${this.createTargetCompanyLabel}`)
        return false
      }
      if (!(this.createForm.faultItems || []).length) {
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
        barcode: this.createForm.barcodeQueryFailed ? '' : normalizeText(this.createForm.barcode),
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
        this.$message.error('请选择寄件信息')
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
    fetchWorkOrderDetail(workOrderId) {
      this.detailLoading = true
      return getWorkOrder(workOrderId).then(res => {
        if (!res) {
          return null
        }
        this.detail = res.data || null
        return this.detail
      }).finally(() => {
        this.detailLoading = false
      })
    },
    openDetail(workOrderId) {
      this.detailVisible = true
      return this.fetchWorkOrderDetail(workOrderId)
    },
    normalizeRowActionCodes(row) {
      const actions = Array.isArray(row && row.availableActions) ? row.availableActions : []
      return actions.filter(action => action && action !== 'RETURN_METHOD' && ACTION_META[action])
    },
    splitRowActions(row) {
      const actionCodes = this.normalizeRowActionCodes(row)
      const primaryCodes = []
      const consumed = new Set()
      const preferredOrder = LIST_PRIMARY_ACTION_ORDER[row && row.mainStatus] || []
      preferredOrder.forEach(action => {
        if (primaryCodes.length >= LIST_MAX_PRIMARY_ACTIONS) {
          return
        }
        if (actionCodes.includes(action) && !consumed.has(action)) {
          primaryCodes.push(action)
          consumed.add(action)
        }
      })
      actionCodes.forEach(action => {
        if (primaryCodes.length >= LIST_MAX_PRIMARY_ACTIONS || consumed.has(action)) {
          return
        }
        primaryCodes.push(action)
        consumed.add(action)
      })
      const moreCodes = actionCodes.filter(action => !consumed.has(action))
      const toButton = (action) => ({
        action,
        label: ACTION_META[action].label,
        title: ACTION_META[action].title,
        type: ACTION_META[action].type
      })
      return {
        primary: primaryCodes.map(toButton),
        more: moreCodes.map(toButton)
      }
    },
    getRowPrimaryActions(row) {
      return this.splitRowActions(row).primary
    },
    getRowMoreActions(row) {
      return this.splitRowActions(row).more
    },
    shouldShowReadonlyReason(row) {
      return this.isCurrentView && !this.normalizeRowActionCodes(row).length && !!(row && row.readonlyReason)
    },
    handleListMoreAction(row, action) {
      this.handleListAction(row, action)
    },
    handleListAction(row, action) {
      const workOrderId = row && row.id
      if (!workOrderId || !action) {
        return
      }
      this.fetchWorkOrderDetail(workOrderId).then(detail => {
        if (!detail) {
          return
        }
        this.handleAction(action)
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
        if (this.shouldSupplementRepairProductModel(action)) {
          this.prepareRepairProductModelDialog(action, this.detail.id)
          return
        }
        this.openActionDialog(action, this.detail.id)
      })
    },
    shouldSupplementRepairProductModel(action) {
      return (action === 'REPAIR_FINISH' || action === 'REVIEW')
        && this.detail
        && this.detail.brandType === BRAND_TYPE_JASIC
        && !normalizeText(this.detail.productModel)
    },
    prepareRepairProductModelDialog(action, workOrderId) {
      this.repairProductModelPendingAction = action
      this.repairProductModelForm = {
        workOrderId,
        productModel: ''
      }
      this.repairProductModelOptions = []
      this.loadRepairProductModelOptions('').then(options => {
        if (!options.length) {
          this.$message.error('当前归属总部未配置启用机型，请先维护故障与维修配置')
          this.closeRepairProductModelDialog()
          return
        }
        this.repairProductModelDialogVisible = true
      })
    },
    loadRepairProductModelOptions(keyword) {
      const workOrderId = this.repairProductModelForm.workOrderId || (this.detail && this.detail.id)
      if (!workOrderId) {
        this.repairProductModelOptions = []
        return Promise.resolve([])
      }
      this.repairProductModelOptionsLoading = true
      return listRepairProductModelOptions(workOrderId, { keyword: normalizeText(keyword) }).then(res => {
        const options = (res && res.data) || []
        this.repairProductModelOptions = options
        return options
      }).finally(() => {
        this.repairProductModelOptionsLoading = false
      })
    },
    closeRepairProductModelDialog() {
      this.repairProductModelDialogVisible = false
      this.repairProductModelSubmitting = false
      this.repairProductModelPendingAction = ''
      this.repairProductModelForm = buildDefaultRepairProductModelForm()
      this.repairProductModelOptions = []
      this.repairProductModelOptionsLoading = false
    },
    submitRepairProductModel() {
      const workOrderId = this.repairProductModelForm.workOrderId
      const productModel = normalizeText(this.repairProductModelForm.productModel)
      const nextAction = this.repairProductModelPendingAction
      if (!workOrderId) {
        return
      }
      if (!productModel) {
        this.$message.error('请选择机器型号')
        return
      }
      this.repairProductModelSubmitting = true
      updateRepairProductModel({ workOrderId, productModel }).then(res => {
        if (!res) {
          return
        }
        this.$message.success('机器型号补录成功')
        this.closeRepairProductModelDialog()
        return this.openDetail(workOrderId).then(() => {
          if (nextAction) {
            this.openActionDialog(nextAction, workOrderId)
          }
        })
      }).finally(() => {
        this.repairProductModelSubmitting = false
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
          return []
        }
        const options = res.data || []
        this.actionRepairFaultOptions = options
        return options
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
      if (action === 'REPAIR_FINISH') {
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
      if (action === 'REPAIR_FINISH' || action === 'REVIEW') {
        preparePromise = this.loadRepairFaultOptions(currentWorkOrderId)
      } else {
        this.actionRepairFaultOptions = []
        this.actionRepairConfigLoading = false
      }
      this.actionForm = form
      this.pendingTechAcceptPayload = null
      this.actionDialogAction = action
      this.actionDialogTitle = ACTION_META[action] ? ACTION_META[action].title : '工单操作'
      preparePromise.then(() => {
        if (action === 'REPAIR_FINISH') {
          this.initializeRepairFaultSelection()
        }
        this.actionDialogVisible = true
      }).catch(() => {
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
      if ((this.actionDialogAction === 'REPAIR_FINISH' || this.actionDialogAction === 'REVIEW')
        && !this.validateRepairActionForm()) {
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
        if (this.actionForm.returnMethod === RETURN_METHOD_MAIL
          && !(this.actionForm.returnVoucherFiles || []).length) {
          this.$message.error('请上传回寄凭证')
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
        case 'REPAIR_FINISH':
          return {
            workOrderId,
            quoteAmount: this.actionForm.quoteAmount,
            quoteDesc: this.actionForm.quoteDesc,
            isFinished: 1,
            faultItems: this.normalizeFaultItems(this.actionForm.faultItems),
            faultRemark: normalizeText(this.actionForm.faultRemark),
            repairDesc: normalizeText(this.actionForm.repairDesc),
            repairItems: this.normalizeRepairItems(this.actionForm.repairItems),
            otherDesc: normalizeText(this.actionForm.otherDesc),
            partList: this.normalizeRepairPartList(this.actionForm.partList),
            faultOldImageFileIds: buildFileIdList(this.actionForm.faultOldImageFiles),
            faultNewImageFileIds: buildFileIdList(this.actionForm.faultNewImageFiles),
            machineImageFileIds: buildFileIdList(this.actionForm.machineImageFiles),
            machineBarcodeImageFileIds: buildFileIdList(this.actionForm.machineBarcodeImageFiles),
            otherImageFileIds: buildFileIdList(this.actionForm.otherImageFiles)
          }
        case 'REVIEW':
          return {
            workOrderId,
            repairDesc: normalizeText(this.actionForm.repairDesc),
            repairItems: this.normalizeRepairItems(this.actionForm.repairItems),
            otherDesc: normalizeText(this.actionForm.otherDesc),
            partList: this.normalizeRepairPartList(this.actionForm.partList),
            faultOldImageFileIds: buildFileIdList(this.actionForm.faultOldImageFiles),
            faultNewImageFileIds: buildFileIdList(this.actionForm.faultNewImageFiles),
            machineImageFileIds: buildFileIdList(this.actionForm.machineImageFiles),
            machineBarcodeImageFileIds: buildFileIdList(this.actionForm.machineBarcodeImageFiles),
            otherImageFileIds: buildFileIdList(this.actionForm.otherImageFiles)
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
    handleRepairItemsChange(value) {
      this.actionForm.repairItems = this.normalizeRepairItems(value)
      if (!this.isOtherRepairSelected) {
        this.actionForm.otherDesc = ''
      }
      if (this.hasRepairFaultConfig) {
        this.actionForm.repairDesc = ''
      }
    },
    handleRepairFaultItemsChange(value) {
      this.actionForm.faultItems = this.normalizeFaultItems(value)
      if (!this.showRepairFaultRemarkInput) {
        this.actionForm.faultRemark = ''
      }
      this.actionForm.repairItems = []
      this.actionForm.otherDesc = ''
      this.actionForm.repairDesc = ''
    },
    normalizeRepairItems(items) {
      return (items || [])
        .map(item => normalizeText(item))
        .filter(item => item)
    },
    normalizeFaultItems(items) {
      return Array.from(new Set((items || [])
        .map(item => normalizeText(item))
        .filter(item => item)))
    },
    normalizeRepairPartList(partList) {
      return (partList || [])
        .map(item => ({
          partName: normalizeText(item && item.partName),
          partQty: item && item.partQty !== undefined && item.partQty !== null && item.partQty !== ''
            ? Number(item.partQty)
            : undefined
        }))
        .filter(item => item.partName || item.partQty !== undefined)
    },
    addRepairPartRow() {
      this.actionForm.partList = [...(this.actionForm.partList || []), buildDefaultRepairPartItem()]
    },
    removeRepairPartRow(index) {
      const currentList = [...(this.actionForm.partList || [])]
      if (currentList.length <= 1) {
        return
      }
      currentList.splice(index, 1)
      this.actionForm.partList = currentList.length ? currentList : [buildDefaultRepairPartItem()]
    },
    faultPartRows(fault) {
      return ((fault && fault.partList) || []).filter(item => item && normalizeText(item.partName))
    },
    splitFaultDescSelections(rawFaultDesc) {
      if (!rawFaultDesc) {
        return []
      }
      return rawFaultDesc
        .split(/[；;]+/)
        .map(item => normalizeText(item))
        .filter(item => item)
    },
    validateRepairActionForm() {
      if (this.hasRepairFaultConfig && this.actionDialogAction === 'REPAIR_FINISH' && !this.selectedRepairFaultItems.length) {
        this.$message.error('请选择维修确认故障')
        return false
      }
      if (this.showRepairFaultRemarkInput && !normalizeText(this.actionForm.faultRemark)) {
        this.$message.error('选择其它故障时，必须填写其它故障说明')
        return false
      }
      if (this.hasRepairFaultConfig && this.actionDialogAction === 'REVIEW' && !this.selectedRepairFaultItems.length) {
        this.$message.error('首次维修登记未记录故障描述，无法提交复检登记')
        return false
      }
      if (this.hasRepairFaultConfig) {
        if (!this.normalizeRepairItems(this.actionForm.repairItems).length) {
          this.$message.error('请选择维修说明')
          return false
        }
        if (this.isOtherRepairSelected && !normalizeText(this.actionForm.otherDesc)) {
          this.$message.error('选择其它维修说明后，必须填写其他维修说明')
          return false
        }
      } else if (!normalizeText(this.actionForm.repairDesc)) {
        this.$message.error('请输入维修说明')
        return false
      }
      if (!this.validateRepairPartList()) {
        return false
      }
      if (!this.validateRepairFileLimit(this.actionForm.faultOldImageFiles, '故障处旧图片')
        || !this.validateRepairFileLimit(this.actionForm.faultNewImageFiles, '故障处新图片')
        || !this.validateRepairFileLimit(this.actionForm.machineImageFiles, '机器正面照片')
        || !this.validateRepairFileLimit(this.actionForm.machineBarcodeImageFiles, '机器条码照片')
        || !this.validateRepairFileLimit(this.actionForm.otherImageFiles, '其他图片')) {
        return false
      }
      return true
    },
    initializeRepairFaultSelection() {
      if (!this.hasRepairFaultConfig) {
        this.actionForm.faultItems = []
        this.actionForm.faultRemark = ''
        return
      }
      const canPrefillFromCustomer = this.detail
        && this.detail.brandType === BRAND_TYPE_JASIC
        && normalizeText(this.detail.barcode)
      if (!canPrefillFromCustomer) {
        this.actionForm.faultItems = []
        this.actionForm.faultRemark = ''
        return
      }
      const matchedItems = this.splitFaultDescSelections(normalizeText(this.detail.faultDesc))
        .filter(item => this.repairFaultOptionsWithOther.includes(item))
      this.actionForm.faultItems = matchedItems
      this.actionForm.faultRemark = matchedItems.includes(DEFAULT_OTHER_FAULT_LABEL)
        ? normalizeText(this.detail.faultRemark)
        : ''
    },
    validateRepairFileLimit(fileList, label) {
      if ((fileList || []).length > 1) {
        this.$message.error(`${label}最多只能上传1张`)
        return false
      }
      return true
    },
    validateRepairPartList() {
      const partList = this.actionForm.partList || []
      let hasValidPart = false
      for (const partItem of partList) {
        const partName = normalizeText(partItem && partItem.partName)
        const partQty = partItem && partItem.partQty
        if (!partName && (partQty === undefined || partQty === null || partQty === '')) {
          continue
        }
        if (!partName) {
          this.$message.error('请输入配件名称')
          return false
        }
        if (!partQty || Number(partQty) <= 0) {
          this.$message.error('请输入正确的配件数量')
          return false
        }
        hasValidPart = true
      }
      if (!hasValidPart) {
        this.$message.error('请至少填写一条配件明细')
        return false
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
      this.$store.dispatch('notify/fetchTodoCount').catch(() => {})
      if (shouldReloadDetail && workOrderId) {
        this.openDetail(workOrderId)
      }
    },
    syncDetailFromRoute(detailId) {
      const workOrderId = Number(detailId)
      if (!Number.isFinite(workOrderId) || workOrderId <= 0) {
        return
      }
      if (this.detailLoading) {
        return
      }
      if (this.detailVisible && this.detail && String(this.detail.id) === String(workOrderId)) {
        return
      }
      this.openDetail(workOrderId)
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
    repairAttachmentGroups(repair) {
      if (!repair) {
        return []
      }
      return [
        { key: 'old', label: '故障处旧图片', files: repair.faultOldImageFiles || [] },
        { key: 'new', label: '故障处新图片', files: repair.faultNewImageFiles || [] },
        { key: 'machine', label: '机器正面照片', files: repair.machineImageFiles || [] },
        { key: 'barcode', label: '机器条码照片', files: repair.machineBarcodeImageFiles || [] },
        { key: 'other', label: '其他图片', files: repair.otherImageFiles || [] }
      ]
    },
    hasRepairAttachmentFiles(repair) {
      return this.repairAttachmentGroups(repair).some(group => (group.files || []).length > 0)
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

.table-actions-cell {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.table-actions-cell__buttons {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  line-height: 1;
}

.table-actions-cell__reason {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}

.table-action-link {
  padding: 0;
}

.table-action-link.is-primary {
  color: #409EFF;
}

.table-action-link.is-warning {
  color: #E6A23C;
}

.table-action-link.is-danger {
  color: #F56C6C;
}

.table-action-link.is-default {
  color: #606266;
}

.table-toolbar {
  margin-bottom: 12px;
}

.create-entry-tabs {
  margin-bottom: 16px;
}

.create-section-title {
  margin: 8px 0 12px;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.create-section-title--toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
}

.create-address-picker {
  display: flex;
  gap: 12px;
}

.create-address-picker .el-textarea {
  flex: 1;
}

.create-address-picker__actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
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

.repair-card + .repair-card {
  margin-top: 12px;
}

.inner-table {
  margin-top: 12px;
}

.repair-files {
  margin-top: 12px;
  padding: 12px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
}

.repair-files__group + .repair-files__group {
  margin-top: 10px;
}

.repair-files__label {
  margin-bottom: 6px;
  font-size: 13px;
  color: #606266;
}

.repair-files__items {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
}

.action-form-message {
  width: calc(100% - 100px);
  margin: 0 0 12px 100px;
}

.repair-part-editor {
  width: 100%;
}

.repair-part-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.repair-part-row + .repair-part-row {
  margin-top: 8px;
}

.repair-part-row__name {
  flex: 1;
}

.repair-part-row__qty {
  width: 120px;
}
</style>
