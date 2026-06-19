import i18n from '@/i18n';
import { Graph, EdgeData, GraphData, NodeData, NodePortStyleProps  } from "g6-v5";
import { BytesFilter, NumberFilter, PercentFilter } from '@/utils/filters/number'
import { MsFilter } from '@/utils/filters/times';
import { v4 as uuidv4 } from 'uuid'
import { getDbIcon } from "@/assets/fonts/db-find-icon";
import dagre from '@dagrejs/dagre';
import _, { cloneDeep } from "lodash";
import { NODE_SIZE, NODE_BOUND_SIZE, offscreenGraphOptions, type LayoutInfo } from "./config";

const systems = ['windows', 'linux', 'mac'];

export interface CustomConfig {
  direction: 'circular' | 'horizontal';
  lineType: '0.5' | '1' | '2' | '3';
  colorType: 'alarm';
  showLabels: boolean;
  maxNode: {
    service: number;
    process: number;
    host: number;
    application: number;
  };
  concatModel: {
    db: 'default' | 'type'
    mq: 'default' | 'type'
    cache: 'default' | 'type'
    remote: 'default' | 'ipaddress' | 'protocol'
  };
  visibleModel: {
    db: boolean;
    mq: boolean;
    cache: boolean;
    remote: boolean;
    nodata: boolean;
  }
}

export const defaultCustomConfig: () => CustomConfig = () => ({
  colorType: 'alarm',
  lineType: '1',
  showLabels: true,
  maxNode: {
    service: 500,
    process: 500,
    host: 500,
    application: 500,
  },
  direction: 'circular', // 'circular' | 'horizontal',
  concatModel: {
    db: 'default',
    mq: 'default',
    cache: 'default',
    remote: 'default',
  },
  visibleModel: {
    db: true,
    mq: true,
    cache: true,
    remote: true,
    nodata: true,
  }
});

export const BaseTypeParams: Record<APM.RelationMap['relationType'], { layoutName: string, edgeName: string }> = {
  application: { layoutName: 'applications', edgeName: 'applicationEdges' },
  business: { layoutName: 'businesses', edgeName: 'businessEdges' },
  service: { layoutName: 'services', edgeName: 'serviceEdges' },
  process: { layoutName: 'processes', edgeName: 'process2process' },
  host: { layoutName: 'hosts', edgeName: 'host2host' },
}

export const formatSource = async (nodesMap: any, edgesMap: any, customConfig: CustomConfig = defaultCustomConfig(), layoutInfo?: LayoutInfo) => {

  const { direction } = customConfig
  
  const sourceMap: Partial<Record<APM.RelationMap['relationType'], { alarmData: { error: number, total: number }, graphData: GraphData }>> = {};
  const _layoutData = Array.isArray(layoutInfo?.layoutData) ? layoutInfo?.layoutData : [];

  const _promises: Promise<GraphData>[] = [];
  Object.entries(BaseTypeParams).forEach(([key, val]) => {

    const _nodes: NodeData[] = [];
    const _edges: EdgeData[] = [];
    const _alarmData = { error: 0, total: 0 };
    const _nodeIdSet = new Set<string>();

    let layoutNodes = nodesMap?.[key] || [];
    let edgesData = edgesMap?.[key] || [];
    // 格式化连线的source 和 target 字段
    formatEdgeField(edgesData, key as APM.RelationMap['relationType'], layoutNodes);

    if (key === 'service') {
      const { nodes: filterNodes = [], edges: filterEdges = [] } = filterAndMergeByConfig(layoutNodes, edgesData, customConfig);
      layoutNodes = filterNodes;
      edgesData = filterEdges;
    }

    layoutNodes = layoutNodes.splice(0, customConfig.maxNode[key as keyof CustomConfig['maxNode']]);
    layoutNodes.forEach((n: any) => {
      const _nodeData: NodeData = {
        id: n.id || uuidv4(),
        type: 'circle',
      };
      switch (key) {
        case 'application':
          _nodeData.id = `app${n.id}`;
          _nodeData.data = {
            label: n.appName,
            showLabel: false,
            originType: n.appType,
            outType: 'application',
            subType: n.appType,
          }
          _nodeData.style = {
            labelText: n.appName,
            iconText: getDbIcon(n.appType || 'agent'),
          }
          break;
        case 'service':
          _nodeData.data = {
            label: n.name || n.serviceName,
            showLabel: false,
            originType: n.service_type,
            subType: n?.type || n?.language,
            outType: 'service',
            datasource: n?.datasource,
          }
          _nodeData.style = {
            labelText: n.name || n.serviceName,
            iconText: getDbIcon(n.type || n.language || n.service_type || 'default' || 'agent'),
          }
          break;
        case 'process':
          _nodeData.id = n.spuid;
          _nodeData.data = {
            label: n.pname,
            showLabel: false,
            pid: n?.pid,
            _clusterId: n?.clusterId,
            hostName: n?.hostName || n?.hostname,
            originType: n.type,
            outType: n.type,
            subType: n.type,
          }
          _nodeData.style = {
            labelText: n.pname,
            iconText: getDbIcon(n.type === 'pod' ? 'k8s-pod' : 'process'),
          }
          break;
        case 'host':
          const os = n?.os;
          let typeIcon = 'service-default';
          const operatingSystem = String(os).toLocaleLowerCase();
          typeIcon = systems.find(t => operatingSystem.includes(t)) || typeIcon;

          _nodeData.id = n.hostName;
          _nodeData.data = {
            label: n.hostName || n.hostname,
            showLabel: false,
            originType: systems.find(t => operatingSystem.includes(t)),
            outType: 'host',
            subType: systems.find(t => operatingSystem.includes(t)),
          }
          _nodeData.style = {
            labelText: n.hostName || n.hostname,
            iconText: getDbIcon(typeIcon),
          }
          break;
      }

      // 通用字段
      _nodeData.data = {
        ..._nodeData.data,
        alarmCount: n.alarmCount,
        hasAlarm: n.alarmCount > 0,
        selectedfill: n.alarmCount > 0 ? '#E12828' : '#2962FF',
      };
      _nodeData.style = {
        ..._nodeData.style,
        fill: n.alarmCount > 0 ? '#E12828' : '#F5F6F7',
        stroke: n.alarmCount > 0 ? '#E12828' : '#B5B7BB',
        iconFill: n.alarmCount > 0 ? '#FFFFFF' : '#121317',
      }
      // 定位坐标
      const layoutNode = _layoutData.find(item => item.id === _nodeData.id);
      if (typeof layoutNode?.x === 'number' && typeof layoutNode?.y === 'number' && layoutInfo?.version !== 'v1') {
        _nodeData.style.x = layoutNode.x;
        _nodeData.style.y = layoutNode.y;
      }
      _nodes.push(_nodeData);
      _nodeIdSet.add(String(_nodeData.id));
    });

    _alarmData.error = layoutNodes.filter((n: any) => n.alarmCount > 0).length;
    _alarmData.total = layoutNodes.length;

    edgesData.forEach((e: any) => {
      // 获取节点名称
      const sourceNode = _nodes.find((n: any) => String(n.id) === String(e.source));
      const targetNode = _nodes.find((n: any) => String(n.id) === String(e.target));
      e.sourceName = sourceNode ? sourceNode.data?.label : String(e.source);
      e.targetName = targetNode ? targetNode.data?.label : String(e.target);
      let _edgeData: EdgeData = {
        source: e?.source,
        target: e?.target,
        style: {},
        data: {
          sourceName: e.sourceName,
          targetName: e.targetName,
        }
      };
      switch (key) {
        case 'service':
          _edgeData = {
            source:  e.source || e.src,
            target: e.target || e.dst,
            style: {
              ..._edgeData.style,
              labelText: i18n.t('modules.views.appMonitor.relationMapNew.s_b0aee91e', { value0: NumberFilter(e.cnt) }) as string,
            },
            data: {
              ..._edgeData.data,
              cnt: e.cnt || 0,
              baseInfo: i18n.t('modules.views.appMonitor.relationMapNew.s_b0aee91e', { value0: NumberFilter(e.cnt) }) as string,
              fullInfo: i18n.t('modules.views.appMonitor.relationMapNew.s_1c69493f', { value0: MsFilter(e.avgDuration), value1: NumberFilter(e.cnt), value2: PercentFilter(e.successRate) }) as string,
            }
          }
          break;
        case 'process':
          _edgeData = {
            source:  e.source || e.laddrPspuid,
            target: e.raddrPspuid,
            style: {
              ..._edgeData.style,
              labelText: i18n.t('modules.views.appMonitor.relationMapNew.s_74d59c68', { value0: BytesFilter(e.volumeRcvd), value1: BytesFilter(e.volumeSent) }) as string,
            },
            data: {
              ..._edgeData.data,
              baseInfo: i18n.t('modules.views.appMonitor.relationMapNew.s_74d59c68', { value0: BytesFilter(e.volumeRcvd), value1: BytesFilter(e.volumeSent) }) as string,
              fullInfo: i18n.t('modules.views.appMonitor.relationMapNew.s_8ffc56ef', { value0: BytesFilter(e.volumeRcvd), value1: BytesFilter(e.volumeSent), value2: BytesFilter(e.throughputRcvd), value3: BytesFilter(e.throughputSent) }) as string,
            }
          }
          break;
        case 'host':
          _edgeData = {
            source: e.source || e.laddrHostname,
            target: e.target || e.raddrHostname,
            style: {
              ..._edgeData.style,
              labelText: i18n.t('modules.views.appMonitor.relationMapNew.s_74d59c68', { value0: BytesFilter(e.volumeRcvd), value1: BytesFilter(e.volumeSent) }) as string,
            },
            data: {
              ..._edgeData.data,
              baseInfo: i18n.t('modules.views.appMonitor.relationMapNew.s_74d59c68', { value0: BytesFilter(e.volumeRcvd), value1: BytesFilter(e.volumeSent) }) as string,
              fullInfo: i18n.t('modules.views.appMonitor.relationMapNew.s_8ffc56ef', { value0: BytesFilter(e.volumeRcvd), value1: BytesFilter(e.volumeSent), value2: BytesFilter(e.throughputRcvd), value3: BytesFilter(e.throughputSent) }) as string,
            }
          }
          break;
      }
      // 移除自身调用连线
      if (_edgeData.source !== _edgeData.target && _nodeIdSet.has(String(_edgeData.source)) && _nodeIdSet.has(String(_edgeData.target))) {
        _edges.push(_edgeData);
      }
    });

    // 到此处，已过滤自身调用的连线；已根据保存的布局数据，设置节点坐标（新增节点无坐标信息）

    // 同心圆流水线布局
    if (direction === 'circular') {
      _promises.push(getConcentricConcatLayout({  nodes: _nodes, edges: _edges }, NODE_BOUND_SIZE * 2, key));
    } else {
      _promises.push(getDagreConcatLayout({ nodes: _nodes, edges: _edges }));
    }

    sourceMap[key as APM.RelationMap['relationType']] = {
      alarmData: _alarmData,
      graphData: {
        nodes: _nodes || [],
        edges: _edges || [],
      }
    };

  });

  const results = await Promise.allSettled(_promises);

  Object.keys(sourceMap).forEach((key, index) => {
    if (results[index]?.status === 'fulfilled') {
      sourceMap[key as APM.RelationMap['relationType']]!.graphData = results[index].value;
    }
  });
  return sourceMap;
}

/** 全局拓扑初始视图最大缩放，避免节点被放得过大 */
export const GLOBAL_TOPOLOGY_INITIAL_MAX_ZOOM = 1

/** 适配初始视口：仅在内容溢出时缩小，且不超过 maxZoom，最后居中 */
export const fitTopologyInitialView = async (
  graph: Graph,
  maxZoom = GLOBAL_TOPOLOGY_INITIAL_MAX_ZOOM,
) => {
  if (graph.getNodeData().length === 0) {
    return
  }
  await graph.fitView({
    when: 'overflow',
    direction: 'both',
  })
  if (graph.getZoom() > maxZoom) {
    await graph.zoomTo(maxZoom)
  }
  await graph.fitCenter()
}

/** 根据画布尺寸与节点数量，放大过密的布局坐标，便于 fitView 计算出合适的初始缩放 */
export const expandLayoutToViewport = (
  graphData: GraphData,
  viewportWidth: number,
  viewportHeight: number,
  padding = 80,
) => {
  const nodes = Array.isArray(graphData.nodes) ? graphData.nodes : []
  if (nodes.length <= 1 || viewportWidth <= 0 || viewportHeight <= 0) {
    return
  }

  const positionedNodes = nodes.filter(
    (n) => typeof n.style?.x === 'number' && typeof n.style?.y === 'number',
  )
  if (positionedNodes.length <= 1) {
    return
  }

  const bboxes = positionedNodes.map((n) => ({
    minX: (n.style!.x as number) - NODE_BOUND_SIZE / 2,
    minY: (n.style!.y as number) - NODE_BOUND_SIZE / 2,
    maxX: (n.style!.x as number) + NODE_BOUND_SIZE / 2,
    maxY: (n.style!.y as number) + NODE_BOUND_SIZE / 2,
  }))
  const bbox = calculationItemsBBox(bboxes)
  const contentWidth = Math.max(bbox.width, NODE_BOUND_SIZE)
  const contentHeight = Math.max(bbox.height, NODE_BOUND_SIZE)

  const availableWidth = Math.max(viewportWidth - padding * 2, NODE_BOUND_SIZE * 2)
  const availableHeight = Math.max(viewportHeight - padding * 2, NODE_BOUND_SIZE * 2)
  const nodeCount = positionedNodes.length
  const fillRatio = nodeCount <= 3 ? 0.38 : nodeCount <= 8 ? 0.32 : nodeCount <= 20 ? 0.26 : 0.22
  const targetWidth = availableWidth * fillRatio
  const targetHeight = availableHeight * fillRatio
  const scale = Math.min(
    targetWidth / contentWidth,
    targetHeight / contentHeight,
    4,
  )

  if (scale <= 1) {
    return
  }

  const centerX = bbox.minX + bbox.width / 2
  const centerY = bbox.minY + bbox.height / 2
  positionedNodes.forEach((n) => {
    n.style = {
      ...n.style,
      x: ((n.style!.x as number) - centerX) * scale + centerX,
      y: ((n.style!.y as number) - centerY) * scale + centerY,
    }
  })
}

/** 根据访问量计算连线粗细，访问量越大线条越粗 */
export const calcEdgeLineWidthByCnt = (
  cnt: number,
  minCnt: number,
  maxCnt: number,
  baseWidth: number,
  len = 3,
): number => {
  const step = Math.max(baseWidth, 0.5)
  if (maxCnt <= minCnt || cnt <= 0) {
    return step
  }
  const bucketSize = (maxCnt - minCnt) / len || 1
  const scale = (cnt - minCnt) / bucketSize + 1
  return +(scale * step).toFixed(2)
}

export const richStyleByConfig = (graphData: GraphData, config: CustomConfig | null) => {
  const { direction = 'circular', lineType = '1' } = config || {};
  const baseWidth = Number(lineType || 1);
  const _nodes = Array.isArray(graphData.nodes) ? graphData.nodes : [];
  const _edges = Array.isArray(graphData.edges) ? graphData.edges : [];
  const isHorizontal = direction === 'horizontal';
  const _portConfig: NodePortStyleProps[] = isHorizontal ? [
    { key: 'port-left', placement: 'left' },
    { key: 'port-right', placement: 'right' },
  ] : [];

  const cntValues = _edges
    .map((e) => Number(e.data?.cnt ?? 0))
    .filter((v) => v > 0);
  const minCnt = cntValues.length ? Math.min(...cntValues) : 0;
  const maxCnt = cntValues.length ? Math.max(...cntValues) : 0;
  
  _nodes.forEach((n) => {
    // 根据布局方向调整节点图标旋转角度
    n.style = {
      ...n.style,
      label: !!config?.showLabels,
      ports: _portConfig,
    }
  });

  _edges.forEach((e) => {
    e.type = isHorizontal ? 'cubic-horizontal' : 'quadratic';
    const cnt = Number(e.data?.cnt ?? 0);
    const lineWidth = calcEdgeLineWidthByCnt(cnt, minCnt, maxCnt, baseWidth);
    e.style = {
      ...e.style,
      lineWidth,
      endArrowSize: Math.max(4, Math.min(8, lineWidth + 2)),
    }
  });
}

export const filterAndMergeByConfig = (nodes: any[], edges: any[], config: CustomConfig | null): GraphData => {
  const { visibleModel = defaultCustomConfig().visibleModel, concatModel = defaultCustomConfig().concatModel } = config || {};
  let _nodes = Array.isArray(nodes) ? nodes : [];
  let _edges = Array.isArray(edges) ? edges : [];
  // 过滤不可见节点
  _nodes = _nodes.filter((n) => {
    const originType = n.service_type;
    if (originType === 'db' && !visibleModel.db) {
      return false;
    }
    if (originType === 'mq' && !visibleModel.mq) {
      return false;
    }
    if (originType === 'cache' && !visibleModel.cache) {
      return false;
    }
    if (originType === 'remote' && !visibleModel.remote) {
      return false;
    }
    // 无连线数据节点，需要二次确认是否可见
    return true;
  });
  const visibleNodeIds = new Set<string>(_nodes.map(n => String(n.id)));
  // 过滤不可见连线
  const filterNodeIds = new Set<string>();
  _edges.filter((e) => visibleNodeIds.has(String(e.source)) && visibleNodeIds.has(String(e.target))).forEach((e) => {
    filterNodeIds.add(String(e.source));
    filterNodeIds.add(String(e.target));
  })
  _edges = _edges.filter((e) => {
    return visibleNodeIds.has(String(e.source)) && visibleNodeIds.has(String(e.target));
  });
  if (!visibleModel.nodata) {
    _nodes = _nodes.filter((n) => filterNodeIds.has(String(n.id)));
  }
  // TODO: 实现合并逻辑
  // 合并节点和连线逻辑（根据 concatModel 配置）
  if (concatModel?.db === 'type') {
    const dbTypeNodeMap: Record<string, { nodes: any[], source: any[], target: any[] }> = {}
    const dbNodes = _nodes.filter((n) => n.service_type === 'db');
    const dbNodeIds = dbNodes.map((n) => String(n.id));
    dbNodes.forEach((n) => {
      const subType: string = (n?.type || n?.language) as string;
      const dbTypeEdges = _edges.filter((e) => n.id === e.source || n.id === e.target);
      const sourceEdges = dbTypeEdges.filter((e) => n.id === e.source);
      const targetEdges = dbTypeEdges.filter((e) => n.id === e.target);
      if (subType) {
        if (!dbTypeNodeMap[subType]) {
          dbTypeNodeMap[subType] = { nodes: [], source: [], target: [] };
        }
        dbTypeNodeMap[subType].nodes.push(n);
        dbTypeNodeMap[subType].source = dbTypeNodeMap[subType].source.concat(sourceEdges);
        dbTypeNodeMap[subType].target = dbTypeNodeMap[subType].target.concat(targetEdges);
      }
    });
    Object.entries(dbTypeNodeMap).forEach(([subType, group]) => {
      const mergedNode: NodeData = {
        id: `db-type-${subType}`,
        name: `${subType}`,
        service_type: 'db',
        type: subType,
      };
      // 合并告警状态
      const alarmCount = group.nodes.reduce((sum, n) => sum + (n.alarmCount || 0), 0);
      mergedNode.alarmCount = alarmCount;
      mergedNode.errType = alarmCount > 0 ? 1 : 0;
      _nodes.push(mergedNode);

      // 重新构建连线，合并逻辑简化
      function mergeEdges(edges: any[], keyType: 'source' | 'target', mergedNodeId: string) {
        const map: Record<string, any> = {};
        edges.forEach((e) => {
          const key = keyType === 'source'
            ? `${mergedNodeId}|||${e.target}`
            : `${e.source}|||${mergedNodeId}`;
          if (!map[key]) {
            map[key] = {
              ...e,
              _durationSum: (e?.avgDuration || 0) * (e?.cnt || 0),
              _successSum: (e?.successRate || 0) * (e?.cnt || 0),
            };
          } else {
            const cnt = e?.cnt || 0;
            map[key].cnt += cnt;
            map[key]._durationSum += (e?.avgDuration || 0) * cnt;
            map[key]._successSum += (e?.successRate || 0) * cnt;
          }
        });
        return Object.entries(map).map(([key, e]) => {
          const [source, target] = key.split('|||');
          e.source = source;
          e.target = target;
          return e;
        });
      }

      const _newEdges = [
        ...mergeEdges(group.source, 'source', mergedNode.id),
        ...mergeEdges(group.target, 'target', mergedNode.id),
      ];

      _edges = _edges.concat(_newEdges.map((e) => {
        const cnt = e?.cnt || 0;
        e.avgDuration = cnt > 0 ? e._durationSum / cnt : 0;
        e.successRate = cnt > 0 ? e._successSum / cnt : 0;
        delete e._durationSum;
        delete e._successSum;
        return e;
      }));

    });
    _nodes = _nodes.filter((n) => !dbNodeIds.includes(String(n.id)));
    _edges = _edges.filter((e) => {
      // 移除与被合并节点相关的连线
      return !dbNodeIds.includes(String(e.source)) && !dbNodeIds.includes(String(e.target));
    });
  }
  return { nodes: _nodes, edges: _edges };
}

// 计算同心圆混合布局
async function getConcentricConcatLayout(graphData: GraphData = { nodes: [], edges: [] }, levelGap = 2 * NODE_BOUND_SIZE, key: string): Promise<GraphData> {
  return new Promise<GraphData>(async (resolve) => {

    if (!graphData.nodes || !Array.isArray(graphData?.nodes) || graphData.nodes.length === 0) {
      resolve({ nodes: [], edges: graphData?.edges || [] });
    }
    
    // 有连线的节点优先布局在内圈
    // 没有连线的节点，从内圈到外圈依次布局
    const cloneNodes = Array.isArray(graphData.nodes) ? cloneDeep(graphData.nodes) : [];
    const nodeWithoutEdges: NodeData[] = [];
    const nodeWithEdges: NodeData[] = [];
    const nodeSet = new Map<string, NodeData>();
    let baseRadius = 0;
    let offsetX = 0;
    let offsetY = 0;
  
    // 聚合有边/无边的节点
    const connectedNodeIds = new Set<string>();
    graphData.edges?.forEach((e) => {
      if (typeof e.source === 'string') {
        connectedNodeIds.add(e.source);
      }
      if (typeof e.target === 'string') {
        connectedNodeIds.add(e.target);
      }
    });

    graphData.nodes?.forEach((n) => {
      nodeSet.set(String(n.id), n);
    });
    cloneNodes.forEach((n) => {
      if (connectedNodeIds.has(n.id as string)) {
        nodeWithEdges.push(n);
      } else {
        nodeWithoutEdges.push(n);
      }
    });
    // 处理有边节点的布局
    // 有边的节点需要先用离屏graph进行forceAtlas2布局计算包围盒信息
    const waitForCalcForceBBox = () => new Promise<void>((resolve) => {
      if (nodeWithEdges.length === 0) {
        resolve();
        return;
      } else {
        let offscreenDiv: any = document.createElement('div');
        offscreenDiv.style.visibility = 'hidden';
        offscreenDiv.style.width = '1440px';
        offscreenDiv.style.height = '1440px';
        document.body.appendChild(offscreenDiv);
        const connectedCount = nodeWithEdges.length;
        const layoutKr = connectedCount <= 3 ? 80 : connectedCount <= 8 ? 40 : 10;
        const layoutKg = connectedCount <= 3 ? 5 : 8;
        let offscreenGraph: any = new Graph({
          ...offscreenGraphOptions,
          layout: {
            type: 'force-atlas2',
            preventOverlap: true,
            nodeSize: NODE_SIZE,
            maxIteration: 100,
            center: [0, 0],
            kr: layoutKr,
            kg: layoutKg,
            ks: 1.5,
          },
          container: offscreenDiv,
          data: {
            nodes: cloneDeep(nodeWithEdges),
            edges: cloneDeep(graphData.edges || []),
          },
        });
        offscreenGraph.on('afterlayout', () => {
          // const allNodes = offscreenGraph.getNodes();
          const bboxes: any[] = [];
          nodeWithEdges.forEach((n) => {
            const nodePosition = offscreenGraph.getElementPosition(n.id as string);
            const originalNode = nodeSet.get(String(n.id));
            if (originalNode) {
              originalNode.style = {
                ...originalNode.style,
                x: nodePosition[0],
                y: nodePosition[1],
              }
            }
            bboxes.push({
              minX: nodePosition[0] - NODE_BOUND_SIZE / 2 - 3,
              minY: nodePosition[1] - NODE_BOUND_SIZE / 2 - 3,
              maxX: nodePosition[0] + NODE_BOUND_SIZE / 2 + 3,
              maxY: nodePosition[1] + NODE_BOUND_SIZE / 2 + 3,
            });
          });
          const connectedBBox = calculationItemsBBox(bboxes);
          const maxSide = Math.max(connectedBBox.width, connectedBBox.height);
          baseRadius = maxSide / 2 + levelGap + NODE_BOUND_SIZE;
          offsetX = connectedBBox.minX + connectedBBox.width / 2;
          offsetY = connectedBBox.minY + connectedBBox.height / 2;
  
          document.body.removeChild(offscreenDiv);
          offscreenDiv = null;
          resolve();
        });
        offscreenGraph.on('afterrender', () => {
          offscreenGraph.clear();
          offscreenGraph.destroy();
          offscreenGraph = null;
        });
        offscreenGraph.render();
      }
    });
  
    await waitForCalcForceBBox();
  
    // 处理无边节点的布局
    const nodeDepth: any[][] = [];
    let depth = 0;
    const nodeDepthMap: any = {};
  
    while (nodeWithoutEdges.length > 0) {
      const calcRadius = baseRadius + depth * NODE_BOUND_SIZE * 2;
      const circleLen = calcRadius * Math.PI * 2;
      const nodeLen = Math.floor(circleLen / (NODE_BOUND_SIZE * 2)); // nodeSize 直径 + 间隙
      const depthNodes = nodeWithoutEdges.splice(0, nodeLen);
      depthNodes.forEach((n) => {
        n.outDepth = depth;
        nodeDepthMap[n.id] = depth;
      })
      nodeDepth.push(depthNodes); // 利用splice方法变更cloneNodes长度，while中判断length为0跳出
      depth += 1;
    }
  
    graphData.nodes!.forEach((n) => {
      if (Object.prototype.hasOwnProperty.call(nodeDepthMap, n.id)) {
        n.outDepth = nodeDepthMap[n.id]
      }
    });
  
    const pipes = nodeDepth.map((nd, index) => ({
      type: 'circular',
      center: [0, 0], // 环的中心
      radius: baseRadius + index * levelGap, // 环的半径x
      filterNodes: graphData.nodes!.filter((n) => n.outDepth === index),
    }));
  
    pipes.forEach((pipe) => {
      const _nodes = pipe.filterNodes as NodeData[];
      const nodeTotal = _nodes.length;
      const angle = Math.PI * 2 / nodeTotal;
      _nodes.forEach((n, index) => {
        if (!n.style) {
          n.style = {};
        }
        n.style!.x = Math.cos(angle * index) * pipe.radius + offsetX;
        n.style!.y = Math.sin(angle * index) * pipe.radius + offsetY;
      })
    })
    resolve(graphData)
    // console.log(cloneDeep(graphData))
  });
  
}

// 计算横向dagre混合布局
async function getDagreConcatLayout(graphData: GraphData = { nodes: [], edges: [] }): Promise<GraphData> {
  // 不同时间下，节点连线有不确定性，如果每次都进行dagre布局，可能导致节点位置跳动较大，或者与已有布局冲突
  // 方案: 首次或全部节点没有保存位置的情况下进行dagre布局计算；后续每次数据处理进行连通分量计算；
  // 遍历连通分量，对每个子图判断是否有任一已有保存位置的节点：
  //  1.如果有，则跳过dagre布局计算，使用已有位置，根据source/target连线方向设置新增节点；
  //  2.如果没有，则进行dagre布局计算，并设置在已有布局的下方开始布局
  
  return new Promise<GraphData>((resolve) => {
    
    if (!graphData.nodes || !Array.isArray(graphData?.nodes) || graphData.nodes.length === 0) {
      resolve({ nodes: [], edges: graphData?.edges || [] });
    }

    const cloneNodes = Array.isArray(graphData.nodes) ? cloneDeep(graphData.nodes) : [];
    const nodeWithoutEdges: NodeData[] = [];
    const nodeWithEdges: NodeData[] = [];
    const edgeWithoutCircle: EdgeData[] = Array.isArray(graphData.edges) ? graphData.edges.filter((e) => e.source !== e.target) : [];
    const nodeSet = new Map<string, NodeData>();
    const xMaxByRank = new Map<number, number>();
    const yMaxByRank = new Map<number, number>();
    xMaxByRank.set(0, 0);
    yMaxByRank.set(0, 0);
    
    let hasPositionNodeIds: string[] = [];

    // 聚合有边/无边的节点
    const connectedNodeIds = new Set<string>();
    edgeWithoutCircle.forEach((e) => {
      if (typeof e.source === 'string') {
        connectedNodeIds.add(e.source);
      }
      if (typeof e.target === 'string') {
        connectedNodeIds.add(e.target);
      }
    });

    graphData.nodes?.forEach((n) => {
      nodeSet.set(String(n.id), n);
      if (typeof n.style?.x === 'number' && typeof n.style?.y === 'number') {
        hasPositionNodeIds.push(String(n.id));
        const xRank = Math.floor(n.style.x / (NODE_BOUND_SIZE * 2));
        const existMaxX = xMaxByRank.get(xRank) || 0;
        const existMaxY = yMaxByRank.get(xRank) || 0;
        
        if (n.style.x > existMaxX) {
          xMaxByRank.set(xRank, n.style.x);
        }
        
        if (n.style.y > existMaxY) {
          yMaxByRank.set(xRank, n.style.y);
        }
      }
    });
    cloneNodes.forEach((n) => {
      if (connectedNodeIds.has(String(n.id))) {
        nodeWithEdges.push(n);
      } else {
        if (!hasPositionNodeIds.includes(String(n.id))) {
          nodeWithoutEdges.push(n);
        }
      }
    });

    const _components = getConnectedComponents(cloneDeep(nodeWithEdges), cloneDeep(edgeWithoutCircle));
    // 优先处理有保存位置节点的子图
    _components.forEach((component) => {
      if (component.some(id => hasPositionNodeIds.includes(id))) {
        // -> 有保存位置节点的子图 <-
        const _edges = edgeWithoutCircle.filter((e) => component.includes(String(e.source)) && component.includes(String(e.target)));
        const g = new dagre.graphlib.Graph();
        g.setGraph({ rankdir: 'LR', nodesep: 50, ranksep: 50 });
        g.setDefaultEdgeLabel(() => ({}));
        component.forEach((nodeId) => { g.setNode(nodeId, { width: 50, height: 50 }) });
        _edges.forEach((edge) => { g.setEdge(edge.source as string, edge.target as string) });
        dagre.layout(g);

        const nodesByRankFormated: Record<number, Array<{ x: number, y: number, rank: number, nodeId: string }>> = {};
        const nodesByRank: Record<number, Array<{ x: number, y: number, rank: number, nodeId: string }>> = {};
        g.nodes().forEach((nodeId) => {
          const nodeInfo = g.node(nodeId);
          const { x, y, rank = 0 } = nodeInfo;
          if (!nodesByRank[rank]) {
            nodesByRank[rank] = [{x, y, rank, nodeId}]
          } else {
            nodesByRank[rank].push({x, y, rank, nodeId});
          }
        });
        Object.keys(nodesByRank).map((rankKey) => Number(rankKey)).sort((a, b) => a - b).forEach((rankNum, index) => {
          nodesByRank[rankNum].sort((a, b) => a.y - b.y);
          nodesByRankFormated[index] = nodesByRank[rankNum];
        });
        // 找到第一个有位置的节点，作为参考点
        const referenceNodeId = component.find(id => hasPositionNodeIds.includes(id)) as string;
        const originalReferenceNode = nodeSet.get(referenceNodeId);
        const referenceRank = Object.entries(nodesByRankFormated).find(([rank, nodes]) => nodes.find(n => n.nodeId === referenceNodeId))?.[0];
        const referenceColIndex = nodesByRankFormated[Number(referenceRank)].findIndex(n => n.nodeId === referenceNodeId);

        Object.entries(nodesByRankFormated).forEach(([rank, __nodes], index) => {
          const rankNum = Number(rank);
          const _x = (rankNum - (Number(referenceRank) || 0)) * NODE_BOUND_SIZE * 2 + (originalReferenceNode?.style?.x || 0);
          __nodes.forEach((__n, __nIndex) => {
            const originalNode = nodeSet.get(__n.nodeId);
            if (typeof originalNode?.style?.x === 'number' && typeof originalNode?.style?.y === 'number') {
              return; // 已有位置不处理
            }
            let _y = NODE_BOUND_SIZE * 2 * __nIndex + (originalReferenceNode?.style?.y || 0) - (referenceColIndex * NODE_BOUND_SIZE * 2);
            // 判断[_x, -y]位置是否有节点占用
            const _xPoses = Array.from(nodeSet.values()).filter(n => n.style?.x === _x).map(n => n.style?.y);
            let maxIteration = 0; // 防止死循环
            let upY = _y;
            let downY = _y;
            while ((_xPoses.includes(upY) || _xPoses.includes(downY)) && maxIteration < 1000) {
              upY -= NODE_BOUND_SIZE * 2; // 有占用则上移一个节点间距
              downY += NODE_BOUND_SIZE * 2; // 有占用则下移一个节点间距
              maxIteration += 1;
            }
            if (!(_xPoses.includes(upY))) {
              _y = upY;
            } else if (!(_xPoses.includes(downY))) {
              _y = downY;
            }
            if (originalNode) {
              originalNode.style = {
                ...originalNode.style,
                x: _x,
                y: _y,
              }
            }
          })
        });
      }
    });

    // 其次处理没有保存位置节点的子图
    _components.forEach((component) => {
      if (component.some(id => hasPositionNodeIds.includes(id))) {
        // 上面处理过，跳过
      } else {
        const _edges = edgeWithoutCircle.filter((e) => component.includes(String(e.source)) && component.includes(String(e.target)));
  
        const g = new dagre.graphlib.Graph();
        g.setGraph({ rankdir: 'LR', nodesep: 50, ranksep: 50 });
        g.setDefaultEdgeLabel(() => ({}));
        component.forEach((nodeId) => { g.setNode(nodeId, { width: 50, height: 50 }) });
        _edges.forEach((edge) => { g.setEdge(edge.source as string, edge.target as string) });
        dagre.layout(g);

        const nodesByRankFormated: Record<number, Array<{ x: number, y: number, rank: number, nodeId: string }>> = {};
        const nodesByRank: Record<number, Array<{ x: number, y: number, rank: number, nodeId: string }>> = {};
        g.nodes().forEach((nodeId) => {
          const nodeInfo = g.node(nodeId);
          const { x, y, rank = 0 } = nodeInfo;
          if (!nodesByRank[rank]) {
            nodesByRank[rank] = [{x, y, rank, nodeId}]
          } else {
            nodesByRank[rank].push({x, y, rank, nodeId});
          }
        });
          
        const yOffsetByRank: number[] = [];
        Object.keys(nodesByRank).map((rankKey) => Number(rankKey)).sort((a, b) => a - b).forEach((rankNum, index) => {
          nodesByRankFormated[index] = nodesByRank[rankNum];
          yOffsetByRank.push(yMaxByRank.get(index) || 25);
        });
        const yOffset = Math.max(...yOffsetByRank) + ( Math.max(...yOffsetByRank) > 0 ? NODE_BOUND_SIZE * 2 : 0 );
        const maxYCount = Math.max(...Object.values(nodesByRank).map(nl => nl.length));

        Object.entries(nodesByRankFormated).forEach(([rank, nodes]) => {
          const rankNum = Number(rank);
          nodes.sort((a, b) => a.y - b.y);
          nodes.forEach((n, nIndex) => {
            const originalNode = nodeSet.get(n.nodeId);
            if (originalNode) {
              const rankCount = nodesByRankFormated[rankNum].length;
              let positionY = NODE_BOUND_SIZE * 2 * nIndex + Math.floor((maxYCount - rankCount) / 2) * NODE_BOUND_SIZE * 2 + yOffset;
              originalNode.style = {
                ...originalNode.style,
                x: NODE_BOUND_SIZE * 2 * rankNum,
                y: positionY,
              }
              // 更新xMaxByRank
              const existMaxX = xMaxByRank.get(rankNum) || 0;
              const positionX = NODE_BOUND_SIZE * 2 * rankNum;
              if (positionX > existMaxX) {
                xMaxByRank.set(rankNum, positionX);
              }
              // 更新yMaxByRank
              const existMaxY = yMaxByRank.get(rankNum) || 0;
              // console.log(positionY, existMaxY, rankNum)
              if (positionY > existMaxY) {
                yMaxByRank.set(rankNum, positionY);
              }
            }
          });
        });
      }
    });

    // 处理没有连线的节点，使用网格布局方式计算节点坐标，置于最右侧
    const totalRanks = xMaxByRank.size;
    const startX = (xMaxByRank.get(totalRanks - 1) || 0) + NODE_BOUND_SIZE * 2;
    const defaultMaxY = NODE_BOUND_SIZE * 2 * 10; // 默认10行高度
    const endY = Math.max(...Array.from(yMaxByRank.values()), defaultMaxY);
    nodeWithoutEdges.forEach((n, index) => {
      const rowNum = Math.floor((endY) / (NODE_BOUND_SIZE * 2));
      const positionX = startX + NODE_BOUND_SIZE * 2 * Math.floor(index / rowNum);
      const positionY = NODE_BOUND_SIZE * 2 * (index % rowNum);
      const originalNode = nodeSet.get(String(n.id));
      if (originalNode) {
        originalNode.style = {
          ...originalNode.style,
          x: positionX,
          y: positionY,
        }
      }
    });
    resolve(graphData);
  });
}

// DFS深度优先搜索获取图的连通分量
function getConnectedComponents(nodes: { id: string }[], edges: { source: string; target: string }[]): string[][] {
  const nodeMap = new Map(nodes.map(n => [n.id, n]));
  const visited = new Set<string>();
  const adj: Record<string, string[]> = {};

  // 构建邻接表
  nodes.forEach(n => { adj[n.id] = []; });
  edges.forEach(e => {
    adj[e.source].push(e.target);
    adj[e.target].push(e.source);
  });

  const components: string[][] = [];

  function dfs(id: string, group: string[]) {
    visited.add(id);
    group.push(id);
    adj[id].forEach(nei => {
      if (!visited.has(nei)) dfs(nei, group);
    });
  }

  nodes.forEach(n => {
    if (!visited.has(n.id)) {
      const group: string[] = [];
      dfs(n.id, group);
      components.push(group);
    }
  });

  return components; // 每个 group 是一组连通节点 id
}

// 遍历getConnectedComponents获取的连通分量，对每个子图进行dagre布局
function applyDagreLayoutToComponents(graphData: GraphData = { nodes: [], edges: [] }, components: string[][]): GraphData {
  if (!Array.isArray(graphData.nodes)) {
    return { nodes: [], edges: [] };
  }
  const _nodes = cloneDeep(graphData.nodes);
  const _edges = cloneDeep(graphData?.edges || []);
  // console.log(_nodes, _edges, components);
  const nodeMap = new Map(_nodes.map(n => [n.id, n]));
  const edgeMap = _edges;

  const layoutedNodes: NodeData[] = [];
  const layoutedEdges: EdgeData[] = [];

  components.forEach(component => {
    const subgraphNodes = component.map(id => nodeMap.get(id)).filter(n => n) as NodeData[];
    const subgraphEdges = edgeMap.filter(e => component.includes(e.source as string) && component.includes(e.target as string));

    // 使用 dagre 布局
    const g = new dagre.graphlib.Graph();
    g.setGraph({ rankdir: 'LR', nodesep: 100, ranksep: 100 });
    g.setDefaultEdgeLabel(() => ({}));

    subgraphNodes.forEach(node => {
      g.setNode(node.id, { width: 100, height: 100 });
    });

    subgraphEdges.forEach(edge => {
      g.setEdge(edge.source as string, edge.target as string);
    });

    dagre.layout(g);

    g.nodes().forEach((nodeId) => {
      const nodeInfo = g.node(nodeId);
      const originalNode = nodeMap.get(nodeId);
      if (originalNode) {
        layoutedNodes.push({
          ...originalNode,
          x: nodeInfo.x,
          y: nodeInfo.y,
        });
      }
    });
    

    layoutedEdges.push(...subgraphEdges);
  });
  console.log('dagre layout result:', layoutedNodes, layoutedEdges);

  return {
    nodes: layoutedNodes,
    edges: layoutedEdges,
  };
}

function calculationItemsBBox(items: any[]) {
  var minx = Infinity;
  var maxx = -Infinity;
  var miny = Infinity;
  var maxy = -Infinity;
  // 获取已节点的所有最大最小x y值
  for (var i = 0; i < items.length; i++) {
    var bbox = items[i];
    var minX = bbox.minX,
      minY = bbox.minY,
      maxX = bbox.maxX,
      maxY = bbox.maxY;
    if (minX < minx) {
      minx = minX;
    }
    if (minY < miny) {
      miny = minY;
    }
    if (maxX > maxx) {
      maxx = maxX;
    }
    if (maxY > maxy) {
      maxy = maxY;
    }
  }
  var x = Math.floor(minx);
  var y = Math.floor(miny);
  var width = Math.ceil(maxx) - Math.floor(minx);
  var height = Math.ceil(maxy) - Math.floor(miny);
  return {
    x: x,
    y: y,
    width: width,
    height: height,
    minX: minx,
    minY: miny,
    maxX: maxx,
    maxY: maxy
  };
};

function formatEdgeField (edges: EdgeData[], key: APM.RelationMap['relationType'], layoutNodes: any[]) {
  edges.forEach((e: any) => {
    switch (key) {
      case 'service':
        e.source = e.src;
        e.target = e.dst;
        break;
      case 'process':
        e.source = e.laddrPspuid;
        e.target = e.raddrPspuid;
        break;
      case 'host':
        e.source = e.laddrHostname;
        e.target = e.raddrHostname;
        break;
    }
  });
}
