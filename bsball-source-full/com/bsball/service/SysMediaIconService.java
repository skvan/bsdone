/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysMediaIcon
 *  com.bsball.repository.SysMediaIconRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysMediaIconService
 *  com.bsball.service.TenantQueryPolicyService
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  lombok.Generated
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.transaction.annotation.Transactional
 *  org.springframework.web.multipart.MultipartFile
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysMediaIcon;
import com.bsball.repository.SysMediaIconRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.TenantQueryPolicyService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import lombok.Generated;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class SysMediaIconService {
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{0,127}$");
    private static final Pattern FONT_SYMBOL_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_-]{0,127}$");
    private static final ObjectMapper JSON = new ObjectMapper();
    private final SysMediaIconRepository repository;
    private final ApiPermissionService apiPermissionService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    private boolean superAdmin() {
        Long uid = CurrentUserHolder.get();
        return uid != null && this.apiPermissionService.isSuperAdmin(uid);
    }

    public PageResult<SysMediaIcon> list(Integer page, Integer pageSize, String keyword, String tag) {
        PageRequest p = PageRequest.of((int)Math.max((page != null ? page : 1) - 1, 0), (int)PaginationSupport.resolvePageSize((Integer)pageSize), (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"id"}));
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        boolean kw = keyword != null && !keyword.isBlank();
        boolean tagFilter = tag != null && !tag.isBlank();
        String tagLower = tagFilter ? tag.trim().toLowerCase() : "";
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList ps = new ArrayList();
            ps.add(cb.isNull((Expression)root.get("deletedAt")));
            if (tid != null) {
                ps.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
            }
            if (kw) {
                String k = "%" + keyword.trim().toLowerCase() + "%";
                ps.add(cb.or(new Predicate[]{cb.like(cb.lower((Expression)root.get("name")), k), cb.like(cb.lower((Expression)root.get("fontSymbol")), k), cb.like(cb.lower((Expression)root.get("tags")), k), cb.like(cb.lower((Expression)root.get("remark")), k)}));
            }
            if (tagFilter) {
                ps.add(cb.like(cb.lower((Expression)root.get("tags")), "%" + tagLower + "%"));
            }
            return cb.and((Predicate[])ps.toArray(Predicate[]::new));
        };
        Page result = this.repository.findAll((Specification)spec, (Pageable)p);
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysMediaIcon get(Long id) {
        SysMediaIcon e = (SysMediaIcon)this.repository.findById((Object)id).orElseThrow(() -> new BusinessException(404, "\u56fe\u6807\u4e0d\u5b58\u5728"));
        this.assertTenant(e);
        return e;
    }

    private void assertTenant(SysMediaIcon e) {
        if (this.superAdmin()) {
            return;
        }
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (e.getTenantId() == null || e.getTenantId() != tid) {
            throw new BusinessException(403, "\u65e0\u6743\u8bbf\u95ee\u8be5\u56fe\u6807");
        }
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new BusinessException(400, "\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String n = name.trim();
        if (!NAME_PATTERN.matcher(n).matches()) {
            throw new BusinessException(400, "\u540d\u79f0\u987b\u4ee5\u5b57\u6bcd\u5f00\u5934\uff0c\u4ec5\u542b\u5b57\u6bcd\u3001\u6570\u5b57\u3001\u4e0b\u5212\u7ebf\u3001\u8fde\u5b57\u7b26");
        }
    }

    private void validateFontSymbolOptional(String fontSymbol) {
        if (fontSymbol == null || fontSymbol.isBlank()) {
            return;
        }
        String s = fontSymbol.trim();
        if (!FONT_SYMBOL_PATTERN.matcher(s).matches()) {
            throw new BusinessException(400, "Font Class \u987b\u4ee5\u5b57\u6bcd\u5f00\u5934\uff0c\u4ec5\u542b\u5b57\u6bcd\u3001\u6570\u5b57\u3001\u4e0b\u5212\u7ebf\u3001\u8fde\u5b57\u7b26");
        }
    }

    private void validatePartStylesJsonOptional(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        try {
            JsonNode root = JSON.readTree(json);
            if (!root.isArray()) {
                throw new BusinessException(400, "\u5206\u6bb5\u6837\u5f0f\u987b\u4e3a JSON \u6570\u7ec4");
            }
            for (JsonNode n : root) {
                if (n.isObject() && n.has("index") && n.get("index").isNumber()) continue;
                throw new BusinessException(400, "\u5206\u6bb5\u6837\u5f0f\u6bcf\u9879\u987b\u542b\u6570\u5b57 index");
            }
        }
        catch (BusinessException e) {
            throw e;
        }
        catch (Exception e) {
            throw new BusinessException(400, "\u5206\u6bb5\u6837\u5f0f JSON \u65e0\u6548");
        }
    }

    private boolean existsFontSymbol(long tenantId, String fontSymbol, Long excludeId) {
        if (fontSymbol == null || fontSymbol.isBlank()) {
            return false;
        }
        String fs = fontSymbol.trim();
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList ps = new ArrayList();
            ps.add(cb.isNull((Expression)root.get("deletedAt")));
            ps.add(cb.equal((Expression)root.get("tenantId"), (Object)tenantId));
            ps.add(cb.equal((Expression)root.get("fontSymbol"), (Object)fs));
            if (excludeId != null) {
                ps.add(cb.notEqual((Expression)root.get("id"), (Object)excludeId));
            }
            return cb.and((Predicate[])ps.toArray(Predicate[]::new));
        };
        return this.repository.count((Specification)spec) > 0L;
    }

    private void validateSvg(String svg) {
        if (svg == null || svg.isBlank()) {
            throw new BusinessException(400, "SVG \u5185\u5bb9\u4e0d\u80fd\u4e3a\u7a7a");
        }
        String s = svg.trim();
        if (!s.toLowerCase().contains("<svg")) {
            throw new BusinessException(400, "\u8bf7\u4e0a\u4f20\u6709\u6548\u7684 SVG\uff08\u9700\u5305\u542b <svg>\uff09");
        }
    }

    @Transactional
    public SysMediaIcon create(SysMediaIcon body) {
        this.validateName(body.getName());
        this.validateSvg(body.getSvgContent());
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        if (this.existsName(tid, body.getName().trim(), null)) {
            throw new BusinessException(400, "\u540d\u79f0\u5df2\u5b58\u5728");
        }
        String fontSym = Optional.ofNullable(body.getFontSymbol()).map(String::trim).filter(s -> !s.isEmpty()).orElse(body.getName().trim());
        this.validateFontSymbolOptional(fontSym);
        if (this.existsFontSymbol(tid, fontSym, null)) {
            throw new BusinessException(400, "Font Class \u5df2\u5b58\u5728");
        }
        this.validatePartStylesJsonOptional(body.getPartStylesJson());
        SysMediaIcon e = new SysMediaIcon();
        e.setTenantId(Long.valueOf(tid));
        e.setName(body.getName().trim());
        e.setSvgContent(body.getSvgContent().trim());
        e.setTags((String)Optional.ofNullable(body.getTags()).map(String::trim).orElse(null));
        e.setScale(Double.valueOf(body.getScale() != null ? body.getScale() : 1.0));
        e.setRotateDeg(Double.valueOf(body.getRotateDeg() != null ? body.getRotateDeg() : 0.0));
        e.setFontSymbol(fontSym);
        e.setOffsetX(Double.valueOf(body.getOffsetX() != null ? body.getOffsetX() : 0.0));
        e.setOffsetY(Double.valueOf(body.getOffsetY() != null ? body.getOffsetY() : 0.0));
        e.setFillColor(Optional.ofNullable(body.getFillColor()).map(String::trim).filter(s -> !s.isEmpty()).orElse("currentColor"));
        e.setPartStylesJson((String)Optional.ofNullable(body.getPartStylesJson()).map(String::trim).filter(s -> !s.isEmpty()).orElse(null));
        e.setRemark((String)Optional.ofNullable(body.getRemark()).map(String::trim).orElse(null));
        return (SysMediaIcon)this.repository.save((Object)e);
    }

    private boolean existsName(long tenantId, String name, Long excludeId) {
        Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
            ArrayList ps = new ArrayList();
            ps.add(cb.isNull((Expression)root.get("deletedAt")));
            ps.add(cb.equal((Expression)root.get("tenantId"), (Object)tenantId));
            ps.add(cb.equal((Expression)root.get("name"), (Object)name));
            if (excludeId != null) {
                ps.add(cb.notEqual((Expression)root.get("id"), (Object)excludeId));
            }
            return cb.and((Predicate[])ps.toArray(Predicate[]::new));
        };
        return this.repository.count((Specification)spec) > 0L;
    }

    @Transactional
    public SysMediaIcon update(Long id, SysMediaIcon body) {
        SysMediaIcon e = this.get(id);
        if (body.getName() != null) {
            this.validateName(body.getName());
            String nn = body.getName().trim();
            if (!nn.equals(e.getName()) && this.existsName(e.getTenantId().longValue(), nn, id)) {
                throw new BusinessException(400, "\u540d\u79f0\u5df2\u5b58\u5728");
            }
            e.setName(nn);
        }
        if (body.getSvgContent() != null) {
            this.validateSvg(body.getSvgContent());
            e.setSvgContent(body.getSvgContent().trim());
        }
        if (body.getTags() != null) {
            e.setTags(body.getTags().trim().isEmpty() ? null : body.getTags().trim());
        }
        if (body.getScale() != null) {
            e.setScale(body.getScale());
        }
        if (body.getRotateDeg() != null) {
            e.setRotateDeg(body.getRotateDeg());
        }
        if (body.getFontSymbol() != null) {
            String fs = body.getFontSymbol().trim();
            if (fs.isEmpty()) {
                fs = e.getName();
            }
            this.validateFontSymbolOptional(fs);
            if (!fs.equals(Optional.ofNullable(e.getFontSymbol()).orElse("")) && this.existsFontSymbol(e.getTenantId().longValue(), fs, id)) {
                throw new BusinessException(400, "Font Class \u5df2\u5b58\u5728");
            }
            e.setFontSymbol(fs);
        }
        if (body.getOffsetX() != null) {
            e.setOffsetX(body.getOffsetX());
        }
        if (body.getOffsetY() != null) {
            e.setOffsetY(body.getOffsetY());
        }
        if (body.getFillColor() != null) {
            e.setFillColor(body.getFillColor().trim().isEmpty() ? "currentColor" : body.getFillColor().trim());
        }
        if (body.getPartStylesJson() != null) {
            String p = body.getPartStylesJson().trim();
            this.validatePartStylesJsonOptional(p.isEmpty() ? null : p);
            e.setPartStylesJson(p.isEmpty() ? null : p);
        }
        if (body.getRemark() != null) {
            e.setRemark(body.getRemark().trim().isEmpty() ? null : body.getRemark().trim());
        }
        return (SysMediaIcon)this.repository.save((Object)e);
    }

    @Transactional
    public void delete(Long id) {
        SysMediaIcon e = this.get(id);
        Long uid = CurrentUserHolder.get();
        e.setDeletedAt(LocalDateTime.now());
        e.setDeletedBy(uid);
        this.repository.save((Object)e);
    }

    @Transactional
    public SysMediaIcon upload(MultipartFile file, String nameOverride, String tags) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "\u8bf7\u9009\u62e9 SVG \u6587\u4ef6");
        }
        String fn = Optional.ofNullable(file.getOriginalFilename()).orElse("").trim();
        if (!fn.toLowerCase().endsWith(".svg")) {
            throw new BusinessException(400, "\u4ec5\u652f\u6301 .svg \u6587\u4ef6");
        }
        String baseName = fn.substring(0, fn.length() - 4);
        String name = nameOverride != null && !nameOverride.isBlank() ? nameOverride.trim() : baseName;
        this.validateName(name);
        String svg = new String(file.getBytes(), StandardCharsets.UTF_8);
        this.validateSvg(svg);
        SysMediaIcon body = new SysMediaIcon();
        body.setName(name);
        body.setSvgContent(svg);
        body.setTags(tags);
        body.setFontSymbol(name);
        return this.create(body);
    }

    @Generated
    public SysMediaIconService(SysMediaIconRepository repository, ApiPermissionService apiPermissionService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.repository = repository;
        this.apiPermissionService = apiPermissionService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

