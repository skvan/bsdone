package com.bsball.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.bsball.common.Result;
import com.bsball.exception.BusinessException;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("BusinessException 带 reason 时，错误体 data.reason 透出稳定原因码")
    void handleBusinessExposesReason() {
        ResponseEntity<Result<Object>> response = handler.handleBusiness(
                new BusinessException(409, "快照变短", "snapshot_event_count_shrink_without_intent"));

        assertEquals(HttpStatus.OK, response.getStatusCode(), "409 沿用既有约定走 200 包装");
        assertEquals(409, response.getBody().getCode());
        assertEquals(Map.of("reason", "snapshot_event_count_shrink_without_intent"),
                response.getBody().getData());
    }

    @Test
    @DisplayName("BusinessException 没有 reason 时错误体保持原样")
    void handleBusinessWithoutReasonKeepsOldShape() {
        ResponseEntity<Result<Object>> response = handler.handleBusiness(
                new BusinessException(400, "参数错误"));

        assertEquals(400, response.getBody().getCode());
        assertNull(response.getBody().getData());
    }
}
