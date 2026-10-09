/*
 * 短信发送失败可感知修复（生产事故 2026-10-09）：sendCode 真实发送结果口径测试。
 *
 * 背景：生产 AK 被阿里云「限制性保护」后，所有真实发送均被 Forbidden 拒绝，
 * 但接口仍返回成功、前端提示「已发送」——用户永远收不到验证码。
 *
 * 本测试锁定修复后的口径：
 *  - MOCK（enabled=false）→ 保持成功，不调用阿里云（开发/未启用场景不变）；
 *  - enabled=true 且阿里云拒绝 → 抛 BusinessException(400)「短信发送失败，请稍后重试」；
 *  - enabled=true 且模板缺失（配置故障）→ 抛 BusinessException(400)，不调用阿里云；
 *  - enabled=true 且发送成功 → 正常返回；短信参数 code 与库中记录一致；
 *  - 失败仍占用 60 秒冷却（紧跟重试 429）；AK 未配置（ak-missing）也显式失败。
 */
package com.bsball.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.bsball.config.SmsProperties;
import com.bsball.exception.BusinessException;
import com.bsball.model.entity.SysSmsCode;
import com.bsball.repository.SysSmsCodeRepository;
import com.bsball.service.sms.AliyunSmsClient;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsService：真实发送失败必须显式失败（不再假成功）")
class SmsServiceSendResultTest {

    private static final String PHONE = "13800138000";
    private static final String SCENE = "register";
    private static final String TEMPLATE = "SMS_TEST_REG";

    @Mock
    private SysSmsCodeRepository repository;

    @Mock
    private AliyunSmsClient aliyunSmsClient;

    private SmsProperties props;

    @BeforeEach
    void setUp() {
        props = new SmsProperties();
        props.setCodeLength(6);
        props.setCodeExpireSec(300);
        props.setSendIntervalSec(60);
        props.setPhoneDailyLimit(10);
        props.setIpHourlyLimit(20);
        props.setIpDailyLimit(50);
        // 防刷查询默认放行
        when(repository.countRecentByPhoneAndScene(anyString(), anyString(), any())).thenReturn(0L);
        when(repository.countRecentByIp(anyString(), any())).thenReturn(0L);
        when(repository.findRecentByPhoneAndScene(anyString(), anyString(), any())).thenReturn(List.of());
        when(repository.save(any(SysSmsCode.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private SmsService newService() {
        SmsService service = new SmsService(props, repository, aliyunSmsClient);
        service.init();
        return service;
    }

    @Test
    @DisplayName("MOCK 模式（enabled=false）：保持成功且不调用阿里云")
    void mockModeStaysSuccessful() {
        props.setEnabled(false);
        props.setTemplateCodeRegister(TEMPLATE);
        SmsService service = newService();

        assertDoesNotThrow(() -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));
        verifyNoInteractions(aliyunSmsClient);
    }

    @Test
    @DisplayName("已启用 + 阿里云拒绝：抛 400「短信发送失败」")
    void aliyunRejectionThrows() {
        props.setEnabled(true);
        props.setAccessKeyId("test-ak");
        props.setAccessKeySecret("test-sk");
        props.setTemplateCodeRegister(TEMPLATE);
        when(aliyunSmsClient.send(eq(PHONE), eq(TEMPLATE), anyMap())).thenReturn(false);
        SmsService service = newService();

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("短信发送失败"),
            "错误文案应直达用户，实际：" + ex.getMessage());
        verify(aliyunSmsClient).send(eq(PHONE), eq(TEMPLATE), anyMap());
    }

    @Test
    @DisplayName("已启用 + 模板缺失（配置故障）：抛 400 且不调用阿里云")
    void missingTemplateThrows() {
        props.setEnabled(true);
        props.setAccessKeyId("test-ak");
        props.setAccessKeySecret("test-sk");
        // 三个模板均为空（resolveTemplate 返回空注册模板）
        SmsService service = newService();

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("短信发送失败"));
        verifyNoInteractions(aliyunSmsClient);
    }

    @Test
    @DisplayName("已启用 + 发送成功：正常返回，短信参数 code 与库中记录一致")
    void aliyunAcceptedKeepsSuccessAndCodeConsistency() {
        props.setEnabled(true);
        props.setAccessKeyId("test-ak");
        props.setAccessKeySecret("test-sk");
        props.setTemplateCodeRegister(TEMPLATE);
        when(aliyunSmsClient.send(eq(PHONE), eq(TEMPLATE), anyMap())).thenReturn(true);
        SmsService service = newService();

        assertDoesNotThrow(() -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));

        ArgumentCaptor<SysSmsCode> recordCaptor = ArgumentCaptor.forClass(SysSmsCode.class);
        verify(repository).save(recordCaptor.capture());
        ArgumentCaptor<Map<String, String>> paramCaptor = ArgumentCaptor.forClass(Map.class);
        verify(aliyunSmsClient).send(eq(PHONE), eq(TEMPLATE), paramCaptor.capture());

        String recordCode = recordCaptor.getValue().getCode();
        assertEquals(6, recordCode.length());
        assertEquals(recordCode, paramCaptor.getValue().get("code"));
        assertEquals("5", paramCaptor.getValue().get("time"));
    }

    @Test
    @DisplayName("失败仍占用 60 秒冷却：紧跟重试返回 429")
    void failureStillOccupiesCooldown() {
        props.setEnabled(true);
        props.setAccessKeyId("test-ak");
        props.setAccessKeySecret("test-sk");
        props.setTemplateCodeRegister(TEMPLATE);
        when(aliyunSmsClient.send(eq(PHONE), eq(TEMPLATE), anyMap())).thenReturn(false);
        SmsService service = newService();

        BusinessException first = assertThrows(BusinessException.class,
            () -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));
        assertEquals(400, first.getCode());

        // 契约：失败也写入冷却（防刷不变量不变），紧跟重试应为 429 而非再次发起发送
        BusinessException second = assertThrows(BusinessException.class,
            () -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));
        assertEquals(429, second.getCode());
    }

    @Test
    @DisplayName("已启用 + AK 未配置（ak-missing）：仍抛 400 显式失败")
    void missingAccessKeyThrows() {
        props.setEnabled(true);
        props.setTemplateCodeRegister(TEMPLATE);
        // accessKeyId / accessKeySecret 保持为空：与 AliyunSmsClient 空 AK 直接返回 false 的行为一致
        when(aliyunSmsClient.send(eq(PHONE), eq(TEMPLATE), anyMap())).thenReturn(false);
        SmsService service = newService();

        BusinessException ex = assertThrows(BusinessException.class,
            () -> service.sendCode(PHONE, SCENE, 1L, "10.0.0.1"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("短信发送失败"));
    }
}
