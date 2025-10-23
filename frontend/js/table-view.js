// SQL字段级血缘分析平台 - 表格视图

(function() {
  'use strict';

  /**
   * 表格视图类
   * @class TableView
   */
  class TableView {
    constructor() {
      this.currentData = [];
      this.filteredData = [];
      this.currentPage = 1;
      this.pageSize = 50;
      this.sortColumn = null;
      this.sortDirection = 'asc';
      
      this.tableElement = document.getElementById('lineageTable');
      this.tableBody = this.tableElement ? this.tableElement.querySelector('tbody') : null;
      
      this.initializeControls();
      this.bindEvents();
    }

    /**
     * 初始化控制面板（搜索、分页等）
     */
    initializeControls() {
      const tableView = document.getElementById('tableView');
      if (!tableView) return;

      // 检查是否已存在控制面板
      if (tableView.querySelector('.table-controls')) return;

      // 创建控制面板
      const controlsHtml = `
        <div class="table-controls">
          <div class="search-box">
            <input type="text" id="tableSearch" class="form-control" placeholder="搜索字段名或表名...">
            <button id="clearSearch" class="btn btn-secondary btn-sm">清除</button>
          </div>
          <div class="table-stats">
            <span id="tableStats">共 0 条记录</span>
          </div>
        </div>
        <div class="table-wrapper"></div>
        <div class="pagination-controls">
          <button id="firstPage" class="btn btn-secondary btn-sm">首页</button>
          <button id="prevPage" class="btn btn-secondary btn-sm">上一页</button>
          <span id="pageInfo" class="page-info">第 1 页 / 共 1 页</span>
          <button id="nextPage" class="btn btn-secondary btn-sm">下一页</button>
          <button id="lastPage" class="btn btn-secondary btn-sm">末页</button>
          <select id="pageSize" class="form-control" style="width: auto; display: inline-block;">
            <option value="20">20条/页</option>
            <option value="50" selected>50条/页</option>
            <option value="100">100条/页</option>
          </select>
        </div>
      `;

      // 将表格移动到 wrapper 中
      const table = tableView.querySelector('#lineageTable');
      tableView.insertAdjacentHTML('afterbegin', controlsHtml);
      const wrapper = tableView.querySelector('.table-wrapper');
      if (table && wrapper) {
        wrapper.appendChild(table);
      }
    }

    /**
     * 绑定事件监听器
     */
    bindEvents() {
      // 排序事件
      if (this.tableElement) {
        const headers = this.tableElement.querySelectorAll('thead th');
        headers.forEach((header, index) => {
          header.style.cursor = 'pointer';
          header.addEventListener('click', () => this.handleSort(index));
        });
      }

      // 搜索事件
      const searchInput = document.getElementById('tableSearch');
      if (searchInput) {
        searchInput.addEventListener('input', window.Utils.debounce(() => {
          this.handleSearch(searchInput.value);
        }, 300));
      }

      const clearSearchBtn = document.getElementById('clearSearch');
      if (clearSearchBtn) {
        clearSearchBtn.addEventListener('click', () => {
          const searchInput = document.getElementById('tableSearch');
          if (searchInput) {
            searchInput.value = '';
            this.handleSearch('');
          }
        });
      }

      // 分页事件
      const firstPageBtn = document.getElementById('firstPage');
      const prevPageBtn = document.getElementById('prevPage');
      const nextPageBtn = document.getElementById('nextPage');
      const lastPageBtn = document.getElementById('lastPage');
      const pageSizeSelect = document.getElementById('pageSize');

      if (firstPageBtn) firstPageBtn.addEventListener('click', () => this.goToPage(1));
      if (prevPageBtn) prevPageBtn.addEventListener('click', () => this.goToPage(this.currentPage - 1));
      if (nextPageBtn) nextPageBtn.addEventListener('click', () => this.goToPage(this.currentPage + 1));
      if (lastPageBtn) lastPageBtn.addEventListener('click', () => this.goToPage(this.getTotalPages()));
      if (pageSizeSelect) {
        pageSizeSelect.addEventListener('change', (e) => {
          this.pageSize = parseInt(e.target.value);
          this.currentPage = 1;
          this.renderTable();
        });
      }
    }

    /**
     * 渲染表格
     * @param {Object} lineageResult - 血缘分析结果
     */
    render(lineageResult) {
      if (!lineageResult || !window.Utils) {
        console.error('无效的数据或 Utils 未加载');
        return;
      }

      // 使用 Utils 转换数据
      const tableData = window.Utils.toTableData(lineageResult);
      this.currentData = tableData;
      this.filteredData = [...tableData];
      this.currentPage = 1;
      this.sortColumn = null;
      this.sortDirection = 'asc';

      // 清空搜索框
      const searchInput = document.getElementById('tableSearch');
      if (searchInput) searchInput.value = '';

      this.renderTable();
    }

    /**
     * 渲染表格内容
     */
    renderTable() {
      if (!this.tableBody) {
        console.error('表格 tbody 元素未找到');
        return;
      }

      // 清空表格
      this.tableBody.innerHTML = '';

      // 空数据提示
      if (this.filteredData.length === 0) {
        this.renderEmptyState();
        this.updateStats();
        return;
      }

      // 计算分页
      const startIndex = (this.currentPage - 1) * this.pageSize;
      const endIndex = Math.min(startIndex + this.pageSize, this.filteredData.length);
      const pageData = this.filteredData.slice(startIndex, endIndex);

      // 渲染表格行
      pageData.forEach((row, index) => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${window.Utils.escapeHtml(row.targetField)}</td>
          <td>${window.Utils.escapeHtml(row.sourceTable)}</td>
          <td>${window.Utils.escapeHtml(row.sourceFields)}</td>
          <td>${window.Utils.escapeHtml(row.transformation)}</td>
          <td>${row.dependencyLevel}</td>
        `;
        
        // 添加行点击高亮效果
        tr.addEventListener('click', () => {
          this.tableBody.querySelectorAll('tr').forEach(r => r.classList.remove('active'));
          tr.classList.add('active');
        });

        this.tableBody.appendChild(tr);
      });

      this.updateStats();
      this.updatePagination();
    }

    /**
     * 渲染空数据状态
     */
    renderEmptyState() {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td colspan="5" style="text-align: center; padding: 40px; color: #999;">
          <div style="font-size: 48px; margin-bottom: 16px;">📭</div>
          <div style="font-size: 16px;">暂无数据</div>
          <div style="font-size: 14px; margin-top: 8px;">请尝试调整搜索条件或重新分析</div>
        </td>
      `;
      this.tableBody.appendChild(tr);

      // 隐藏分页控件
      const paginationControls = document.querySelector('.pagination-controls');
      if (paginationControls) paginationControls.style.display = 'none';
    }

    /**
     * 处理排序
     * @param {number} columnIndex - 列索引
     */
    handleSort(columnIndex) {
      const columns = ['targetField', 'sourceTable', 'sourceFields', 'transformation', 'dependencyLevel'];
      const column = columns[columnIndex];

      if (this.sortColumn === column) {
        this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
      } else {
        this.sortColumn = column;
        this.sortDirection = 'asc';
      }

      this.filteredData.sort((a, b) => {
        let aVal = a[column];
        let bVal = b[column];

        // 处理数字类型
        if (column === 'dependencyLevel') {
          aVal = parseInt(aVal) || 0;
          bVal = parseInt(bVal) || 0;
        } else {
          aVal = String(aVal).toLowerCase();
          bVal = String(bVal).toLowerCase();
        }

        if (this.sortDirection === 'asc') {
          return aVal > bVal ? 1 : aVal < bVal ? -1 : 0;
        } else {
          return aVal < bVal ? 1 : aVal > bVal ? -1 : 0;
        }
      });

      // 更新表头排序指示器
      this.updateSortIndicators(columnIndex);

      this.currentPage = 1;
      this.renderTable();
    }

    /**
     * 更新排序指示器
     * @param {number} activeColumnIndex - 当前排序列索引
     */
    updateSortIndicators(activeColumnIndex) {
      if (!this.tableElement) return;

      const headers = this.tableElement.querySelectorAll('thead th');
      headers.forEach((header, index) => {
        header.classList.remove('sort-asc', 'sort-desc');
        
        if (index === activeColumnIndex) {
          header.classList.add(this.sortDirection === 'asc' ? 'sort-asc' : 'sort-desc');
        }
      });
    }

    /**
     * 处理搜索
     * @param {string} keyword - 搜索关键词
     */
    handleSearch(keyword) {
      if (!keyword || keyword.trim() === '') {
        this.filteredData = [...this.currentData];
      } else {
        const lowerKeyword = keyword.toLowerCase();
        this.filteredData = this.currentData.filter(row => {
          return (
            row.targetField.toLowerCase().includes(lowerKeyword) ||
            row.sourceTable.toLowerCase().includes(lowerKeyword) ||
            row.sourceFields.toLowerCase().includes(lowerKeyword)
          );
        });
      }

      this.currentPage = 1;
      this.renderTable();
    }

    /**
     * 跳转到指定页
     * @param {number} page - 页码
     */
    goToPage(page) {
      const totalPages = this.getTotalPages();
      if (page < 1 || page > totalPages) return;

      this.currentPage = page;
      this.renderTable();
    }

    /**
     * 获取总页数
     * @returns {number} 总页数
     */
    getTotalPages() {
      return Math.ceil(this.filteredData.length / this.pageSize);
    }

    /**
     * 更新统计信息
     */
    updateStats() {
      const statsElement = document.getElementById('tableStats');
      if (statsElement) {
        const total = this.currentData.length;
        const filtered = this.filteredData.length;
        
        if (total === filtered) {
          statsElement.textContent = `共 ${total} 条记录`;
        } else {
          statsElement.textContent = `共 ${total} 条记录，筛选后 ${filtered} 条`;
        }
      }
    }

    /**
     * 更新分页信息
     */
    updatePagination() {
      const totalPages = this.getTotalPages();
      const pageInfoElement = document.getElementById('pageInfo');
      const paginationControls = document.querySelector('.pagination-controls');

      if (paginationControls) {
        paginationControls.style.display = totalPages > 1 ? 'flex' : 'none';
      }

      if (pageInfoElement) {
        pageInfoElement.textContent = `第 ${this.currentPage} 页 / 共 ${totalPages} 页`;
      }

      // 更新按钮状态
      const firstPageBtn = document.getElementById('firstPage');
      const prevPageBtn = document.getElementById('prevPage');
      const nextPageBtn = document.getElementById('nextPage');
      const lastPageBtn = document.getElementById('lastPage');

      if (firstPageBtn) firstPageBtn.disabled = this.currentPage === 1;
      if (prevPageBtn) prevPageBtn.disabled = this.currentPage === 1;
      if (nextPageBtn) nextPageBtn.disabled = this.currentPage === totalPages;
      if (lastPageBtn) lastPageBtn.disabled = this.currentPage === totalPages;
    }

    /**
     * 清空表格
     */
    clear() {
      if (this.tableBody) {
        this.tableBody.innerHTML = '';
      }
      this.currentData = [];
      this.filteredData = [];
      this.currentPage = 1;
      this.updateStats();
    }
  }

  // 创建全局实例
  const tableView = new TableView();

  // 暴露到全局
  window.TableView = tableView;

})();
