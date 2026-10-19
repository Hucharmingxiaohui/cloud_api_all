package com.dji.sample.df.solarDf.service;

import com.dji.sample.df.solarDf.dao.*;
import com.dji.sample.df.solarDf.model.entity.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SolarAccessoryServiceTest {
    private final SolarAccessoryMapper devices = mock(SolarAccessoryMapper.class);
    private final SolarAccessoryWaypointMapper waypoints = mock(SolarAccessoryWaypointMapper.class);
    private final OrthophotoEntityMapper photos = mock(OrthophotoEntityMapper.class);
    private final SolarAccessoryService service = new SolarAccessoryService(devices, waypoints, photos);

    private SolarAccessory device() {
        SolarAccessory device = new SolarAccessory();
        device.setOrthophotoId("photo-1");
        device.setName("逆变器-01");
        device.setDeviceType("inverter");
        device.setLongitude(120.0);
        device.setLatitude(36.0);
        device.setHeight(50.0);
        device.setEnabled(true);
        SolarAccessoryWaypoint waypoint = new SolarAccessoryWaypoint();
        waypoint.setName("正面");
        waypoint.setLongitude(120.001);
        waypoint.setLatitude(36.001);
        waypoint.setHeight(60.0);
        waypoint.setHeading(180.0);
        waypoint.setPitch(-30.0);
        waypoint.setFocalLength(24.0);
        waypoint.setAction("take_photo");
        waypoint.setImageFormat("visible");
        waypoint.setEnabled(true);
        device.setWaypoints(Collections.singletonList(waypoint));
        return device;
    }

    @Test
    void savesDeviceWithOrderedOwnedWaypoints() {
        when(photos.selectById("photo-1")).thenReturn(new OrthophotoEntity());
        when(devices.insert(any())).thenReturn(1);
        when(waypoints.insert(any())).thenReturn(1);
        SolarAccessory saved = service.save(device());
        assertNotNull(saved.getId());
        ArgumentCaptor<SolarAccessoryWaypoint> capture = ArgumentCaptor.forClass(SolarAccessoryWaypoint.class);
        verify(waypoints).insert(capture.capture());
        assertEquals(saved.getId(), capture.getValue().getDeviceId());
        assertEquals(1, capture.getValue().getSortOrder());
        assertNotNull(capture.getValue().getId());
    }

    @Test
    void rejectsInvalidCameraAndCoordinatesBeforeWriting() {
        for (Double value : Arrays.asList(null, Double.NaN, Double.POSITIVE_INFINITY, 181.0)) {
            SolarAccessory device = device();
            device.setLongitude(value);
            assertThrows(IllegalArgumentException.class, () -> service.save(device));
        }
        SolarAccessory device = device();
        device.getWaypoints().get(0).setFocalLength(0.0);
        assertThrows(IllegalArgumentException.class, () -> service.save(device));
        verifyNoInteractions(devices, waypoints);
    }

    @Test
    void cannotMoveExistingDeviceToAnotherOrthophoto() {
        SolarAccessory device = device();
        device.setId("existing");
        SolarAccessory existing = device();
        existing.setOrthophotoId("another-photo");
        when(photos.selectById("photo-1")).thenReturn(new OrthophotoEntity());
        when(devices.selectById("existing")).thenReturn(existing);
        assertThrows(IllegalArgumentException.class, () -> service.save(device));
        verify(devices, never()).updateById(any());
        verifyNoInteractions(waypoints);
    }

    @Test
    void failedChildInsertFailsAggregateSave() {
        when(photos.selectById("photo-1")).thenReturn(new OrthophotoEntity());
        when(devices.insert(any())).thenReturn(1);
        when(waypoints.insert(any())).thenReturn(0);
        assertThrows(IllegalStateException.class, () -> service.save(device()));
    }

    @Test
    void removesChildrenWhenOrthophotoIsDeleted() {
        SolarAccessory existing = device();
        existing.setId("existing");
        when(devices.selectList(any())).thenReturn(Collections.singletonList(existing));
        service.deleteByOrthophoto("photo-1");
        verify(waypoints).delete(any());
        verify(devices).delete(any());
    }

    @Test
    void updateReplacesChildrenAndIgnoresClientSuppliedOwnership() {
        SolarAccessory existing = device();
        existing.setId("existing");
        existing.getWaypoints().get(0).setDeviceId("other-device");
        when(photos.selectById("photo-1")).thenReturn(new OrthophotoEntity());
        when(devices.selectById("existing")).thenReturn(device());
        when(devices.updateById(any())).thenReturn(1);
        when(waypoints.insert(any())).thenReturn(1);
        SolarAccessory saved = service.save(existing);
        verify(waypoints).delete(any());
        assertEquals("existing", saved.getWaypoints().get(0).getDeviceId());
    }

    @Test
    void bindsSnakeCaseParameters() throws Exception {
        ObjectMapper mapper = new ObjectMapper().setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        SolarAccessory device = mapper.readValue("{\"orthophoto_id\":\"photo-1\",\"device_type\":\"inverter\",\"waypoints\":[{\"focal_length\":24,\"sort_order\":1}]}", SolarAccessory.class);
        assertEquals("photo-1", device.getOrthophotoId());
        assertEquals(24.0, device.getWaypoints().get(0).getFocalLength());
        assertEquals(1, device.getWaypoints().get(0).getSortOrder());
    }
}
