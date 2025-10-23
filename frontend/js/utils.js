// SQL字段级血缘分析平台 - 工具函数库

(function() {
  'use strict';

  /**
   * 工具函数库
   * @namespace Utils
   */
  const Utils = {

    // ============ 数据格式转换函数 ============

    /**
     * 将 LineageResult 转换为表格数据
     * @param {Object} lineageResult - 血缘分析结果
     * @param {string} lineageResult.sql - 原始SQL
     * @param {string} lineageResult.dbType - 数据库类型
     * @param {Array<string>} lineageResult.tables - 表列表
     * @param {Array<Object>} lineageResult.fieldDependencies - 字段依赖列表
     * @param {boolean} lineageResult.success - 是否成功
     * @param {string} lineageResult.errorMessage - 错误信息
     * @returns {Array<Object>} 表格行数据数组
     */
    toTableData(lineageResult) {
      if (!lineageResult || !lineageResult.success) {
        console.error('无效的血缘分析结果:', lineageResult);
        return [];
      }

      const tableData = [];
      const { fieldDependencies } = lineageResult;

      if (!fieldDependencies || fieldDependencies.length === 0) {
        return [];
      }

      fieldDependencies.forEach((dep, index) => {
        const targetField = dep.targetAlias || dep.targetField || '未知字段';
        const sourceTable = this.formatTableName(dep.sourceTable, dep.sourceTableAlias);
        const sourceFields = dep.sourceFields && dep.sourceFields.length > 0 
          ? dep.sourceFields.join(', ') 
          : '直接引用';
        
        let transformation = '';
        if (dep.expression) {
          transformation = dep.expression;
        } else if (dep.isAggregation) {
          transformation = '聚合函数';
        } else {
          transformation = '直接映射';
        }

        const dependencyLevel = this.calculateDependencyLevel(dep, fieldDependencies);

        tableData.push({
          id: index + 1,
          targetField,
          sourceTable,
          sourceFields,
          transformation,
          dependencyLevel,
          raw: dep
        });
      });

      return tableData;
    },

    /**
     * 将 LineageResult 转换为树形数据
     * @param {Object} lineageResult - 血缘分析结果
     * @returns {Object} 树形结构数据
     */
    toTreeData(lineageResult) {
      if (!lineageResult || !lineageResult.success) {
        console.error('无效的血缘分析结果:', lineageResult);
        return null;
      }

      const { fieldDependencies, tables } = lineageResult;

      if (!fieldDependencies || fieldDependencies.length === 0) {
        return null;
      }

      const root = {
        name: 'SQL查询结果',
        type: 'root',
        children: []
      };

      // 按目标字段分组
      const targetFieldMap = new Map();
      fieldDependencies.forEach(dep => {
        const targetName = dep.targetAlias || dep.targetField;
        if (!targetFieldMap.has(targetName)) {
          targetFieldMap.set(targetName, []);
        }
        targetFieldMap.get(targetName).push(dep);
      });

      // 构建树形结构
      targetFieldMap.forEach((deps, targetName) => {
        const targetNode = {
          name: targetName,
          type: 'target',
          children: []
        };

        deps.forEach(dep => {
          const sourceNode = {
            name: this.formatTableName(dep.sourceTable, dep.sourceTableAlias),
            type: 'table',
            children: []
          };

          if (dep.sourceFields && dep.sourceFields.length > 0) {
            dep.sourceFields.forEach(field => {
              sourceNode.children.push({
                name: field,
                type: 'field',
                expression: dep.expression || null,
                isAggregation: dep.isAggregation
              });
            });
          }

          targetNode.children.push(sourceNode);
        });

        root.children.push(targetNode);
      });

      return root;
    },

    /**
     * 将 LineageResult 转换为图数据（用于 DAG 可视化）
     * @param {Object} lineageResult - 血缘分析结果
     * @returns {Object} 图数据 { nodes: [], edges: [] }
     */
    toGraphData(lineageResult) {
      if (!lineageResult || !lineageResult.success) {
        console.error('无效的血缘分析结果:', lineageResult);
        return { nodes: [], edges: [] };
      }

      const { fieldDependencies, tables } = lineageResult;

      if (!fieldDependencies || fieldDependencies.length === 0) {
        return { nodes: [], edges: [] };
      }

      const nodes = [];
      const edges = [];
      const nodeMap = new Map();

      // 生成唯一节点 ID
      const getNodeId = (type, name) => `${type}_${name}`;

      // 添加节点（避免重复）
      const addNode = (id, label, type) => {
        if (!nodeMap.has(id)) {
          nodes.push({
            id,
            label,
            type,
            style: this.getNodeStyle(type)
          });
          nodeMap.set(id, true);
        }
      };

      // 遍历字段依赖，构建节点和边
      fieldDependencies.forEach((dep, index) => {
        const targetName = dep.targetAlias || dep.targetField;
        const targetId = getNodeId('target', targetName);
        addNode(targetId, targetName, 'target');

        if (dep.sourceTable) {
          const tableName = this.formatTableName(dep.sourceTable, dep.sourceTableAlias);
          const tableId = getNodeId('table', tableName);
          addNode(tableId, tableName, 'table');

          if (dep.sourceFields && dep.sourceFields.length > 0) {
            dep.sourceFields.forEach(field => {
              const fieldId = getNodeId('field', `${tableName}.${field}`);
              addNode(fieldId, field, 'field');

              // 添加边：source field -> table
              edges.push({
                id: `edge_${fieldId}_${tableId}`,
                source: fieldId,
                target: tableId,
                label: dep.isAggregation ? '聚合' : ''
              });
            });
          }

          // 添加边：table -> target field
          edges.push({
            id: `edge_${tableId}_${targetId}`,
            source: tableId,
            target: targetId,
            label: dep.expression || ''
          });
        }
      });

      return { nodes, edges };
    },

    // ============ 辅助函数 ============

    /**
     * 格式化表名（处理表别名）
     * @param {string} table - 表名
     * @param {string} alias - 表别名
     * @returns {string} 格式化后的表名
     */
    formatTableName(table, alias) {
      if (!table) return '未知表';
      if (alias && alias !== table) {
        return `${table} (${alias})`;
      }
      return table;
    },

    /**
     * 计算依赖层级（简化版）
     * @param {Object} dep - 字段依赖
     * @param {Array<Object>} allDeps - 所有字段依赖
     * @returns {number} 依赖层级
     */
    calculateDependencyLevel(dep, allDeps) {
      // 简化实现：如果有表达式或聚合，层级为 2，否则为 1
      if (dep.expression || dep.isAggregation) {
        return 2;
      }
      return 1;
    },

    /**
     * 获取节点样式（用于图可视化）
     * @param {string} type - 节点类型
     * @returns {Object} 样式对象
     */
    getNodeStyle(type) {
      const styles = {
        target: {
          fill: '#5B8FF9',
          stroke: '#2F54EB',
          size: 60
        },
        table: {
          fill: '#5AD8A6',
          stroke: '#237804',
          size: 50
        },
        field: {
          fill: '#F6BD16',
          stroke: '#D48806',
          size: 40
        }
      };
      return styles[type] || styles.field;
    },

    // ============ 通用工具函数 ============

    /**
     * 格式化日期时间
     * @param {Date|string|number} date - 日期对象、时间戳或日期字符串
     * @param {string} format - 格式字符串，默认 'YYYY-MM-DD HH:mm:ss'
     * @returns {string} 格式化后的日期字符串
     */
    formatDate(date, format = 'YYYY-MM-DD HH:mm:ss') {
      const d = new Date(date);
      if (isNaN(d.getTime())) {
        return '无效日期';
      }

      const pad = (num) => String(num).padStart(2, '0');

      const tokens = {
        YYYY: d.getFullYear(),
        MM: pad(d.getMonth() + 1),
        DD: pad(d.getDate()),
        HH: pad(d.getHours()),
        mm: pad(d.getMinutes()),
        ss: pad(d.getSeconds())
      };

      return format.replace(/YYYY|MM|DD|HH|mm|ss/g, match => tokens[match]);
    },

    /**
     * 截断字符串
     * @param {string} str - 原始字符串
     * @param {number} maxLength - 最大长度
     * @param {string} suffix - 后缀，默认 '...'
     * @returns {string} 截断后的字符串
     */
    truncate(str, maxLength, suffix = '...') {
      if (!str || str.length <= maxLength) {
        return str || '';
      }
      return str.substring(0, maxLength - suffix.length) + suffix;
    },

    /**
     * 转义 HTML 特殊字符
     * @param {string} str - 原始字符串
     * @returns {string} 转义后的字符串
     */
    escapeHtml(str) {
      if (!str) return '';
      const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#39;'
      };
      return str.replace(/[&<>"']/g, m => map[m]);
    },

    /**
     * 深拷贝对象
     * @param {*} obj - 原始对象
     * @returns {*} 深拷贝后的对象
     */
    deepClone(obj) {
      if (obj === null || typeof obj !== 'object') {
        return obj;
      }

      if (obj instanceof Date) {
        return new Date(obj.getTime());
      }

      if (obj instanceof Array) {
        return obj.map(item => this.deepClone(item));
      }

      const clonedObj = {};
      for (const key in obj) {
        if (obj.hasOwnProperty(key)) {
          clonedObj[key] = this.deepClone(obj[key]);
        }
      }
      return clonedObj;
    },

    /**
     * 防抖函数
     * @param {Function} func - 要防抖的函数
     * @param {number} wait - 等待时间（毫秒）
     * @returns {Function} 防抖后的函数
     */
    debounce(func, wait) {
      let timeout;
      return function executedFunction(...args) {
        const later = () => {
          clearTimeout(timeout);
          func(...args);
        };
        clearTimeout(timeout);
        timeout = setTimeout(later, wait);
      };
    },

    /**
     * 节流函数
     * @param {Function} func - 要节流的函数
     * @param {number} limit - 时间限制（毫秒）
     * @returns {Function} 节流后的函数
     */
    throttle(func, limit) {
      let inThrottle;
      return function executedFunction(...args) {
        if (!inThrottle) {
          func(...args);
          inThrottle = true;
          setTimeout(() => inThrottle = false, limit);
        }
      };
    },

    // ============ 错误处理 ============

    /**
     * 包装异步函数，统一错误处理
     * @param {Function} asyncFunc - 异步函数
     * @returns {Promise} 包装后的 Promise
     */
    async wrapAsync(asyncFunc) {
      try {
        return await asyncFunc();
      } catch (error) {
        this.logError(error);
        throw error;
      }
    },

    /**
     * 记录错误日志
     * @param {Error|string} error - 错误对象或错误信息
     * @param {Object} context - 上下文信息
     */
    logError(error, context = {}) {
      const timestamp = this.formatDate(new Date());
      const errorMsg = error instanceof Error ? error.message : String(error);
      const stack = error instanceof Error ? error.stack : '';

      console.error('[ERROR]', timestamp, errorMsg, context);
      if (stack) {
        console.error('Stack:', stack);
      }

      // 可以在这里添加错误上报逻辑
    },

    /**
     * 显示友好的错误信息
     * @param {Error|string} error - 错误对象或错误信息
     * @returns {string} 友好的错误信息
     */
    getFriendlyErrorMessage(error) {
      const errorMsg = error instanceof Error ? error.message : String(error);

      const errorMappings = {
        'Network Error': '网络连接失败，请检查网络设置',
        'timeout': '请求超时，请稍后重试',
        '401': '未授权，请先登录',
        '403': '无权限访问',
        '404': '请求的资源不存在',
        '500': '服务器内部错误，请联系管理员'
      };

      for (const [key, message] of Object.entries(errorMappings)) {
        if (errorMsg.includes(key)) {
          return message;
        }
      }

      return '操作失败，请稍后重试';
    },

    // ============ 验证函数 ============

    /**
     * 验证 LineageResult 数据完整性
     * @param {Object} lineageResult - 血缘分析结果
     * @returns {boolean} 是否有效
     */
    validateLineageResult(lineageResult) {
      if (!lineageResult) {
        console.warn('LineageResult 为空');
        return false;
      }

      if (!lineageResult.success) {
        console.warn('LineageResult 标记为失败:', lineageResult.errorMessage);
        return false;
      }

      if (!lineageResult.fieldDependencies || !Array.isArray(lineageResult.fieldDependencies)) {
        console.warn('fieldDependencies 无效');
        return false;
      }

      return true;
    }
  };

  // 暴露到全局
  window.Utils = Utils;

})();
