// SQL字段级血缘分析平台 - 树形视图

(function() {
  'use strict';

  /**
   * 树形视图类
   * @class TreeView
   */
  class TreeView {
    constructor() {
      this.container = document.getElementById('treeContainer');
      this.currentData = null;
      this.graph = null;
      this.selectedTarget = null;
      
      this.initializeControls();
    }

    /**
     * 初始化控制面板
     */
    initializeControls() {
      const treeView = document.getElementById('treeView');
      if (!treeView) return;

      // 检查是否已存在控制面板
      if (treeView.querySelector('.tree-controls')) return;

      // 创建控制面板
      const controlsHtml = `
        <div class="tree-controls">
          <div class="control-group">
            <label for="targetFieldSelect">选择目标字段：</label>
            <select id="targetFieldSelect" class="form-control">
              <option value="">-- 请先分析 SQL --</option>
            </select>
          </div>
          <div class="control-group">
            <button id="expandAllBtn" class="btn btn-secondary btn-sm">全部展开</button>
            <button id="collapseAllBtn" class="btn btn-secondary btn-sm">全部折叠</button>
            <button id="fitViewBtn" class="btn btn-secondary btn-sm">适应画布</button>
          </div>
        </div>
        <div id="treeContainer" class="tree-canvas"></div>
      `;

      treeView.innerHTML = controlsHtml;
      this.container = document.getElementById('treeContainer');

      this.bindEvents();
    }

    /**
     * 绑定事件监听器
     */
    bindEvents() {
      const targetSelect = document.getElementById('targetFieldSelect');
      if (targetSelect) {
        targetSelect.addEventListener('change', (e) => {
          this.selectedTarget = e.target.value;
          this.renderTree();
        });
      }

      const expandAllBtn = document.getElementById('expandAllBtn');
      if (expandAllBtn) {
        expandAllBtn.addEventListener('click', () => this.expandAll());
      }

      const collapseAllBtn = document.getElementById('collapseAllBtn');
      if (collapseAllBtn) {
        collapseAllBtn.addEventListener('click', () => this.collapseAll());
      }

      const fitViewBtn = document.getElementById('fitViewBtn');
      if (fitViewBtn) {
        fitViewBtn.addEventListener('click', () => this.fitView());
      }
    }

    /**
     * 渲染树形视图
     * @param {Object} lineageResult - 血缘分析结果
     */
    render(lineageResult) {
      if (!lineageResult || !window.Utils) {
        console.error('无效的数据或 Utils 未加载');
        return;
      }

      if (!window.G6) {
        console.error('G6 库未加载，请检查 lib/g6.min.js');
        this.renderError('G6 库未加载，树形视图无法显示');
        return;
      }

      this.currentData = lineageResult;

      // 更新目标字段下拉菜单
      this.updateTargetFieldSelect();

      // 如果有选中的目标字段，渲染树形图
      if (this.selectedTarget) {
        this.renderTree();
      }
    }

    /**
     * 更新目标字段下拉菜单
     */
    updateTargetFieldSelect() {
      const targetSelect = document.getElementById('targetFieldSelect');
      if (!targetSelect || !this.currentData) return;

      // 获取所有唯一的目标字段
      const targetFields = new Set();
      if (this.currentData.fieldDependencies) {
        this.currentData.fieldDependencies.forEach(dep => {
          const targetName = dep.targetAlias || dep.targetField;
          if (targetName) {
            targetFields.add(targetName);
          }
        });
      }

      // 更新下拉菜单
      targetSelect.innerHTML = '<option value="">-- 选择目标字段 --</option>';
      targetFields.forEach(field => {
        const option = document.createElement('option');
        option.value = field;
        option.textContent = field;
        targetSelect.appendChild(option);
      });

      // 默认选择第一个
      if (targetFields.size > 0 && !this.selectedTarget) {
        this.selectedTarget = Array.from(targetFields)[0];
        targetSelect.value = this.selectedTarget;
      }
    }

    /**
     * 渲染树形图
     */
    renderTree() {
      if (!this.selectedTarget || !this.currentData) {
        this.renderEmpty();
        return;
      }

      // 将 LineageResult 转换为树形数据
      const treeData = this.buildTreeData(this.selectedTarget);

      if (!treeData || !treeData.children || treeData.children.length === 0) {
        this.renderEmpty();
        return;
      }

      // 销毁旧的图实例
      if (this.graph) {
        this.graph.destroy();
        this.graph = null;
      }

      // 创建 G6 树形图
      this.createGraph(treeData);
    }

    /**
     * 构建树形数据（从目标字段到源字段）
     * @param {string} targetField - 目标字段名
     * @returns {Object} 树形数据
     */
    buildTreeData(targetField) {
      if (!this.currentData || !this.currentData.fieldDependencies) {
        return null;
      }

      const deps = this.currentData.fieldDependencies.filter(dep => {
        const name = dep.targetAlias || dep.targetField;
        return name === targetField;
      });

      if (deps.length === 0) {
        return null;
      }

      // 根节点
      const root = {
        id: `target_${targetField}`,
        label: targetField,
        type: 'target',
        children: []
      };

      // 构建第二层（源表）
      deps.forEach((dep, index) => {
        const tableName = window.Utils.formatTableName(dep.sourceTable, dep.sourceTableAlias);
        const tableNode = {
          id: `table_${index}_${tableName}`,
          label: tableName,
          type: 'table',
          children: []
        };

        // 构建第三层（源字段）
        if (dep.sourceFields && dep.sourceFields.length > 0) {
          dep.sourceFields.forEach((field, fIndex) => {
            const fieldNode = {
              id: `field_${index}_${fIndex}_${field}`,
              label: field,
              type: 'field',
              expression: dep.expression || null,
              isAggregation: dep.isAggregation
            };
            tableNode.children.push(fieldNode);
          });
        } else {
          // 没有源字段，添加占位节点
          tableNode.children.push({
            id: `field_${index}_0_direct`,
            label: '直接引用',
            type: 'field'
          });
        }

        root.children.push(tableNode);
      });

      return root;
    }

    /**
     * 创建 G6 树形图
     * @param {Object} treeData - 树形数据
     */
    createGraph(treeData) {
      if (!this.container) return;

      const width = this.container.offsetWidth || 800;
      const height = this.container.offsetHeight || 600;

      // 定义节点样式
      const nodeStyles = {
        target: {
          fill: '#5B8FF9',
          stroke: '#2F54EB',
          size: [120, 40]
        },
        table: {
          fill: '#5AD8A6',
          stroke: '#237804',
          size: [100, 40]
        },
        field: {
          fill: '#F6BD16',
          stroke: '#D48806',
          size: [80, 30]
        }
      };

      // 创建图实例
      this.graph = new G6.TreeGraph({
        container: this.container,
        width,
        height,
        modes: {
          default: [
            {
              type: 'collapse-expand',
              onChange: function(item, collapsed) {
                const data = item.getModel();
                data.collapsed = collapsed;
                return true;
              },
            },
            'drag-canvas',
            'zoom-canvas',
          ],
        },
        defaultNode: {
          type: 'rect',
          size: [100, 40],
          style: {
            fill: '#C6E5FF',
            stroke: '#5B8FF9',
            lineWidth: 2,
            radius: 4
          },
          labelCfg: {
            style: {
              fill: '#000',
              fontSize: 12,
            },
          },
        },
        defaultEdge: {
          type: 'cubic-vertical',
          style: {
            stroke: '#A3B1BF',
            lineWidth: 2,
          },
        },
        layout: {
          type: 'compactBox',
          direction: 'TB',
          getId: function getId(d) {
            return d.id;
          },
          getHeight: function getHeight() {
            return 16;
          },
          getWidth: function getWidth() {
            return 16;
          },
          getVGap: function getVGap() {
            return 40;
          },
          getHGap: function getHGap() {
            return 70;
          },
        },
      });

      // 自定义节点样式
      this.graph.node((node) => {
        const nodeType = node.type || 'field';
        const style = nodeStyles[nodeType] || nodeStyles.field;
        
        return {
          label: node.label,
          size: style.size,
          style: {
            fill: style.fill,
            stroke: style.stroke,
            lineWidth: 2,
            radius: 4
          },
          labelCfg: {
            style: {
              fill: '#fff',
              fontSize: 12,
              fontWeight: nodeType === 'target' ? 'bold' : 'normal'
            },
          },
        };
      });

      // 自定义边样式
      this.graph.edge((edge) => {
        return {
          style: {
            stroke: '#A3B1BF',
            lineWidth: 2,
          },
        };
      });

      // 绑定节点事件
      this.graph.on('node:click', (e) => {
        const { item } = e;
        const model = item.getModel();
        
        if (model.expression) {
          console.log('转换逻辑:', model.expression);
          // 可以在这里显示 tooltip
        }
      });

      // 加载数据
      this.graph.data(treeData);
      this.graph.render();
      this.graph.fitView();
    }

    /**
     * 展开所有节点
     */
    expandAll() {
      if (!this.graph) return;
      
      const nodes = this.graph.getNodes();
      nodes.forEach(node => {
        const model = node.getModel();
        if (model.collapsed) {
          this.graph.expandNode(node);
        }
      });
    }

    /**
     * 折叠所有节点
     */
    collapseAll() {
      if (!this.graph) return;
      
      const nodes = this.graph.getNodes();
      nodes.forEach(node => {
        const model = node.getModel();
        if (!model.collapsed && node.getChildren().length > 0) {
          this.graph.collapseNode(node);
        }
      });
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
     * 渲染空状态
     */
    renderEmpty() {
      if (!this.container) return;

      this.container.innerHTML = `
        <div style="display: flex; flex-direction: column; align-items: center; justify-content: center; height: 100%; color: #999;">
          <div style="font-size: 48px; margin-bottom: 16px;">🌳</div>
          <div style="font-size: 16px;">暂无树形数据</div>
          <div style="font-size: 14px; margin-top: 8px;">请选择目标字段或重新分析</div>
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
     * 清空树形图
     */
    clear() {
      if (this.graph) {
        this.graph.destroy();
        this.graph = null;
      }
      
      this.currentData = null;
      this.selectedTarget = null;
      
      const targetSelect = document.getElementById('targetFieldSelect');
      if (targetSelect) {
        targetSelect.innerHTML = '<option value="">-- 请先分析 SQL --</option>';
      }

      if (this.container) {
        this.container.innerHTML = '';
      }
    }
  }

  // 创建全局实例
  const treeView = new TreeView();

  // 暴露到全局
  window.TreeView = treeView;

})();
