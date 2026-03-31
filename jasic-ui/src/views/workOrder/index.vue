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
        <el-radio-group :value="activeMainStatus" size="small" @change="handleMainStatusChange">
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
              {{ statusLabel(row.displayStatus || row.mainStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="当前受理公司" prop="currentAcceptCompanyName" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前维修员" prop="assignedUserName" min-width="110" />
        <el-table-column label="关系" min-width="120">
          <template slot-scope="{ row }">
            {{ relationLabel(row.relationType) }}
          </template>
        </el-table-column>
        <el-table-column label="转单" min-width="80" align="center">
          <template slot-scope="{ row }">
            <el-tag :type="row.hasTransfer === 1 ? 'warning' : 'info'" size="mini">
              {{ row.hasTransfer === 1 ? '是' : '否' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" min-width="160" />
        <el-table-column label="操作" fixed="right" width="150">
          <template slot-scope="{ row }">
            <el-button v-if="canShowRowAssign(row)" type="text" size="mini" @click="handleAssignRow(row)">派单</el-button>
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
      <el-form ref="createForm" :model="createForm" :rules="createRules" label-width="96px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="客户姓名" prop="customerName">
              <el-input v-model="createForm.customerName" placeholder="请输入客户姓名" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户手机号" prop="customerMobile">
              <el-input v-model="createForm.customerMobile" placeholder="请输入客户手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="条码" prop="barcode">
              <el-input v-model="createForm.barcode" placeholder="请输入条码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="归属总部" prop="hqCompanyId">
              <el-select v-model="createForm.hqCompanyId" placeholder="请选择归属总部" filterable>
                <el-option v-for="item in createHqOptions" :key="item.id" :label="item.companyName" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="物料编码" prop="productCode">
              <el-input v-model="createForm.productCode" placeholder="请输入物料编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="机型" prop="productModel">
              <el-input v-model="createForm.productModel" placeholder="请输入机型" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="品牌编码" prop="brandCode">
              <el-input v-model="createForm.brandCode" placeholder="请输入品牌编码" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="服务方式" prop="serviceMode">
              <el-select v-model="createForm.serviceMode" placeholder="请选择服务方式">
                <el-option v-for="item in serviceModeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="质保状态" prop="warrantyStatus">
              <el-input v-model="createForm.warrantyStatus" placeholder="请输入质保状态" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="故障描述" prop="faultDesc">
              <el-input v-model="createForm.faultDesc" type="textarea" :rows="3" placeholder="请输入故障描述" />
            </el-form-item>
          </el-col>
        </el-row>

        <div v-if="isCreateMailMode" class="section-title">寄修信息</div>
        <el-row v-if="isCreateMailMode" :gutter="16">
          <el-col :span="12">
            <el-form-item label="寄件人" prop="senderName">
              <el-input v-model="createForm.senderName" placeholder="请输入寄件人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="寄件手机号" prop="senderMobile">
              <el-input v-model="createForm.senderMobile" placeholder="请输入寄件手机号" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="寄件地址" prop="senderAddress">
              <el-input v-model="createForm.senderAddress" placeholder="请输入寄件地址" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="寄件单号" prop="sendExpressNo">
              <el-input v-model="createForm.sendExpressNo" placeholder="请输入寄件快递单号" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer">
        <el-button @click="createDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="createSubmitting" @click="submitCreate">确定</el-button>
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
            <el-descriptions-item label="主状态">{{ statusLabel(detail.mainStatus) }}</el-descriptions-item>
            <el-descriptions-item label="客户姓名">{{ textValue(detail.customerName) }}</el-descriptions-item>
            <el-descriptions-item label="客户手机号">{{ textValue(detail.customerMobile) }}</el-descriptions-item>
            <el-descriptions-item label="条码">{{ textValue(detail.barcode) }}</el-descriptions-item>
            <el-descriptions-item label="物料编码">{{ textValue(detail.productCode) }}</el-descriptions-item>
            <el-descriptions-item label="机型">{{ textValue(detail.productModel) }}</el-descriptions-item>
            <el-descriptions-item label="品牌编码">{{ textValue(detail.brandCode) }}</el-descriptions-item>
            <el-descriptions-item label="服务方式">{{ textValue(detail.serviceMode) }}</el-descriptions-item>
            <el-descriptions-item label="质保状态">{{ textValue(detail.warrantyStatus) }}</el-descriptions-item>
            <el-descriptions-item label="当前受理公司">{{ textValue(detail.currentAcceptCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="当前维修员">{{ textValue(detail.assignedUserName) }}</el-descriptions-item>
            <el-descriptions-item label="建单公司">{{ textValue(detail.createCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="归属总部">{{ textValue(detail.hqCompanyName) }}</el-descriptions-item>
            <el-descriptions-item label="转单次数">{{ detail.transferCount || 0 }}</el-descriptions-item>
            <el-descriptions-item label="评价状态">{{ textValue(detail.evaluateStatus) }}</el-descriptions-item>
            <el-descriptions-item label="故障描述" :span="2">{{ textValue(detail.faultDesc) }}</el-descriptions-item>
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

          <div class="section-title">参与方</div>
          <el-table :data="detail.participants || []" border size="small">
            <el-table-column label="公司" prop="companyName" min-width="160" />
            <el-table-column label="主体类型" prop="subjectType" min-width="100" />
            <el-table-column label="参与类型" prop="participateType" min-width="120" />
            <el-table-column label="当前处理方" min-width="100">
              <template slot-scope="{ row }">{{ yesNoText(row.isCurrentHandler) }}</template>
            </el-table-column>
            <el-table-column label="只读" min-width="80">
              <template slot-scope="{ row }">{{ yesNoText(row.isReadonly) }}</template>
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
            <el-descriptions-item label="评分">{{ textValue(detail.evaluation.score) }}</el-descriptions-item>
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

    <el-dialog :title="actionDialogTitle" :visible.sync="actionDialogVisible" width="640px" append-to-body>
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

        <template v-else-if="actionDialogAction === 'QUOTE'">
          <el-form-item label="故障判断">
            <el-input v-model="actionForm.faultJudge" type="textarea" :rows="3" placeholder="请输入故障判断" />
          </el-form-item>
          <el-form-item label="报价金额">
            <el-input-number v-model="actionForm.quoteAmount" :min="0" :precision="2" :controls="false" style="width: 100%;" />
          </el-form-item>
          <el-form-item label="报价说明">
            <el-input v-model="actionForm.quoteDesc" type="textarea" :rows="3" placeholder="请输入报价说明" />
          </el-form-item>
        </template>

        <template v-else-if="actionDialogAction === 'REPAIR_SAVE' || actionDialogAction === 'REPAIR_FINISH'">
          <el-form-item label="维修摘要">
            <el-input v-model="actionForm.repairSummary" placeholder="请输入维修摘要" />
          </el-form-item>
          <el-form-item label="维修说明">
            <el-input v-model="actionForm.repairDesc" type="textarea" :rows="3" placeholder="请输入维修说明" />
          </el-form-item>
          <el-form-item label="其他说明">
            <el-input v-model="actionForm.otherDesc" type="textarea" :rows="3" placeholder="请输入其他说明" />
          </el-form-item>
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
              <el-input v-model="item.faultDesc" placeholder="请输入故障描述" />
            </el-form-item>
            <el-form-item label="维修说明">
              <el-input v-model="item.repairDesc" type="textarea" :rows="2" placeholder="请输入维修说明" />
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
          <el-form-item label="关闭原因" required>
            <el-input v-model="actionForm.closeReason" type="textarea" :rows="3" placeholder="请输入关闭原因" />
          </el-form-item>
        </template>
      </el-form>
      <div slot="footer">
        <el-button @click="actionDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="actionSubmitting" @click="submitAction">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  addWorkOrder,
  assignWorkOrder,
  countWorkOrderStatus,
  closeWorkOrder,
  getWorkOrder,
  listAssignUserOptions,
  listCreateHqOptions,
  listTransferTargetOptions,
  listWorkOrder,
  quoteWorkOrder,
  repairWorkOrder,
  reviewWorkOrder,
  techAcceptWorkOrder,
  transferWorkOrder,
  updateWorkOrderSendExpress
} from '@/api/workOrder'

const SERVICE_MODE_MAIL = '寄修'
const SERVICE_MODE_STORE = '到店维修'
const RETURN_METHOD_MAIL = '回寄'
const RETURN_METHOD_PICKUP = '自提'
const REVIEW_RESULT_PASS = '通过'
const REVIEW_RESULT_CONTINUE = '继续维修'

const STATUS_LABELS = {
  PENDING_ASSIGN: '待派单',
  PENDING_TECH_ACCEPT: '待维修员接单',
  IN_PROGRESS: '维修中',
  COMPLETED: '已完成',
  CLOSED: '已关闭'
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
    customerId: undefined,
    customerName: '',
    customerMobile: '',
    barcode: '',
    productCode: '',
    productModel: '',
    brandCode: '',
    serviceMode: SERVICE_MODE_STORE,
    warrantyStatus: '',
    faultDesc: '',
    senderName: '',
    senderMobile: '',
    senderAddress: '',
    sendExpressNo: '',
    hqCompanyId: undefined
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
    repairSummary: '',
    repairDesc: '',
    otherDesc: '',
    isFinished: 0,
    faults: [createFaultItem()],
    reviewResult: REVIEW_RESULT_PASS,
    reviewDesc: '',
    isContinueRepair: 0,
    sendExpressNo: '',
    returnMethod: RETURN_METHOD_PICKUP,
    returnExpressNo: '',
    closeReason: ''
  }
}

export default {
  name: 'WorkOrderManage',
  data() {
    return {
      serviceModeMail: SERVICE_MODE_MAIL,
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
        { label: '待维修员接单', value: 'PENDING_TECH_ACCEPT' },
        { label: '维修中', value: 'IN_PROGRESS' },
        { label: '已完成', value: 'COMPLETED' },
        { label: '已关闭', value: 'CLOSED' }
      ],
      hasTransferOptions: [
        { label: '是', value: 1 },
        { label: '否', value: 0 }
      ],
      serviceModeOptions: [
        { label: SERVICE_MODE_STORE, value: SERVICE_MODE_STORE },
        { label: SERVICE_MODE_MAIL, value: SERVICE_MODE_MAIL }
      ],
      createDialogVisible: false,
      createSubmitting: false,
      createForm: buildDefaultCreateForm(),
      createHqOptions: [],
      createRules: {
        customerName: [{ required: true, message: '请输入客户姓名', trigger: 'blur' }],
        customerMobile: [{ required: true, message: '请输入客户手机号', trigger: 'blur' }],
        serviceMode: [{ required: true, message: '请选择服务方式', trigger: 'change' }],
        hqCompanyId: [{ required: true, message: '请选择归属总部', trigger: 'change' }]
      },
      detailVisible: false,
      detailLoading: false,
      detail: null,
      actionDialogVisible: false,
      actionDialogAction: '',
      actionDialogTitle: '',
      actionSubmitting: false,
      actionForm: buildDefaultActionForm(),
      assignUserOptions: [],
      transferTargetOptions: []
    }
  },
  computed: {
    isCreateMailMode() {
      return this.createForm.serviceMode === SERVICE_MODE_MAIL
    },
    activeMainStatus() {
      return this.queryParams.mainStatus || 'ALL'
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
  created() {
    this.loadCreateHqOptions()
    this.getList()
  },
  methods: {
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
    loadCreateHqOptions() {
      return listCreateHqOptions().then(res => {
        if (!res) {
          return
        }
        this.createHqOptions = res.data || []
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleScopeChange() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleMainStatusChange(value) {
      this.queryParams.mainStatus = value === 'ALL' ? '' : value
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
      this.createForm = buildDefaultCreateForm()
      this.createDialogVisible = true
      this.loadCreateHqOptions()
      this.$nextTick(() => {
        if (this.$refs.createForm) {
          this.$refs.createForm.clearValidate()
        }
      })
    },
    submitCreate() {
      this.$refs.createForm.validate(valid => {
        if (!valid) {
          return
        }
        if (this.isCreateMailMode && !this.validateSendInfo(this.createForm)) {
          return
        }
        const payload = Object.assign({}, this.createForm)
        if (!this.isCreateMailMode) {
          payload.senderName = ''
          payload.senderMobile = ''
          payload.senderAddress = ''
          payload.sendExpressNo = ''
        }
        this.createSubmitting = true
        addWorkOrder(payload).then(res => {
          if (!res) {
            return
          }
          this.$message.success('新增成功')
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
    validateSendInfo(form) {
      if (!form.senderName) {
        this.$message.error('请输入寄件人')
        return false
      }
      if (!form.senderMobile) {
        this.$message.error('请输入寄件手机号')
        return false
      }
      if (!form.senderAddress) {
        this.$message.error('请输入寄件地址')
        return false
      }
      return true
    },
    handleView(row) {
      this.openDetail(row.id)
    },
    handleAssignRow(row) {
      if (!row || !this.canShowRowAssign(row)) {
        return
      }
      this.loadAssignUserOptions(row.id).then(() => {
        this.openActionDialog('ASSIGN', row.id)
      })
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
      if (action === 'TECH_ACCEPT') {
        this.confirmTechAccept()
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
    openActionDialog(action, workOrderId) {
      const currentWorkOrderId = workOrderId || (this.detail && this.detail.id)
      if (!currentWorkOrderId) {
        return
      }
      const form = buildDefaultActionForm()
      form.workOrderId = currentWorkOrderId
      if (action === 'REPAIR_FINISH') {
        form.isFinished = 1
      }
      if (action === 'UPLOAD_SEND_EXPRESS') {
        form.sendExpressNo = this.detail.sendExpressNo || ''
      }
      if (action === 'CLOSE') {
        form.returnMethod = this.detail.returnMethod || (this.detail.serviceMode === SERVICE_MODE_MAIL ? RETURN_METHOD_MAIL : RETURN_METHOD_PICKUP)
        form.returnExpressNo = this.detail.returnExpressNo || ''
        form.closeReason = this.detail.closeReason || ''
      }
      this.actionForm = form
      this.actionDialogAction = action
      this.actionDialogTitle = ACTION_META[action] ? ACTION_META[action].title : '工单操作'
      this.actionDialogVisible = true
    },
    confirmTechAccept() {
      this.$confirm('确认维修员接单吗？', '提示', { type: 'warning' }).then(() => {
        this.actionSubmitting = true
        return techAcceptWorkOrder({ workOrderId: this.detail.id })
      }).then(res => {
        if (!res) {
          return
        }
        this.$message.success('接单成功')
        this.refreshAfterAction()
      }).catch(() => {}).finally(() => {
        this.actionSubmitting = false
      })
    },
    submitAction() {
      if (!this.validateAction()) {
        return
      }
      const payload = this.buildActionPayload()
      if (!payload) {
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
      if ((this.actionDialogAction === 'REPAIR_SAVE' || this.actionDialogAction === 'REPAIR_FINISH')
        && !this.actionForm.repairSummary
        && !this.actionForm.repairDesc
        && !this.actionForm.otherDesc
        && this.buildRepairFaults().length === 0) {
        this.$message.error('请至少填写一项维修内容')
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
          return { workOrderId, sendExpressNo: this.actionForm.sendExpressNo }
        case 'CLOSE':
          return {
            workOrderId,
            returnMethod: this.actionForm.returnMethod,
            returnExpressNo: this.actionForm.returnMethod === RETURN_METHOD_MAIL ? this.actionForm.returnExpressNo : '',
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
          return closeWorkOrder(payload)
        default:
          return null
      }
    },
    buildRepairFaults() {
      return (this.actionForm.faults || []).map(item => ({
        faultDesc: item.faultDesc ? item.faultDesc.trim() : '',
        repairDesc: item.repairDesc ? item.repairDesc.trim() : '',
        partDesc: item.partDesc ? item.partDesc.trim() : '',
        imageUrls: item.imageUrls ? item.imageUrls.trim() : ''
      })).filter(item => item.faultDesc || item.repairDesc || item.partDesc || item.imageUrls)
    },
    addFaultItem() {
      this.actionForm.faults.push(createFaultItem())
    },
    removeFaultItem(index) {
      this.actionForm.faults.splice(index, 1)
    },
    refreshAfterAction() {
      const workOrderId = this.resolveActionWorkOrderId()
      const shouldReloadDetail = this.detailVisible && this.detail && String(this.detail.id) === String(workOrderId)
      this.actionDialogVisible = false
      this.getList()
      if (shouldReloadDetail && workOrderId) {
        this.openDetail(workOrderId)
      }
    },
    statusLabel(status) {
      return STATUS_LABELS[status] || status || '-'
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
    relationLabel(relationType) {
      if (relationType === 'CURRENT_ASSIGNEE') {
        return '我的工单'
      }
      if (relationType === 'CURRENT_OWNER_MANAGER' || relationType === 'CURRENT_OWNER_MEMBER') {
        return '当前处理'
      }
      if (relationType === 'HQ_OBSERVER') {
        return '总部只读'
      }
      if (relationType === 'HISTORY_PARTICIPANT_READONLY') {
        return '历史只读'
      }
      return relationType || '-'
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
    canShowRowAssign(row) {
      if (!row) {
        return false
      }
      if (!this.hasPerm('workorder:assign')) {
        return false
      }
      if (row.mainStatus !== 'PENDING_ASSIGN') {
        return false
      }
      if (row.assignedUserId) {
        return false
      }
      if (row.relationType === 'CURRENT_OWNER_MANAGER') {
        return true
      }
      return String(row.currentAcceptCompanyId) === String(this.$store.getters.currentCompanyId)
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
