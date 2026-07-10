package com.databuff.apm.web.flow;

import com.databuff.apm.common.flow.ServiceFlowSpanRules;
import com.databuff.apm.common.query.ApmQueryModels.ServiceFlowTreeRow;
import com.databuff.apm.common.util.PortalServiceIdResolver;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/** Builds portal multiple-service-flow trees from aggregated {@code metric_service_flow} rows. */
public final class MultipleServiceFlowTreeBuilder {

    private MultipleServiceFlowTreeBuilder() {
    }

    public static Map<String, Map<String, Object>> build(
            List<ServiceFlowTreeRow> rows,
            String dstServiceId,
            Set<String> pathIds) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        List<FlowNode> merged = mergeRows(rows);
        List<FlowNode> roots = new ArrayList<>();
        getRootList(merged, roots, dstServiceId, pathIds == null ? Set.of() : pathIds);
        Map<String, Map<String, Object>> serviceFlows = new LinkedHashMap<>();
        for (FlowNode root : roots) {
            serviceFlows.put(root.service, toMap(root));
        }
        return serviceFlows;
    }

    private static List<FlowNode> mergeRows(List<ServiceFlowTreeRow> rows) {
        Map<String, List<ServiceFlowTreeRow>> grouped = rows.stream()
                .collect(Collectors.groupingBy(ServiceFlowTreeRow::pathId));
        List<FlowNode> merged = new ArrayList<>();
        for (List<ServiceFlowTreeRow> group : grouped.values()) {
            ServiceFlowTreeRow preferred = group.stream()
                    .filter(row -> row.isIn() == 1)
                    .findFirst()
                    .orElse(group.get(0));
            long call = 0L;
            long error = 0L;
            long srcCall = 0L;
            long duration = 0L;
            long minDuration = Long.MAX_VALUE;
            long maxDuration = Long.MIN_VALUE;
            Set<String> resources = new LinkedHashSet<>();
            for (ServiceFlowTreeRow row : group) {
                call += row.callCount();
                error += row.errorCount();
                srcCall += row.srcCall();
                duration += row.sumDuration();
                minDuration = Math.min(minDuration, row.sumDuration());
                maxDuration = Math.max(maxDuration, row.sumDuration());
                if (row.resource() != null && !row.resource().isBlank()
                        && !ServiceFlowSpanRules.isComponentResource(row.resource())) {
                    resources.add(row.resource());
                }
            }
            FlowNode node = new FlowNode();
            node.uid = nullToEmpty(preferred.pathId());
            node.parentId = nullToEmpty(preferred.parentPathId());
            node.serviceId = PortalServiceIdResolver.resolve(preferred.serviceId(), preferred.service());
            node.service = nullToEmpty(preferred.service());
            node.resource = ServiceFlowSpanRules.isComponentResource(preferred.resource())
                    ? ""
                    : nullToEmpty(preferred.resource());
            node.isIn = preferred.isIn();
            node.call = call;
            node.error = error;
            node.srcCall = srcCall;
            node.duration = duration;
            node.avgDuration = duration;
            node.minDuration = minDuration == Long.MAX_VALUE ? 0L : minDuration;
            node.maxDuration = maxDuration == Long.MIN_VALUE ? 0L : maxDuration;
            node.resources = resources;
            merged.add(node);
        }
        return merged;
    }

    private static void getRootList(
            List<FlowNode> nodes,
            List<FlowNode> rootList,
            String dstServiceId,
            Set<String> pathIds) {
        nodes.removeIf(node -> Objects.equals(node.uid, node.parentId));
        Map<String, List<FlowNode>> groupMap = nodes.stream()
                .collect(Collectors.groupingBy(node -> node.uid + node.parentId));
        groupMap.forEach((key, value) -> {
            Optional<FlowNode> inbound = value.stream().filter(node -> node.isIn == 1).findFirst();
            if (inbound.isPresent()) {
                value.removeIf(node -> node.isIn == 0);
            }
        });
        nodes.clear();
        nodes.addAll(groupMap.values().stream().map(value -> value.get(0)).toList());

        List<FlowNode> treeNodeList = new ArrayList<>();
        for (FlowNode node : nodes) {
            if (node.parentId == null || node.parentId.isBlank()) {
                rootList.add(node);
            } else {
                treeNodeList.add(node);
            }
        }

        Map<String, List<FlowNode>> treeNodeMap = treeNodeList.stream()
                .filter(node -> node.parentId != null && !node.parentId.isBlank())
                .collect(Collectors.groupingBy(node -> node.parentId));
        for (FlowNode root : rootList) {
            generateTree(root, treeNodeMap, 0, pathIds);
            horizontalMergeWithSameServiceId(root);
            verticalMergeWithSameServiceId(root);
            treeSetData(root, root.children, root.duration);
        }

        if (dstServiceId != null && !dstServiceId.isBlank()) {
            treeNodeList.addAll(rootList);
            Map<String, FlowNode> treeMap = treeNodeList.stream()
                    .collect(Collectors.toMap(node -> node.uid, node -> node, (left, right) -> left));
            List<FlowNode> dstNodes = treeNodeList.stream()
                    .filter(node -> PortalServiceIdResolver.matches(dstServiceId, node.serviceId))
                    .toList();
            for (FlowNode node : dstNodes) {
                node.dts = true;
                FlowNode index = node;
                do {
                    index.show = true;
                    index = treeMap.get(index.parentId);
                } while (index != null);
            }
            rootList.removeIf(root -> !root.show);
            for (FlowNode root : rootList) {
                deleteNode(root);
            }
        }
    }

    private static void deleteNode(FlowNode node) {
        if (node.dts || node.children.isEmpty()) {
            return;
        }
        List<FlowNode> kept = new ArrayList<>();
        for (FlowNode child : node.children) {
            if (child.show) {
                kept.add(child);
            }
        }
        node.children = kept;
        for (FlowNode child : kept) {
            deleteNode(child);
        }
    }

    private static void generateTree(
            FlowNode root,
            Map<String, List<FlowNode>> treeNodeMap,
            int level,
            Set<String> pathIds) {
        root.level = level;
        root.pathIds.add(root.uid);
        List<FlowNode> children = new ArrayList<>();
        findChildren(root, treeNodeMap, children);
        List<FlowNode> filtered = new ArrayList<>();
        for (FlowNode child : children) {
            if (pathIds.contains(child.uid)) {
                filtered.add(child);
            }
        }
        root.children = filtered.isEmpty() ? children : filtered;
        for (FlowNode child : children) {
            generateTree(child, treeNodeMap, level + 1, pathIds);
        }
    }

    private static void findChildren(
            FlowNode node,
            Map<String, List<FlowNode>> treeNodeMap,
            List<FlowNode> newChildren) {
        List<FlowNode> children = treeNodeMap.get(node.uid);
        if (children == null) {
            return;
        }
        for (FlowNode child : children) {
            if (child.isIn == 1) {
                newChildren.add(child);
            } else {
                findChildren(child, treeNodeMap, newChildren);
            }
        }
        treeNodeMap.remove(node.uid);
    }

    private static void horizontalMergeWithSameServiceId(FlowNode parent) {
        if (parent == null || parent.children == null) {
            return;
        }
        if (parent.isIn == 0) {
            parent.srcCall = 0L;
        }
        Map<String, List<FlowNode>> groupingBy = parent.children.stream()
                .collect(Collectors.groupingBy(node -> node.serviceId));
        groupingBy.forEach((key, value) -> {
            for (int i = 0; i < value.size(); i++) {
                if (i != 0) {
                    value.get(i).srcCall = 0L;
                }
            }
        });

        List<FlowNode> addChildren = new ArrayList<>();
        for (FlowNode child : parent.children) {
            if (parent.serviceId.equals(child.serviceId)) {
                if (parent.isIn == 0) {
                    parent.duration = child.duration;
                    parent.call = child.call;
                    parent.error = child.error;
                    parent.srcCall = child.srcCall;
                    parent.minDuration = child.minDuration;
                    parent.maxDuration = child.maxDuration;
                    parent.isIn = child.isIn;
                    parent.resources = new LinkedHashSet<>(filterFlowResources(child.resources));
                    parent.pathIds.addAll(child.pathIds);
                } else {
                    parent.resources.addAll(filterFlowResources(child.resources));
                    parent.duration += child.duration;
                    parent.call += child.call;
                    parent.error += child.error;
                    parent.srcCall += child.srcCall;
                    parent.minDuration = Math.min(parent.minDuration, child.minDuration);
                    parent.maxDuration = Math.max(parent.maxDuration, child.maxDuration);
                    parent.pathIds.addAll(child.pathIds);
                }
                addChildren.addAll(child.children);
                horizontalMergeWithSameServiceId(child);
            } else {
                addChildren.add(child);
                horizontalMergeWithSameServiceId(child);
            }
        }
        parent.children = addChildren;
    }

    private static void verticalMergeWithSameServiceId(FlowNode node) {
        if (node == null || node.children == null) {
            return;
        }
        Map<String, List<FlowNode>> groupBy = node.children.stream()
                .collect(Collectors.groupingBy(child -> child.serviceId));
        List<FlowNode> mergeList = new ArrayList<>();
        groupBy.forEach((key, value) -> {
            if (value.size() > 1) {
                FlowNode first = value.get(0);
                for (int i = 1; i < value.size(); i++) {
                    FlowNode other = value.get(i);
                    first.resources.addAll(filterFlowResources(other.resources));
                    first.duration += other.duration;
                    first.srcCall += other.srcCall;
                    first.call += other.call;
                    first.error += other.error;
                    first.minDuration = Math.min(first.minDuration, other.minDuration);
                    first.maxDuration = Math.max(first.maxDuration, other.maxDuration);
                    first.children.addAll(other.children);
                    first.pathIds.addAll(other.pathIds);
                    verticalMergeWithSameServiceId(first);
                }
                mergeList.add(first);
            } else {
                mergeList.add(value.get(0));
                verticalMergeWithSameServiceId(value.get(0));
            }
        });
        node.children = mergeList;
    }

    private static void treeSetData(FlowNode parentTree, List<FlowNode> childTrees, long rootDuration) {
        if (parentTree == null || childTrees == null) {
            return;
        }
        long outCall = 0L;
        for (FlowNode childTree : childTrees) {
            outCall += childTree.call;
        }
        parentTree.outCall = outCall;
        parentTree.avgDuration = parentTree.call == 0L ? 0L : parentTree.duration / parentTree.call;

        for (FlowNode childTree : childTrees) {
            childTree.durationCvPct = rootDuration == 0L
                    ? 0.0
                    : BigDecimal.valueOf(childTree.duration)
                            .divide(BigDecimal.valueOf(rootDuration), 2, RoundingMode.HALF_UP)
                            .doubleValue();
            childTree.callPct = parentTree.call == 0L
                    ? 0.0
                    : BigDecimal.valueOf(childTree.srcCall)
                            .divide(BigDecimal.valueOf(parentTree.call), 2, RoundingMode.HALF_UP)
                            .doubleValue();
            childTree.avgCall = childTree.srcCall == 0L
                    ? 0.0
                    : BigDecimal.valueOf(childTree.call)
                            .divide(BigDecimal.valueOf(childTree.srcCall), 2, RoundingMode.HALF_UP)
                            .doubleValue();
            treeSetData(childTree, childTree.children, rootDuration);
        }
    }

    private static Map<String, Object> toMap(FlowNode node) {
        List<Map<String, Object>> children = node.children.stream().map(MultipleServiceFlowTreeBuilder::toMap).toList();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("uid", node.uid);
        map.put("parentId", node.parentId);
        map.put("level", node.level);
        map.put("service", node.service);
        map.put("name", node.service);
        map.put("resource", node.resource);
        map.put("serviceId", node.serviceId);
        map.put("srcCall", node.srcCall);
        map.put("call", node.call);
        map.put("isIn", node.isIn);
        map.put("error", node.error);
        map.put("duration", node.duration);
        map.put("avgDuration", node.avgDuration);
        map.put("callPct", node.callPct);
        map.put("avgCall", node.avgCall);
        map.put("durationCvPct", node.durationCvPct);
        map.put("maxDuration", node.maxDuration);
        map.put("minDuration", node.minDuration);
        map.put("outCall", node.outCall);
        map.put("resources", new ArrayList<>(node.resources));
        map.put("pathIds", new ArrayList<>(node.pathIds));
        map.put("serviceDurationRange", List.of());
        map.put("children", children);
        return map;
    }

    private static Set<String> filterFlowResources(Set<String> resources) {
        if (resources == null || resources.isEmpty()) {
            return Set.of();
        }
        Set<String> filtered = new LinkedHashSet<>();
        for (String resource : resources) {
            if (!ServiceFlowSpanRules.isComponentResource(resource)) {
                filtered.add(resource);
            }
        }
        return filtered;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class FlowNode {
        private String uid;
        private String parentId;
        private int level;
        private String service;
        private String resource;
        private String serviceId;
        private long srcCall;
        private long call;
        private int isIn;
        private long error;
        private long duration;
        private long avgDuration;
        private double callPct;
        private double avgCall;
        private double durationCvPct;
        private long outCall;
        private long maxDuration;
        private long minDuration;
        private Set<String> resources = new LinkedHashSet<>();
        private Set<String> pathIds = new LinkedHashSet<>();
        private List<FlowNode> children = new ArrayList<>();
        private boolean show;
        private boolean dts;
    }
}
