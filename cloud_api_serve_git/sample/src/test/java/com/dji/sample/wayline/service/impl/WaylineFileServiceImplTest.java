package com.dji.sample.wayline.service.impl;

import com.dji.sample.component.oss.model.OssConfiguration;
import com.dji.sample.component.oss.service.impl.OssServiceContext;
import com.dji.sample.wayline.dao.IWaylineFileMapper;
import com.dji.sample.wayline.model.entity.WaylineFileEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaylineFileServiceImplTest {

    @Mock
    private IWaylineFileMapper mapper;

    @Mock
    private OssServiceContext ossService;

    @InjectMocks
    private WaylineFileServiceImpl service;

    @BeforeEach
    void setUp() {
        OssConfiguration.bucket = "wayline-bucket";
        OssConfiguration.objectDirPrefix = "wayline";
    }

    @Test
    void overwriteKmzFileUpdatesExistingWaylineIdWithoutReinserting() throws Exception {
        WaylineFileEntity existing = WaylineFileEntity.builder()
                .id(12)
                .waylineId("wayline-123")
                .workspaceId("workspace-1")
                .name("old-route")
                .objectKey("wayline/old-route.kmz")
                .sign("old-sign")
                .favorited(true)
                .waylinePos(7)
                .createTime(100L)
                .build();
        when(mapper.selectOne(any())).thenReturn(existing);
        when(mapper.updateById(any())).thenReturn(1);
        when(ossService.deleteObject(OssConfiguration.bucket, existing.getObjectKey())).thenReturn(true);

        MockMultipartFile replacement = kmzFile("edited-route.kmz");
        when(ossService.getObject(eq(OssConfiguration.bucket), any()))
                .thenReturn(new ByteArrayInputStream(replacement.getBytes()));

        String returnedId = service.overwriteKmzFile(
                replacement, "workspace-1", "wayline-123", "editor");

        ArgumentCaptor<WaylineFileEntity> updateCaptor = ArgumentCaptor.forClass(WaylineFileEntity.class);
        verify(mapper).updateById(updateCaptor.capture());
        WaylineFileEntity updated = updateCaptor.getValue();

        assertEquals("wayline-123", returnedId);
        assertEquals("wayline-123", updated.getWaylineId());
        assertEquals(12, updated.getId());
        assertEquals("workspace-1", updated.getWorkspaceId());
        assertEquals(100L, updated.getCreateTime());
        assertEquals(true, updated.getFavorited());
        assertEquals(7, updated.getWaylinePos());
        assertEquals("edited-route", updated.getName());
        assertEquals("editor", updated.getUsername());
        assertNotEquals("wayline/old-route.kmz", updated.getObjectKey());
        assertTrue(updated.getObjectKey().startsWith("wayline/workspace-1/wayline-123/"));
        verify(mapper, never()).insert(any());
        verify(mapper, never()).delete(any());
        verify(ossService).putObject(eq(OssConfiguration.bucket), eq(updated.getObjectKey()), any());
        verify(ossService).deleteObject(OssConfiguration.bucket, "wayline/old-route.kmz");
    }

    @Test
    void overwriteKmzFileKeepsExistingRowAndObjectWhenUploadCannotBeVerified() throws Exception {
        WaylineFileEntity existing = WaylineFileEntity.builder()
                .id(12)
                .waylineId("wayline-123")
                .workspaceId("workspace-1")
                .objectKey("wayline/old-route.kmz")
                .build();
        when(mapper.selectOne(any())).thenReturn(existing);
        when(ossService.getObject(eq(OssConfiguration.bucket), any()))
                .thenReturn(new ByteArrayInputStream(new byte[0]));

        MockMultipartFile replacement = kmzFile("edited-route.kmz");
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () ->
                service.overwriteKmzFile(replacement, "workspace-1", "wayline-123", "editor"));

        verify(mapper, never()).updateById(any());
        verify(ossService, never()).deleteObject(OssConfiguration.bucket, "wayline/old-route.kmz");
        verify(ossService).deleteObject(eq(OssConfiguration.bucket), org.mockito.ArgumentMatchers.contains("wayline-123"));
    }

    private MockMultipartFile kmzFile(String filename) throws Exception {
        String template = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:wpml=\"http://www.dji.com/wpmz/1.0.6\">"
                + "<Document><wpml:missionConfig>"
                + "<wpml:droneInfo><wpml:droneEnumValue>77</wpml:droneEnumValue>"
                + "<wpml:droneSubEnumValue>2</wpml:droneSubEnumValue></wpml:droneInfo>"
                + "<wpml:payloadInfo><wpml:payloadEnumValue>66</wpml:payloadEnumValue>"
                + "<wpml:payloadSubEnumValue>0</wpml:payloadSubEnumValue></wpml:payloadInfo>"
                + "</wpml:missionConfig><Folder><wpml:templateType>waypoint</wpml:templateType></Folder>"
                + "</Document></kml>";
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("wpmz/template.kml"));
            zip.write(template.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return new MockMultipartFile("file", filename, "application/vnd.google-earth.kmz", bytes.toByteArray());
    }
}
