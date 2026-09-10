/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.bsball.model.enums.LbsProvider
 *  com.bsball.service.iplocation.IpLocationDetail
 *  com.bsball.service.iplocation.IpLocationProvider
 */
package com.bsball.service.iplocation;

import com.bsball.model.enums.LbsProvider;
import com.bsball.service.iplocation.IpLocationDetail;
import java.util.Optional;

public interface IpLocationProvider {
    public boolean isConfigured();

    public Optional<IpLocationDetail> locate(String var1);

    public LbsProvider lbsProvider();
}

