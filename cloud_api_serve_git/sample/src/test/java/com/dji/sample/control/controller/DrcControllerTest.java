package com.dji.sample.control.controller;

import com.dji.sample.control.model.dto.JwtAclDTO;
import com.dji.sample.control.model.param.DrcModeParam;
import com.dji.sample.control.service.IDrcService;
import com.dji.sdk.common.HttpResultResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrcControllerTest {

    @Mock
    private IDrcService drcService;

    @InjectMocks
    private DrcController controller;

    @Test
    void drcEnterOnlyStartsDrcWithoutRequestingFlightControl() {
        DrcModeParam param = DrcModeParam.builder()
                .clientId("client-1")
                .dockSn("dock-1")
                .build();
        JwtAclDTO acl = JwtAclDTO.builder()
                .sub(List.of("thing/product/dock-1/drc/up"))
                .pub(List.of("thing/product/dock-1/drc/down"))
                .build();
        when(drcService.deviceDrcEnterOnly("workspace-1", param)).thenReturn(acl);

        HttpResultResponse response = controller.drcEnterOnly("workspace-1", param);

        assertEquals(HttpResultResponse.CODE_SUCCESS, response.getCode());
        assertEquals(acl, response.getData());
        verify(drcService).deviceDrcEnterOnly("workspace-1", param);
    }

}
