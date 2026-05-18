package com.jasic.aftersales.system.notify.support;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知事件执行上下文。
 *
 * <p>阶段二开始，事件处理器不再直接决定“落站内消息还是落外部分发”，
 * 而是统一负责把业务事件解析成运行时上下文。
 * 事件消费编排层再基于该上下文，按场景下启用的多个通知目标逐个渲染并分流执行。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Data
public class NotifyEventExecutionContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 通知场景编码。
     */
    private String sceneCode;

    /**
     * 接收对象类型。
     */
    private String receiverType;

    /**
     * 接收对象ID。
     */
    private Long receiverId;

    /**
     * 站内目标使用的接收公司ID。
     */
    private Long receiverCompanyId;

    /**
     * 接收对象名称快照。
     */
    private String receiverName;

    /**
     * 外部目标使用的接收地址快照。
     *
     * <p>当前 `MP_SUBSCRIBE` 对应微信 openid。</p>
     */
    private String receiverAddress;

    /**
     * 模板变量快照。
     *
     * <p>该快照在事件消费阶段即固定下来，避免后续分发重试时回查最新业务数据。</p>
     */
    private Map<String, Object> templateVariables = new LinkedHashMap<>();

    /**
     * 站内消息扩展字段快照。
     */
    private String messageExtJson;

    /**
     * 按接收对象类型归档的接收人快照。
     *
     * <p>当同一个场景同时启用多个通知目标，且不同目标指向不同接收对象时，
     * 统一由该映射提供“按目标取接收人”的能力。</p>
     */
    private Map<String, List<NotifyReceiverSnapshot>> receiverSnapshots = new LinkedHashMap<>();

    /**
     * 追加接收人快照。
     *
     * @param snapshot 接收人快照
     */
    public void addReceiverSnapshot(NotifyReceiverSnapshot snapshot) {
        if (snapshot == null || snapshot.getReceiverType() == null) {
            return;
        }
        addReceiverSnapshots(snapshot.getReceiverType(), Collections.singletonList(snapshot));
    }

    /**
     * 按接收对象类型追加一组接收人快照。
     *
     * <p>阶段二开始，B 端网点级通知需要支持“一单多人”。
     * 因此这里统一把同一 `receiverType` 下的多个接收人都固化到上下文中，
     * 由消费层在创建分发表时逐个展开，避免业务层自行循环发送而绕过主链路。</p>
     *
     * @param receiverType 接收对象类型
     * @param snapshots 接收人快照列表
     */
    public void addReceiverSnapshots(String receiverType, List<NotifyReceiverSnapshot> snapshots) {
        if (receiverType == null || snapshots == null || snapshots.isEmpty()) {
            return;
        }
        if (receiverSnapshots == null) {
            receiverSnapshots = new LinkedHashMap<>();
        }
        List<NotifyReceiverSnapshot> mergedSnapshots = receiverSnapshots.computeIfAbsent(
                receiverType,
                key -> new ArrayList<>()
        );
        for (NotifyReceiverSnapshot snapshot : snapshots) {
            if (snapshot == null) {
                continue;
            }
            mergedSnapshots.add(snapshot);
        }
    }

    /**
     * 按接收对象类型读取接收人快照。
     *
     * <p>为兼容仍只写入单接收人的旧 handler，
     * 当映射中未命中时会尝试回退到上下文主接收人字段。</p>
     *
     * @param receiverType 接收对象类型
     * @return 接收人快照；未命中时返回 {@code null}
     */
    public NotifyReceiverSnapshot getReceiverSnapshot(String receiverType) {
        if (receiverType == null) {
            return null;
        }
        List<NotifyReceiverSnapshot> snapshots = getReceiverSnapshots(receiverType);
        if (!snapshots.isEmpty()) {
            return snapshots.get(0);
        }
        if (receiverType.equals(this.receiverType)) {
            return NotifyReceiverSnapshot.of(
                    this.receiverType,
                    this.receiverId,
                    this.receiverCompanyId,
                    this.receiverName,
                    this.receiverAddress
            );
        }
        return null;
    }

    /**
     * 按接收对象类型读取接收人快照列表。
     *
     * <p>为兼容仍然只写入单接收人的旧 handler，
     * 当多接收人映射未命中时，会尝试回退到上下文主接收人字段。</p>
     *
     * @param receiverType 接收对象类型
     * @return 接收人快照列表；未命中时返回空列表
     */
    public List<NotifyReceiverSnapshot> getReceiverSnapshots(String receiverType) {
        if (receiverType == null) {
            return Collections.emptyList();
        }
        List<NotifyReceiverSnapshot> snapshots = receiverSnapshots == null ? null : receiverSnapshots.get(receiverType);
        if (snapshots != null && !snapshots.isEmpty()) {
            return Collections.unmodifiableList(snapshots);
        }
        NotifyReceiverSnapshot fallbackSnapshot = receiverType.equals(this.receiverType)
                ? NotifyReceiverSnapshot.of(
                this.receiverType,
                this.receiverId,
                this.receiverCompanyId,
                this.receiverName,
                this.receiverAddress
        )
                : null;
        return fallbackSnapshot == null
                ? Collections.emptyList()
                : Collections.singletonList(fallbackSnapshot);
    }

    /**
     * 读取只读接收人快照映射。
     *
     * @return 接收人快照映射
     */
    public Map<String, List<NotifyReceiverSnapshot>> getReceiverSnapshots() {
        if (receiverSnapshots == null) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(receiverSnapshots);
    }
}
