package com.dji.sample.wayline.controller;

import com.dji.sample.common.model.CustomClaim;
import com.dji.sample.wayline.service.IWaylineFileService;
import com.dji.sdk.common.HttpResultResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockMultipartFile;

import static com.dji.sample.component.AuthInterceptor.TOKEN_CLAIM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaylineFileControllerTest {

    @Mock
    private IWaylineFileService waylineFileService;

    @InjectMocks
    private WaylineFileController controller;

    @Test
    void overwriteWaylineUsesAuthenticatedWorkspaceAndKeepsId() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(TOKEN_CLAIM, new CustomClaim("user-1", "editor", 1, "workspace-1"));
        MockMultipartFile file = new MockMultipartFile("file", "route.kmz", "application/vnd.google-earth.kmz", new byte[]{1});
        when(waylineFileService.overwriteKmzFile(file, "workspace-1", "wayline-123", "editor"))
                .thenReturn("wayline-123");

        HttpResultResponse<String> response = controller.overwriteWayline(
                request, "workspace-1", "wayline-123", file);

        assertEquals(0, response.getCode());
        assertEquals("wayline-123", response.getData());
        verify(waylineFileService).overwriteKmzFile(file, "workspace-1", "wayline-123", "editor");
    }
}
