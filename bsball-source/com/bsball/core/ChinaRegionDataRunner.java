/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.config.InitSeedProperties
 *  com.bsball.core.ChinaRegionDataRunner
 *  com.bsball.model.entity.ChinaRegion
 *  com.bsball.repository.ChinaRegionRepository
 *  com.bsball.utils.ChinaAdcodeUtils
 *  com.fasterxml.jackson.databind.JsonNode
 *  com.fasterxml.jackson.databind.ObjectMapper
 *  jakarta.persistence.EntityManager
 *  lombok.Generated
 *  org.slf4j.Logger
 *  org.slf4j.LoggerFactory
 *  org.springframework.boot.CommandLineRunner
 *  org.springframework.core.annotation.Order
 *  org.springframework.core.io.ClassPathResource
 *  org.springframework.stereotype.Component
 *  org.springframework.transaction.annotation.Transactional
 */
package com.bsball.core;

import com.bsball.config.InitSeedProperties;
import com.bsball.model.entity.ChinaRegion;
import com.bsball.repository.ChinaRegionRepository;
import com.bsball.utils.ChinaAdcodeUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Generated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/*
 * Exception performing whole class analysis ignored.
 */
@Component
@Order(value=20)
public class ChinaRegionDataRunner
implements CommandLineRunner {
    @Generated
    private static final Logger log = LoggerFactory.getLogger(ChinaRegionDataRunner.class);
    private static final String DATA_RESOURCE = "data/china-pca-code.json";
    private static final int BATCH = 400;
    private final ChinaRegionRepository chinaRegionRepository;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;
    private final InitSeedProperties initSeedProperties;

    @Transactional
    public void run(String ... args) {
        if (!this.initSeedProperties.isImportChinaRegion()) {
            log.debug("app.init.import-china-region=false\uff0c\u8df3\u8fc7\u4e2d\u56fd\u884c\u653f\u533a\u5212\u8868\u521d\u59cb\u5316");
            return;
        }
        if (this.chinaRegionRepository.count() > 0L) {
            return;
        }
        ClassPathResource res = new ClassPathResource("data/china-pca-code.json");
        if (!res.exists()) {
            log.warn("\u672a\u627e\u5230 {}\uff0c\u8df3\u8fc7\u4e2d\u56fd\u884c\u653f\u533a\u5212\u521d\u59cb\u5316", (Object)"data/china-pca-code.json");
            return;
        }
        LinkedHashMap byAdcode = new LinkedHashMap();
        try (InputStream in = res.getInputStream();){
            JsonNode root = this.objectMapper.readTree(in);
            if (!root.isArray()) {
                log.warn("{} \u683c\u5f0f\u5f02\u5e38\uff1a\u6839\u8282\u70b9\u9700\u4e3a\u6570\u7ec4", (Object)"data/china-pca-code.json");
                return;
            }
            for (JsonNode prov : root) {
                this.flattenProvince(prov, byAdcode);
            }
            ArrayList all = new ArrayList(byAdcode.values());
            for (int i = 0; i < all.size(); i += 400) {
                int to = Math.min(i + 400, all.size());
                this.chinaRegionRepository.saveAll(all.subList(i, to));
                this.entityManager.flush();
                this.entityManager.clear();
            }
            log.info("\u4e2d\u56fd\u884c\u653f\u533a\u5212\u5df2\u5bfc\u5165\uff0c\u5171 {} \u6761\uff08\u6765\u6e90: {}\uff09", (Object)this.chinaRegionRepository.count(), (Object)"data/china-pca-code.json");
        }
        catch (Exception e) {
            log.error("\u5bfc\u5165\u4e2d\u56fd\u884c\u653f\u533a\u5212\u5931\u8d25: {}", (Object)e.getMessage());
        }
    }

    private void flattenProvince(JsonNode prov, Map<String, ChinaRegion> byAdcode) {
        String pNorm = ChinaAdcodeUtils.normalize((String)prov.get("code").asText());
        String pName = prov.get("name").asText();
        ChinaRegionDataRunner.put(byAdcode, (ChinaRegion)ChinaRegionDataRunner.build((String)pNorm, (String)pName, (int)1, null));
        JsonNode cities = prov.get("children");
        if (cities == null || !cities.isArray()) {
            return;
        }
        for (JsonNode city : cities) {
            String cNorm = ChinaAdcodeUtils.normalize((String)city.get("code").asText());
            String cName = city.get("name").asText();
            ChinaRegionDataRunner.put(byAdcode, (ChinaRegion)ChinaRegionDataRunner.build((String)cNorm, (String)cName, (int)2, (String)pNorm));
            JsonNode dists = city.get("children");
            if (dists == null || !dists.isArray()) continue;
            for (JsonNode d : dists) {
                String dNorm = ChinaAdcodeUtils.normalize((String)d.get("code").asText());
                String dName = d.get("name").asText();
                ChinaRegionDataRunner.put(byAdcode, (ChinaRegion)ChinaRegionDataRunner.build((String)dNorm, (String)dName, (int)3, (String)cNorm));
            }
        }
    }

    private static void put(Map<String, ChinaRegion> byAdcode, ChinaRegion r) {
        ChinaRegion prev = byAdcode.putIfAbsent(r.getAdcode(), r);
        if (prev != null) {
            log.debug("\u884c\u653f\u533a\u5212 adcode \u91cd\u590d\uff0c\u4fdd\u7559\u9996\u6761: {} ({})", (Object)r.getAdcode(), (Object)r.getName());
        }
    }

    private static ChinaRegion build(String adcode, String name, int level, String parentAdcode) {
        ChinaRegion row = new ChinaRegion();
        row.setAdcode(adcode);
        row.setName(name);
        row.setLevel((short)level);
        row.setParentAdcode(parentAdcode);
        return row;
    }

    @Generated
    public ChinaRegionDataRunner(ChinaRegionRepository chinaRegionRepository, ObjectMapper objectMapper, EntityManager entityManager, InitSeedProperties initSeedProperties) {
        this.chinaRegionRepository = chinaRegionRepository;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
        this.initSeedProperties = initSeedProperties;
    }
}

