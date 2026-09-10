/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.common.PageResult
 *  com.bsball.common.PaginationSupport
 *  com.bsball.config.UploadConfig
 *  com.bsball.core.CurrentUserHolder
 *  com.bsball.exception.BusinessException
 *  com.bsball.model.entity.SysResource
 *  com.bsball.repository.SysResourceRepository
 *  com.bsball.service.ApiPermissionService
 *  com.bsball.service.SysResourceService
 *  com.bsball.service.TenantQueryPolicyService
 *  jakarta.persistence.EntityManager
 *  jakarta.persistence.criteria.Expression
 *  jakarta.persistence.criteria.Predicate
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.data.domain.Page
 *  org.springframework.data.domain.PageRequest
 *  org.springframework.data.domain.Pageable
 *  org.springframework.data.domain.Sort
 *  org.springframework.data.domain.Sort$Direction
 *  org.springframework.data.jpa.domain.Specification
 *  org.springframework.stereotype.Service
 *  org.springframework.web.multipart.MultipartFile
 */
package com.bsball.service;

import com.bsball.common.PageResult;
import com.bsball.common.PaginationSupport;
import com.bsball.config.UploadConfig;
import com.bsball.core.CurrentUserHolder;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysResource;
import com.bsball.repository.SysResourceRepository;
import com.bsball.service.ApiPermissionService;
import com.bsball.service.TenantQueryPolicyService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/*
 * Exception performing whole class analysis ignored.
 */
@Service
public class SysResourceService {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(SysResourceService.class);
    private static final String FILES_PREFIX = "/files/";
    private final SysResourceRepository sysResourceRepository;
    private final EntityManager entityManager;
    private final Path uploadRootPath;
    private final Set<String> allowedImageExtensions;
    private final UploadConfig uploadConfig;
    private final ApiPermissionService apiPermissionService;
    private final TenantQueryPolicyService tenantQueryPolicyService;

    private void normalizeUrlForResponse(List<SysResource> list) {
        if (list == null) {
            return;
        }
        for (SysResource r : list) {
            this.entityManager.detach((Object)r);
            if (r.getUrl() != null && !r.getUrl().isBlank() || r.getPath() == null || r.getPath().isBlank()) continue;
            r.setUrl("/files/" + r.getPath());
        }
    }

    public PageResult<SysResource> list(Integer page, Integer pageSize, String keyword) {
        Page result;
        boolean hasKeyword;
        Pageable p = this.buildPageable(page, pageSize);
        Long tid = this.tenantQueryPolicyService.tenantIdOrNullForQuery();
        boolean bl = hasKeyword = keyword != null && !keyword.isBlank();
        if (tid == null && !hasKeyword) {
            result = this.sysResourceRepository.findAll(p);
        } else {
            Specification & Serializable spec = (Specification & Serializable)(root, q, cb) -> {
                ArrayList predicates = new ArrayList();
                if (tid != null) {
                    predicates.add(cb.equal((Expression)root.get("tenantId"), (Object)tid));
                }
                if (hasKeyword) {
                    String k = "%" + keyword.toLowerCase() + "%";
                    predicates.add(cb.or((Expression)cb.like(cb.lower((Expression)root.get("name")), k), (Expression)cb.like(cb.lower((Expression)root.get("path")), k)));
                }
                return cb.and((Predicate[])predicates.toArray(Predicate[]::new));
            };
            result = this.sysResourceRepository.findAll((Specification)spec, p);
        }
        this.normalizeUrlForResponse(result.getContent());
        return PageResult.of((List)result.getContent(), (long)result.getTotalElements());
    }

    public SysResource create(SysResource entity) {
        entity.setTenantId(Long.valueOf(this.tenantQueryPolicyService.requiredTenantId()));
        return (SysResource)this.sysResourceRepository.save((Object)entity);
    }

    public SysResource upload(MultipartFile file) throws IOException {
        String safeName;
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("\u8bf7\u9009\u62e9\u8981\u4e0a\u4f20\u7684\u6587\u4ef6");
        }
        String originalFilename = Optional.ofNullable(file.getOriginalFilename()).orElse("").trim();
        if (originalFilename.isEmpty()) {
            throw new IllegalArgumentException("\u6587\u4ef6\u540d\u65e0\u6548");
        }
        String ext = SysResourceService.getExtension((String)originalFilename);
        if (ext == null || !this.allowedImageExtensions.contains(ext)) {
            throw new IllegalArgumentException("\u4ec5\u652f\u6301\u56fe\u7247\u683c\u5f0f: " + String.join((CharSequence)", ", this.allowedImageExtensions));
        }
        if (file.getSize() > this.uploadConfig.getMaxFileSize()) {
            throw new IllegalArgumentException("\u6587\u4ef6\u5927\u5c0f\u8d85\u8fc7\u9650\u5236\uff08\u6700\u5927 " + this.uploadConfig.getMaxFileSize() / 1024L / 1024L + "MB\uff09");
        }
        Path root = this.uploadRootPath;
        String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
        String relativePath = dateDir + "/" + (safeName = UUID.randomUUID().toString().replace("-", "") + "." + ext);
        Path targetFile = root.resolve(relativePath).normalize();
        if (!targetFile.startsWith(root)) {
            throw new IllegalArgumentException("\u8def\u5f84\u975e\u6cd5");
        }
        Files.createDirectories(targetFile.getParent(), new FileAttribute[0]);
        file.transferTo(targetFile.toFile());
        SysResource resource = new SysResource();
        resource.setTenantId(Long.valueOf(this.tenantQueryPolicyService.requiredTenantId()));
        resource.setName(originalFilename);
        resource.setPath(relativePath);
        resource.setUrl("/files/" + relativePath);
        resource.setSize(Long.valueOf(file.getSize()));
        resource.setMime(Optional.ofNullable(file.getContentType()).orElse(""));
        return (SysResource)this.sysResourceRepository.save((Object)resource);
    }

    public void delete(Long id) {
        Path root;
        Path filePath;
        Long opId = CurrentUserHolder.get();
        boolean superUser = opId != null && this.apiPermissionService.isSuperAdmin(opId);
        long tid = this.tenantQueryPolicyService.requiredTenantId();
        SysResource resource = this.sysResourceRepository.findById((Object)id).orElse(null);
        if (resource == null) {
            return;
        }
        if (!(superUser || resource.getTenantId() != null && Long.valueOf(tid).equals(resource.getTenantId()))) {
            throw new BusinessException(403, "\u65e0\u6743\u5220\u9664\u8be5\u8d44\u6e90");
        }
        if (resource.getPath() != null && !resource.getPath().isBlank() && (filePath = (root = this.uploadRootPath).resolve(resource.getPath().replace("..", "")).normalize()).startsWith(root)) {
            try {
                Files.deleteIfExists(filePath);
            }
            catch (IOException iOException) {
                // empty catch block
            }
        }
        this.sysResourceRepository.deleteById((Object)id);
    }

    private static String getExtension(String filename) {
        int i = filename.lastIndexOf(46);
        return i > 0 && i < filename.length() - 1 ? filename.substring(i + 1).toLowerCase() : null;
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int p = page != null && page > 0 ? page : 1;
        int ps = PaginationSupport.resolvePageSize((Integer)pageSize);
        return PageRequest.of((int)(p - 1), (int)ps, (Sort)Sort.by((Sort.Direction)Sort.Direction.DESC, (String[])new String[]{"id"}));
    }

    @Generated
    public SysResourceService(SysResourceRepository sysResourceRepository, EntityManager entityManager, Path uploadRootPath, Set<String> allowedImageExtensions, UploadConfig uploadConfig, ApiPermissionService apiPermissionService, TenantQueryPolicyService tenantQueryPolicyService) {
        this.sysResourceRepository = sysResourceRepository;
        this.entityManager = entityManager;
        this.uploadRootPath = uploadRootPath;
        this.allowedImageExtensions = allowedImageExtensions;
        this.uploadConfig = uploadConfig;
        this.apiPermissionService = apiPermissionService;
        this.tenantQueryPolicyService = tenantQueryPolicyService;
    }
}

