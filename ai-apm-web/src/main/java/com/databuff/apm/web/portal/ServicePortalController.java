package com.databuff.apm.web.portal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Portal-compatible service APIs ({@code POST /webapi/service/*}).
 * Response shapes match databuff-portal / U1.1.0-databuff webapp.
 */
@RestController
@RequestMapping("/service")
public class ServicePortalController {

    private final ServicePortalService servicePortalService;

    public ServicePortalController(ServicePortalService servicePortalService) {
        this.servicePortalService = servicePortalService;
    }

    @PostMapping("/serviceListTrendChart")
    public Map<String, Object> serviceListTrendChart(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.serviceListTrendChart(body));
    }

    @PostMapping("/list")
    public Map<String, Object> list(@RequestBody Map<String, Object> body) {
        return servicePortalService.list(body);
    }

    @PostMapping("/serviceInfo")
    public Map<String, Object> serviceInfo(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.serviceInfo(body));
    }

    @PostMapping("/serviceDetailTrendChart")
    public Map<String, Object> serviceDetailTrendChart(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.serviceDetailTrendChart(body));
    }

    @PostMapping("/graph_stats")
    public Map<String, Object> graphStats(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.graphStats(body));
    }

    @PostMapping("/call_graph_stats")
    public Map<String, Object> callGraphStats(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.callGraphStats(body));
    }

    @PostMapping("/middlewareList")
    public Map<String, Object> middlewareList(@RequestBody Map<String, Object> body) {
        return servicePortalService.middlewareList(body);
    }

    @PostMapping("/dbList")
    public Map<String, Object> dbList(@RequestBody Map<String, Object> body) {
        return servicePortalService.dbList(body);
    }

    @PostMapping("/mqList")
    public Map<String, Object> mqList(@RequestBody Map<String, Object> body) {
        return servicePortalService.mqList(body);
    }

    @PostMapping("/cacheList")
    public Map<String, Object> cacheList(@RequestBody Map<String, Object> body) {
        return servicePortalService.cacheList(body);
    }

    @PostMapping("/remoteCallList")
    public Map<String, Object> remoteCallList(@RequestBody Map<String, Object> body) {
        return servicePortalService.remoteCallList(body);
    }

    @PostMapping("/relationList")
    public Map<String, Object> relationList(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.relationList(body));
    }

    @PostMapping("/endpoints")
    public Map<String, Object> endpoints(@RequestBody Map<String, Object> body) {
        return servicePortalService.endpoints(body);
    }

    @PostMapping("/distribution_stats")
    public Map<String, Object> distributionStats(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.distributionStats(body));
    }

    @PostMapping("/reqTop")
    public Map<String, Object> reqTop(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.reqTop(body));
    }

    @PostMapping("/slowSqlTopList")
    public Map<String, Object> slowSqlTopList(@RequestBody Map<String, Object> body) {
        return servicePortalService.slowSqlTopList(body);
    }

    @PostMapping("/exceptionDistMap")
    public Map<String, Object> exceptionDistMap(@RequestBody Map<String, Object> body) {
        return servicePortalService.exceptionDistMap(body);
    }

    @PostMapping("/call_endpoints")
    public Map<String, Object> callEndpoints(@RequestBody Map<String, Object> body) {
        return servicePortalService.callEndpoints(body);
    }

    @PostMapping("/resourceInfo")
    public Map<String, Object> resourceInfo(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.resourceInfo(body));
    }

    @PostMapping("/resources")
    public Map<String, Object> resources(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.resources(body));
    }

    @PostMapping("/resourcesGroupBy")
    public Map<String, Object> resourcesGroupBy(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.resourcesGroupBy(body));
    }

    @PostMapping("/metric_stats")
    public Map<String, Object> metricStats(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.metricStats(body));
    }

    @PostMapping("/resource_stats")
    public Map<String, Object> resourceStats(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.resourceStats(body));
    }

    @PostMapping("/call_info")
    public Map<String, Object> callInfo(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.callInfo(body));
    }

    @PostMapping("/reqContributorService")
    public Map<String, Object> reqContributorService(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.reqContributorService(body));
    }

    @PostMapping("/basicServices")
    public Map<String, Object> basicServices(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.basicServices(body));
    }

    @PostMapping("/basicAllServices")
    public Map<String, Object> basicAllServices(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.basicAllServices(body));
    }

    @PostMapping("/getBasicServiceInstance")
    public Map<String, Object> getBasicServiceInstance(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.getBasicServiceInstance(body));
    }

    @GetMapping("/getBasicServiceInstance")
    public Map<String, Object> getBasicServiceInstanceGet(
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String fromTime,
            @RequestParam(required = false) String toTime,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String sid) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (start != null) {
            body.put("start", start);
        }
        if (end != null) {
            body.put("end", end);
        }
        if (fromTime != null) {
            body.put("fromTime", fromTime);
        }
        if (toTime != null) {
            body.put("toTime", toTime);
        }
        if (serviceId != null) {
            body.put("serviceId", serviceId);
        }
        if (sid != null) {
            body.put("sid", sid);
        }
        return portalEnvelope(servicePortalService.getBasicServiceInstance(body));
    }

    @GetMapping("/getServiceInstance")
    public Map<String, Object> getServiceInstance(
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String sid,
            @RequestParam(required = false) String serviceInstance,
            @RequestParam(required = false) String si) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (start != null) {
            body.put("start", start);
        }
        if (end != null) {
            body.put("end", end);
        }
        if (serviceId != null) {
            body.put("serviceId", serviceId);
        }
        if (sid != null) {
            body.put("sid", sid);
        }
        if (serviceInstance != null) {
            body.put("serviceInstance", serviceInstance);
        }
        if (si != null) {
            body.put("si", si);
        }
        return portalEnvelope(servicePortalService.getServiceInstance(body));
    }

    @GetMapping("/getServiceInstanceRelations")
    public Map<String, Object> getServiceInstanceRelations(
            @RequestParam(required = false) Long start,
            @RequestParam(required = false) Long end,
            @RequestParam(required = false) String serviceId,
            @RequestParam(required = false) String serviceInstance) {
        Map<String, Object> body = new LinkedHashMap<>();
        if (start != null) {
            body.put("start", start);
        }
        if (end != null) {
            body.put("end", end);
        }
        if (serviceId != null) {
            body.put("serviceId", serviceId);
        }
        if (serviceInstance != null) {
            body.put("serviceInstance", serviceInstance);
        }
        return portalEnvelope(servicePortalService.getServiceInstanceRelations(body));
    }

    @PostMapping("/k8sNamespaceList")
    public Map<String, Object> k8sNamespaceList(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.k8sNamespaceList(body));
    }

    @PostMapping("/pool_get_names")
    public Map<String, Object> poolGetNames(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.poolGetNames(body));
    }

    @PostMapping("/resourceRelation")
    public Map<String, Object> resourceRelation(@RequestBody Map<String, Object> body) {
        return portalEnvelope(servicePortalService.resourceRelation(body));
    }

    private static Map<String, Object> portalEnvelope(Object data) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", 200);
        response.put("message", "success");
        response.put("data", data);
        return response;
    }
}
