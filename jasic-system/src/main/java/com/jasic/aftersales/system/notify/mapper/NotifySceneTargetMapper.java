package com.jasic.aftersales.system.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.notify.domain.entity.NotifySceneTarget;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知场景目标配置 Mapper。
 *
 * <p>负责 `notify_scene_target` 的基础读写，不承载复杂业务逻辑。</p>
 *
 * @author Codex
 * @date 2026/05/16
 */
@Mapper
public interface NotifySceneTargetMapper extends BaseMapper<NotifySceneTarget> {
}
