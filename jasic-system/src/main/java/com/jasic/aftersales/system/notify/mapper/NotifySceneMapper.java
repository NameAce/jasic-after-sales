package com.jasic.aftersales.system.notify.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.jasic.aftersales.system.notify.domain.entity.NotifyScene;
import org.apache.ibatis.annotations.Mapper;

/**
 * 通知场景 Mapper。
 *
 * <p>负责 `notify_scene` 的基础读写，不承载复杂业务逻辑。</p>
 *
 * @author Zoro
 * @date 2026/05/16
 */
@Mapper
public interface NotifySceneMapper extends BaseMapper<NotifyScene> {
}
