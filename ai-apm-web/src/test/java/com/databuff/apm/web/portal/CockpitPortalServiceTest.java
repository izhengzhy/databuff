package com.databuff.apm.web.portal;

import com.databuff.apm.common.query.ApmQueryModels.TrafficLightPoint;
import com.databuff.apm.common.time.ApmTimeZones;
import com.databuff.apm.common.util.PortalServiceIdResolver;
import com.databuff.apm.web.TestStorageSupport;
import com.databuff.apm.web.cockpit.TrafficLightService;
import com.databuff.apm.web.monitor.Alarm;
import com.databuff.apm.web.monitor.AlarmStore;
import com.databuff.apm.web.monitor.EventRule;
import com.databuff.apm.web.monitor.TestMonitorRecordIds;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CockpitPortalServiceTest {

    private TrafficLightService trafficLightService;
    private ServicePortalService servicePortalService;
    private AlarmStore alarmStore;
    private CockpitPortalService cockpitPortalService;

    @BeforeEach
    void setUp() {
        trafficLightService = new TrafficLightService(mock(), TestStorageSupport.storage());
        servicePortalService = Mockito.mock(ServicePortalService.class);
        alarmStore = new AlarmStore(TestMonitorRecordIds.create());
        cockpitPortalService = new CockpitPortalService(
                trafficLightService, servicePortalService, alarmStore);
    }

    @Test
    void servicesHealthReturnsGroupedServiceOrders() {
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", "checkout", "name", "checkout", "service", "checkout")));

        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of("exceptionRed", 2, "exceptionYellow", 1));
        when(spyTrafficLight.trafficLight(anyLong(), anyLong())).thenReturn(List.of(
                new TrafficLightPoint("2026-06-01 12:00:00", "checkout", 3, 10)));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        long ts = ApmTimeZones.wallClockToEpochMilli("2026-06-01 12:00:00");
        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "warning", "alarm",
                Alarm.STATUS_OPEN, Instant.ofEpochMilli(ts + 30_000L), null));

        List<Map<String, Object>> rows = service.servicesHealth(Map.of(
                "fromTime", ts,
                "toTime", ts + 60_000L,
                "orderBy", "exception"));

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("timestamp")).isEqualTo(ts);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) rows.get(0).get("serviceOrders");
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).get("name")).isEqualTo("checkout");
        assertThat(orders.get(0).get("value")).isEqualTo(3L);
        assertThat(orders.get(0).get("trafficLight")).isEqualTo("red");
    }

    @Test
    void countServiceAlarmsUsesDefaultAlarmThresholds() {
        long ts = ApmTimeZones.wallClockToEpochMilli("2026-06-09 14:53:00");
        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "warning", "alarm",
                Alarm.STATUS_OPEN, Instant.ofEpochMilli(ts + 10_000L), null));
        alarmStore.persistExisting(new Alarm(
                "A2", 2L, "payment", EventRule.WAY_THRESHOLD, "warning", "alarm",
                Alarm.STATUS_OPEN, Instant.ofEpochMilli(ts + 20_000L), null));

        Map<String, Object> data = cockpitPortalService.countServiceAlarms(Map.of(
                "fromTime", ts,
                "toTime", ts + 60_000L,
                "interval", 60_000L,
                "orderBy", "alarm"));

        @SuppressWarnings("unchecked")
        Map<String, Object> point = (Map<String, Object>) data.get(String.valueOf(ts));
        assertThat(point.get("value")).isEqualTo(2L);
        assertThat(point.get("trafficLight")).isEqualTo("red");
    }

    @Test
    void countServiceAlarmsColorsExceptionTrendByMinute() {
        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of("exceptionRed", 1, "exceptionYellow", 1));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        long ts = ApmTimeZones.wallClockToEpochMilli("2026-06-09 14:53:00");
        when(spyTrafficLight.trafficLight(ts, ts + 60_000L)).thenReturn(List.of(
                new TrafficLightPoint("2026-06-09 14:53:00", "service-b", 2, 10)));

        Map<String, Object> data = service.countServiceAlarms(Map.of(
                "fromTime", ts,
                "toTime", ts + 60_000L,
                "interval", 60_000L,
                "orderBy", "exception"));

        @SuppressWarnings("unchecked")
        Map<String, Object> point = (Map<String, Object>) data.get(String.valueOf(ts));
        assertThat(point.get("value")).isEqualTo(2L);
        assertThat(point.get("trafficLight")).isEqualTo("red");
    }

    @Test
    void servicesHealthDedupesDuplicateServiceRows() {
        String serviceId = PortalServiceIdResolver.normalize("checkout");
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", serviceId, "name", "checkout", "service", "checkout")));

        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of("exceptionRed", 10, "exceptionYellow", 5, "showServiceNumber", 20));
        when(spyTrafficLight.trafficLight(anyLong(), anyLong())).thenReturn(List.of(
                new TrafficLightPoint("2026-06-01 12:00:00", "checkout", 2, 10),
                new TrafficLightPoint("2026-06-01 12:00:00", serviceId, 3, 8)));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        long ts = ApmTimeZones.wallClockToEpochMilli("2026-06-01 12:00:00");
        List<Map<String, Object>> rows = service.servicesHealth(Map.of(
                "fromTime", ts,
                "toTime", ts + 60_000L,
                "orderBy", "exception"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> orders = (List<Map<String, Object>>) rows.get(0).get("serviceOrders");
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0).get("value")).isEqualTo(5L);
    }

    @Test
    void servicesHealthCountsOpenAlarmAcrossActiveMinutes() {
        String serviceId = PortalServiceIdResolver.normalize("checkout");
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", serviceId, "name", "checkout", "service", "checkout")));

        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of("red", 1, "yellow", 1, "showServiceNumber", 20));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        long minuteOne = ApmTimeZones.wallClockToEpochMilli("2026-06-09 14:53:00");
        long minuteTwo = minuteOne + 60_000L;
        long minuteThree = minuteTwo + 60_000L;
        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "warning", "alarm",
                Alarm.STATUS_OPEN, Instant.ofEpochMilli(minuteOne), null));

        List<Map<String, Object>> rows = service.servicesHealth(Map.of(
                "fromTime", minuteOne,
                "toTime", minuteThree + 60_000L,
                "orderBy", "alarm"));

        assertThat(rows).hasSize(3);
        for (Map<String, Object> row : rows) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> orders = (List<Map<String, Object>>) row.get("serviceOrders");
            assertThat(orders.get(0).get("value")).isEqualTo(1L);
        }
    }

    @Test
    void countServiceAlarmsTotalCountsYellowRedOccurrencesInDisplayWindow() {
        String checkoutId = PortalServiceIdResolver.normalize("checkout");
        String paymentId = PortalServiceIdResolver.normalize("payment");
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", checkoutId, "name", "checkout", "service", "checkout"),
                Map.of("id", paymentId, "name", "payment", "service", "payment")));

        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of(
                "alarmRed", 2, "alarmYellow", 1,
                "exceptionRed", 10, "exceptionYellow", 2,
                "showServiceNumber", 20));
        long minuteOne = ApmTimeZones.wallClockToEpochMilli("2026-06-09 14:53:00");
        long minuteTwo = minuteOne + 60_000L;
        long minuteThree = minuteTwo + 60_000L;
        long minuteFour = minuteThree + 60_000L;
        long minuteFive = minuteFour + 60_000L;
        when(spyTrafficLight.trafficLight(minuteOne, minuteOne + 60_000L)).thenReturn(List.of(
                new TrafficLightPoint("2026-06-09 14:53:00", "checkout", 2, 10)));
        when(spyTrafficLight.trafficLight(minuteTwo, minuteTwo + 60_000L)).thenReturn(List.of(
                new TrafficLightPoint("2026-06-09 14:54:00", "checkout", 2, 10)));
        when(spyTrafficLight.trafficLight(minuteThree, minuteThree + 60_000L)).thenReturn(List.of(
                new TrafficLightPoint("2026-06-09 14:55:00", "checkout", 2, 10)));
        when(spyTrafficLight.trafficLight(minuteFour, minuteFour + 60_000L)).thenReturn(List.of(
                new TrafficLightPoint("2026-06-09 14:56:00", "checkout", 2, 10)));
        when(spyTrafficLight.trafficLight(minuteFive, minuteFive + 60_000L)).thenReturn(List.of(
                new TrafficLightPoint("2026-06-09 14:57:00", "checkout", 2, 10)));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "warning", "alarm",
                Alarm.STATUS_OPEN, Instant.ofEpochMilli(minuteOne + 10_000L), null));
        alarmStore.persistExisting(new Alarm(
                "A2", 2L, "payment", EventRule.WAY_THRESHOLD, "warning", "alarm",
                Alarm.STATUS_OPEN, Instant.ofEpochMilli(minuteFive + 10_000L), null));

        Map<String, Object> data = service.countServiceAlarmsTotal(Map.of(
                "fromTime", minuteOne,
                "toTime", minuteFive + 60_000L,
                "windowEnd", minuteFive + 60_000L));

        assertThat(data.get("alarmCount")).isEqualTo(6L);
        assertThat(data.get("exceptionCount")).isEqualTo(5L);
    }

    @Test
    void getEntityAlarmListClassifiesServices() {
        String serviceId = PortalServiceIdResolver.normalize("checkout");
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", serviceId, "name", "checkout", "service", "checkout"),
                Map.of("id", "idle-svc", "name", "idle-svc", "service", "idle-svc")));
        when(servicePortalService.listDistinctServices(anyLong(), anyLong()))
                .thenReturn(List.of(serviceId));

        alarmStore.open(serviceId, EventRule.WAY_THRESHOLD, "critical", "critical alarm");

        long now = System.currentTimeMillis();
        Map<String, Object> data = cockpitPortalService.getEntityAlarmList(Map.of(
                "type", "SERVICE",
                "fromTime", now - 3_600_000L,
                "toTime", now + 60_000L));

        assertThat(data.get("total")).isEqualTo(2);
        assertThat(data.get("matterDataCount")).isEqualTo(1);
        assertThat(data.get("minorDataCount")).isEqualTo(0);
        assertThat(data.get("noDataCount")).isEqualTo(1);
        assertThat(data.get("noAlarmCount")).isEqualTo(0);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> entities = (List<Map<String, Object>>) data.get("alarmEntityList");
        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).get("entityName")).isEqualTo("checkout");
        assertThat(entities.get(0).get("matterDataCount")).isEqualTo(1);
        assertThat(entities.get(1).get("noDataCount")).isEqualTo(1);
    }

    @Test
    void getEntityAlarmListReturnsEmptyForUnsupportedType() {
        Map<String, Object> data = cockpitPortalService.getEntityAlarmList(Map.of("type", "BUSINESS"));
        assertThat(data.get("total")).isEqualTo(0);
        assertThat(data.get("alarmEntityList")).isEqualTo(List.of());
    }

    @Test
    void getAlarmCountGroupsByStatusAndLevel() {
        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "warning", "warning alarm",
                Alarm.STATUS_RESOLVED, Instant.now(), Instant.now()));
        alarmStore.persistExisting(new Alarm(
                "A2", 2L, "payment", EventRule.WAY_THRESHOLD, "critical", "critical alarm",
                Alarm.STATUS_OPEN, Instant.now(), null));

        long now = System.currentTimeMillis();
        List<Map<String, Object>> rows = cockpitPortalService.getAlarmCount(Map.of(
                "fromTime", now - 3_600_000L,
                "toTime", now + 60_000L));

        assertThat(rows).hasSize(1);
        Map<String, Object> summary = rows.get(0);
        assertThat(summary.get("matterData")).isEqualTo(1);
        assertThat(summary.get("minorData")).isEqualTo(1);
    }

    @Test
    void entityDataReturnsServiceHealthSummary() {
        when(servicePortalService.listDistinctServices(anyLong(), anyLong()))
                .thenReturn(List.of("checkout"));

        long now = System.currentTimeMillis();
        Map<String, Object> data = cockpitPortalService.entityData(Map.of(
                "type", "SERVICE",
                "fromTime", now - 3_600_000L,
                "toTime", now));

        assertThat(data.get("total")).isEqualTo(1);
        assertThat(data.get("healthRangeScoreList")).isInstanceOf(List.class);
    }

    @Test
    void servicesHealthCountsOneShotAlarmsInTriggeredMinute() {
        String serviceId = PortalServiceIdResolver.normalize("checkout");
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", serviceId, "name", "checkout", "service", "checkout")));

        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of("alarmRed", 1, "alarmYellow", 1, "showServiceNumber", 20));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        long minuteOne = ApmTimeZones.wallClockToEpochMilli("2026-06-09 14:53:00");
        long minuteTwo = minuteOne + 60_000L;
        long minuteThree = minuteTwo + 60_000L;
        // One-shot alarm (resolvedAt == triggeredAt), as produced by AlarmStore.openResolved.
        Instant triggered = Instant.ofEpochMilli(minuteOne + 10_000L);
        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "critical", "alarm",
                Alarm.STATUS_RESOLVED, triggered, triggered));

        List<Map<String, Object>> rows = service.servicesHealth(Map.of(
                "fromTime", minuteOne,
                "toTime", minuteThree + 60_000L,
                "orderBy", "alarm"));

        assertThat(rows).hasSize(3);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> firstOrders = (List<Map<String, Object>>) rows.get(0).get("serviceOrders");
        assertThat(firstOrders).hasSize(1);
        assertThat(firstOrders.get(0).get("name")).isEqualTo("checkout");
        assertThat(firstOrders.get(0).get("value")).isEqualTo(1L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> laterOrders = (List<Map<String, Object>>) rows.get(1).get("serviceOrders");
        assertThat(laterOrders.get(0).get("value")).isEqualTo(0L);
    }

    @Test
    void countServiceAlarmsTotalCountsOneShotAlarmsInDisplayWindow() {
        String serviceId = PortalServiceIdResolver.normalize("checkout");
        when(servicePortalService.basicServices(any())).thenReturn(List.of(
                Map.of("id", serviceId, "name", "checkout", "service", "checkout")));

        TrafficLightService spyTrafficLight = mock(TrafficLightService.class);
        when(spyTrafficLight.getConfig()).thenReturn(Map.of(
                "alarmRed", 1, "alarmYellow", 1, "showServiceNumber", 20));
        CockpitPortalService service = new CockpitPortalService(
                spyTrafficLight, servicePortalService, alarmStore);

        long minuteOne = ApmTimeZones.wallClockToEpochMilli("2026-06-09 14:53:00");
        long minuteTwo = minuteOne + 60_000L;
        long windowEnd = minuteTwo;
        Instant triggered = Instant.ofEpochMilli(minuteOne + 10_000L);
        alarmStore.persistExisting(new Alarm(
                "A1", 2L, "checkout", EventRule.WAY_THRESHOLD, "critical", "alarm",
                Alarm.STATUS_RESOLVED, triggered, triggered));

        Map<String, Object> data = service.countServiceAlarmsTotal(Map.of(
                "fromTime", minuteOne,
                "toTime", windowEnd,
                "windowEnd", windowEnd));

        assertThat(data.get("alarmCount")).isEqualTo(1L);
    }
}
