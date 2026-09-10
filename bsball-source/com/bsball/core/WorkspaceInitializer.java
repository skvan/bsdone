/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.core.WorkspaceInitializer
 *  org.springframework.context.ApplicationContextInitializer
 *  org.springframework.core.env.ConfigurableEnvironment
 *  org.springframework.web.context.ConfigurableWebApplicationContext
 */
package com.bsball.core;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.web.context.ConfigurableWebApplicationContext;

public class WorkspaceInitializer
implements ApplicationContextInitializer<ConfigurableWebApplicationContext> {
    public void initialize(ConfigurableWebApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();
        String workspace = env.getProperty("app.workspace", ".");
        String uploadDir = env.getProperty("app.upload.dir", "uploads");
        Path base = Paths.get(workspace, new String[0]).toAbsolutePath().normalize();
        for (String dir : new String[]{"data", "logs", uploadDir}) {
            File f = base.resolve(dir).toFile();
            if (f.exists() || !f.mkdirs()) continue;
            System.out.println("[bs-ball] \u5df2\u521b\u5efa\u76ee\u5f55: " + f.getAbsolutePath());
        }
    }
}

