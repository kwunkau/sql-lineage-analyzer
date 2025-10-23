// SQL字段级血缘分析平台 - DAG 可视化

(function() {
  'use strict';

  /**
   * 血缘可视化类
   * @class LineageVisualizer
   */
  class LineageVisualizer {
    constructor() {
      this.container = document.getElementById('graphContainer');
      this.currentData = null;
      this.graph = null;
      this.highlightedNodes = new Set();
      this.highlightedEdges = new Set();
      
      this.initializeControls();
    }

    /**
     * 初始化控制面板
     */
    initializeControls() {
      const graphView = document.getElementById('graphView');
      if (!graphView) return;

      // 检查是否已存在控制面板
      if (graphView.querySelector('.graph-controls')) return;

      // 创建控制面板
      const controlsHtml = `
        <div class="graph-controls">
          <div class="control-group">
            <button id="fitViewGraphBtn" class="btn btn-secondary btn-sm">适应画布</button>
            <button id="zoomInBtn" class="btn btn-secondary btn-sm">放大</button>
            <button id="zoomOutBtn" class="btn btn-secondary btn-sm">缩小</button>
            <button id="resetGraphBtn" class="btn btn-secondary btn-sm">重置高亮</button>
          </div>
          <div class="control-group">
            <label>
              <input type="checkbox" id="showMinimapCheckbox" checked>
              显示缩略图
            </label>
          </div>
        </div>
        <div id="graphContainer" class="graph-canvas"></div>
      `;

      graphView.innerHTML = controlsHtml;
      this.container = document.getElementById('graphContainer');

      this.bindEvents();
    }

    /**
     * 绑定事件监听器
     */
    bindEvents() {
      const fitViewBtn = document.getElementById('fitViewGraphBtn');
      if (fitViewBtn) {
        fitViewBtn.addEventListener('click', () => this.fitView());
      }

      const zoomInBtn = document.getElementById('zoomInBtn');
      if (zoomInBtn) {
        zoomInBtn.addEventListener('click', () => this.zoomIn());
      }

      const zoomOutBtn = document.getElementById('zoomOutBtn');
      if (zoomOutBtn) {
        zoomOutBtn.addEventListener('click', () => this.zoomOut());
      }

      const resetBtn = document.getElementById('resetGraphBtn');
      if (resetBtn) {
        resetBtn.addEventListener('click', () => this.resetHighlight());
      }

      const minimapCheckbox = document.getElementById('showMinimapCheckbox');
      if (minimapCheckbox) {
        minimapCheckbox.addEventListener('change', (e) => {
          this.toggleMinimap(e.target.checked);
        });
      }
    }

    /**
     * 渲染 DAG 图
     * @param {Object} lineageResult - 血缘分析结果
     */
    render(lineageResult) {
      if (!lineageResult || !window.Utils) {
        console.error('无效的数据或 Utils 未加载');
        return;
      }

      if (!window.G6) {
        console.error('G6 库未加载，请检查 lib/g6.min.js');
        this.renderError('G6 库未加载，DAG 图无法显示');
        return;
      }

      this.currentData = lineageResult;

      // 使用 Utils 转换数据
      const graphData = window.Utils.toGraphData(lineageResult);

      if (!graphData || graphData.nodes.length === 0) {
        this.renderEmpty();
        return;
      }

      // 销毁旧的图实例
      if (this.graph) {
        this.graph.destroy();
        this.graph = null;
      }

      // 创建 G6 图
      this.createGraph(graphData);
    }

    /**
     * 创建 G6 图
     * @param {Object} graphData - 图数据 { nodes, edges }
     */
    createGraph(graphData) {
      if (!this.container) return;

      const width = this.container.offsetWidth || 800;
      const height = this.container.offsetHeight || 600;

      // 创建图实例
      this.graph = new G6.Graph({
        container: this.container,
        width,
        height,
        modes: {
          default: ['drag-canvas', 'zoom-canvas', 'drag-node'],
        },
        layout: {
          type: 'dagre',
          rankdir: 'LR',
          nodesep: 30,
          ranksep: 80,
        },
        defaultNode: {
          type: 'rect',
          size: [80, 40],
          style: {
            fill: '#C6E5FF',
            stroke: '#5B8FF9',
            lineWidth: 2,
            radius: 4,
          },
          labelCfg: {
            style: {
              fill: '#000',
              fontSize: 12,
            },
          },
        },
        defaultEdge: {
          type: 'polyline',
          style: {
            stroke: '#A3B1BF',
            lineWidth: 2,
            endArrow: {
              path: G6.Arrow.triangle(8, 10, 0),
              fill: '#A3B1BF',
            },
          },
          labelCfg: {
            autoRotate: true,
            style: {
              fill: '#666',
              fontSize: 10,
              background: {
                fill: '#fff',
                padding: [2, 4],
                radius: 2,
              },
            },
          },
        },
        plugins: [
          new G6.Minimap({
            size: [150, 100],
            className: 'g6-minimap',
            type: 'default',
          }),
        ],
      });

      // 自定义节点样式
      this.graph.node((node) => {
        const nodeType = node.type || 'field';
        const style = node.style || {};
        
        let shape = 'rect';
        let size = [80, 40];
        
        if (nodeType === 'target') {
          size = [100, 50];
        } else if (nodeType === 'table') {
          size = [90, 45];
        } else if (nodeType === 'field') {
          shape = 'circle';
          size = [40, 40];
        }
        
        return {
          label: node.label,
          type: shape,
          size: size,
          style: {
            fill: style.fill || '#C6E5FF',
            stroke: style.stroke || '#5B8FF9',
            lineWidth: 2,
            radius: shape === 'rect' ? 4 : undefined,
          },
          labelCfg: {
            style: {
              fill: '#fff',
              fontSize: 11,
              fontWeight: nodeType === 'target' ? 'bold' : 'normal',
            },
          },
        };
      });

      // 绑定节点事件
      this.graph.on('node:click', (e) => {
        this.handleNodeClick(e);
      });

      this.graph.on('canvas:click', () => {
        this.resetHighlight();
      });

      // 加载数据
      this.graph.data(graphData);
      this.graph.render();
      this.graph.fitView();
    }

    /**
     * 处理节点点击事件（高亮依赖链路）
     * @param {Object} e - 事件对象
     */
    handleNodeClick(e) {
      const { item } = e;
      const nodeId = item.getID();

      // 重置之前的高亮
      this.resetHighlight();

      // 获取依赖链路
      const { nodes, edges } = this.findDependencyPath(nodeId);

      // 高亮节点和边
      nodes.forEach(id => {
        const node = this.graph.findById(id);
        if (node) {
          this.graph.setItemState(node, 'highlight', true);
          this.highlightedNodes.add(id);
        }
      });

      edges.forEach(id => {
        const edge = this.graph.findById(id);
        if (edge) {
          this.graph.setItemState(edge, 'highlight', true);
          this.highlightedEdges.add(id);
        }
      });

      // 设置高亮样式
      this.graph.getNodes().forEach(node => {
        const id = node.getID();
        if (!this.highlightedNodes.has(id)) {
          this.graph.setItemState(node, 'inactive', true);
        }
      });

      this.graph.getEdges().forEach(edge => {
        const id = edge.getID();
        if (!this.highlightedEdges.has(id)) {
          this.graph.setItemState(edge, 'inactive', true);
        }
      });
    }

    /**
     * 查找依赖路径（BFS）
     * @param {string} nodeId - 节点 ID
     * @returns {Object} { nodes: Set, edges: Set }
     */
    findDependencyPath(nodeId) {
      const nodes = new Set([nodeId]);
      const edges = new Set();
      const queue = [nodeId];
      const visited = new Set([nodeId]);

      while (queue.length > 0) {
        const currentId = queue.shift();
        const node = this.graph.findById(currentId);
        
        if (!node) continue;

        // 查找入边（依赖源）
        const inEdges = node.getInEdges();
        inEdges.forEach(edge => {
          const edgeId = edge.getID();
          const sourceId = edge.getSource().getID();
          
          edges.add(edgeId);
          if (!visited.has(sourceId)) {
            nodes.add(sourceId);
            queue.push(sourceId);
            visited.add(sourceId);
          }
        });

        // 查找出边（被依赖）
        const outEdges = node.getOutEdges();
        outEdges.forEach(edge => {
          const edgeId = edge.getID();
          const targetId = edge.getTarget().getID();
          
          edges.add(edgeId);
          if (!visited.has(targetId)) {
            nodes.add(targetId);
            queue.push(targetId);
            visited.add(targetId);
          }
        });
      }

      return { nodes, edges };
    }

    /**
     * 重置高亮
     */
    resetHighlight() {
      if (!this.graph) return;

      this.graph.getNodes().forEach(node => {
        this.graph.clearItemStates(node);
      });

      this.graph.getEdges().forEach(edge => {
        this.graph.clearItemStates(edge);
      });

      this.highlightedNodes.clear();
      this.highlightedEdges.clear();
    }

    /**
     * 适应画布
     */
    fitView() {
      if (this.graph) {
        this.graph.fitView();
      }
    }

    /**
     * 放大
     */
    zoomIn() {
      if (this.graph) {
        const zoom = this.graph.getZoom();
        this.graph.zoomTo(zoom * 1.2);
      }
    }

    /**
     * 缩小
     */
    zoomOut() {
      if (this.graph) {
        const zoom = this.graph.getZoom();
        this.graph.zoomTo(zoom / 1.2);
      }
    }

    /**
     * 切换缩略图显示
     * @param {boolean} show - 是否显示
     */
    toggleMinimap(show) {
      if (!this.graph) return;

      const minimap = this.container.querySelector('.g6-minimap');
      if (minimap) {
        minimap.style.display = show ? 'block' : 'none';
      }
    }

    /**
     * 渲染空状态
     */
    renderEmpty() {
      if (!this.container) return;

      this.container.innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #999;">
          <div style="font-size: 48px; margin-bottom: 16px;">📊</div>
          <div style="font-size: 16px;">暂无图数据</div>
          <div style="font-size: 14px; margin-top: 8px;">请重新分析 SQL</div>
        </div>
      `;
    }

    /**
     * 渲染错误信息
     * @param {string} message - 错误信息
     */
    renderError(message) {
      if (!this.container) return;

      this.container.innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #ff4d4f;">
          <div style="font-size: 48px; margin-bottom: 16px;">⚠️</div>
          <div style="font-size: 16px;">${window.Utils.escapeHtml(message)}</div>
          <div style="font-size: 14px; margin-top: 8px;">请检查库文件是否正确加载</div>
        </div>
      `;
    }

    /**
     * 清空图
     */
    clear() {
      if (this.graph) {
        this.graph.destroy();
        this.graph = null;
      }
      
      this.currentData = null;
      this.highlightedNodes.clear();
      this.highlightedEdges.clear();

      if (this.container) {
        this.container.innerHTML = '';
      }
    }
  }

  // 注册高亮状态样式
  if (window.G6) {
    G6.registerNode('rect', {
      setState(name, value, item) {
        const group = item.getContainer();
        const shape = group.get('children')[0];
        
        if (name === 'highlight') {
          if (value) {
            shape.attr('stroke', '#FF6B00');
            shape.attr('lineWidth', 3);
          } else {
            shape.attr('stroke', '#5B8FF9');
            shape.attr('lineWidth', 2);
          }
        }
        
        if (name === 'inactive') {
          if (value) {
            shape.attr('opacity', 0.3);
          } else {
            shape.attr('opacity', 1);
          }
        }
      },
    }, 'single-node');

    G6.registerEdge('polyline', {
      setState(name, value, item) {
        const group = item.getContainer();
        const shape = group.get('children')[0];
        
        if (name === 'highlight') {
          if (value) {
            shape.attr('stroke', '#FF6B00');
            shape.attr('lineWidth', 3);
          } else {
            shape.attr('stroke', '#A3B1BF');
            shape.attr('lineWidth', 2);
          }
        }
        
        if (name === 'inactive') {
          if (value) {
            shape.attr('opacity', 0.2);
          } else {
            shape.attr('opacity', 1);
          }
        }
      },
    }, 'single-edge');
  }

  // 创建全局实例
  const visualizer = new LineageVisualizer();

  // 暴露到全局
  window.LineageVisualizer = visualizer;

})();
