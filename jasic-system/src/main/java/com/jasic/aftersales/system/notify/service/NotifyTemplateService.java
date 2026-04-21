package com.jasic.aftersales.system.notify.service;

import com.jasic.aftersales.common.core.domain.PageResult;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplateDTO;
import com.jasic.aftersales.system.notify.domain.dto.NotifyTemplatePreviewDTO;
import com.jasic.aftersales.system.notify.domain.query.NotifyTemplateQuery;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplatePreviewVO;
import com.jasic.aftersales.system.notify.domain.vo.NotifyTemplateVO;
import com.jasic.aftersales.system.notify.support.NotifyTemplateRenderResult;

import java.util.Map;

public interface NotifyTemplateService {

    PageResult<NotifyTemplateVO> listPage(NotifyTemplateQuery query);

    NotifyTemplateVO getById(Long id);

    Long saveCustom(NotifyTemplateDTO dto);

    void updateCustom(NotifyTemplateDTO dto);

    void removeCustom(Long id);

    NotifyTemplatePreviewVO preview(NotifyTemplatePreviewDTO dto);

    NotifyTemplateRenderResult render(String templateCode, Map<String, Object> variables);

    void refreshCache();
}
