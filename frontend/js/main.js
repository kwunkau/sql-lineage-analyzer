// SQL字段级血缘分析平台 - 主应用脚本

(function() {
  'use strict';

  // 保存最近的分析结果
  let currentLineageResult = null;

  // ============ 应用初始化 ============
  document.addEventListener('DOMContentLoaded', function() {
    initializeApp();
    bindEvents();
  });

  /**
   * 初始化应用
   */
  function initializeApp() {
    console.log('SQL血缘分析平台已加载');
    
    // 检查必要的依赖库
    if (typeof axios === 'undefined') {
      console.error('Axios库未加载，请检查lib目录');
      showMessage('依赖库加载失败', 'error');
    }
  }

  /**
   * 绑定事件监听器
   */
  function bindEvents() {
    // 分析按钮
    const analyzeBtn = document.getElementById('analyzeBtn');
    if (analyzeBtn) {
      analyzeBtn.addEventListener('click', handleAnalyze);
    }

    // 清空按钮
    const clearBtn = document.getElementById('clearBtn');
    if (clearBtn) {
      clearBtn.addEventListener('click', handleClear);
    }

    // 导出按钮
    const exportBtn = document.getElementById('exportBtn');
    if (exportBtn) {
      exportBtn.addEventListener('click', handleExport);
    }

    // 视图切换按钮
    const viewBtns = document.querySelectorAll('.view-btn');
    viewBtns.forEach(btn => {
      btn.addEventListener('click', handleViewSwitch);
    });
  }

  /**
   * 处理SQL分析
   */
  async function handleAnalyze() {
    const sqlInput = document.getElementById('sqlInput');
    const dbType = document.getElementById('dbType');
    
    const sql = sqlInput.value.trim();
    if (!sql) {
      showMessage('请输入SQL语句', 'warning');
      return;
    }

    try {
      showLoading(true);
      
      // 调用API
      const result = await window.LineageAPI.analyze(sql, dbType.value);
      
      // 保存分析结果
      currentLineageResult = result;
      
      // 显示结果
      displayResults(result);
      
      // 启用导出按钮
      document.getElementById('exportBtn').disabled = false;
      
      showMessage('SQL分析完成', 'success');
    } catch (error) {
      console.error('分析失败:', error);
      showMessage('分析失败: ' + (error.message || '未知错误'), 'error');
    } finally {
      showLoading(false);
    }
  }

  /**
   * 处理清空操作
   */
  function handleClear() {
    document.getElementById('sqlInput').value = '';
    document.getElementById('results').style.display = 'none';
    document.getElementById('exportBtn').disabled = true;
    currentLineageResult = null;
  }

  /**
   * 处理导出操作
   */
  async function handleExport() {
    if (!currentLineageResult) {
      showMessage('请先分析SQL', 'warning');
      return;
    }

    try {
      showLoading(true);
      await window.LineageAPI.exportExcel(currentLineageResult);
      showMessage('导出成功', 'success');
    } catch (error) {
      console.error('导出失败:', error);
      showMessage('导出失败: ' + (error.message || '未知错误'), 'error');
    } finally {
      showLoading(false);
    }
  }

  /**
   * 处理视图切换
   */
  function handleViewSwitch(event) {
    const btn = event.target;
    const view = btn.dataset.view;
    
    // 更新按钮状态
    document.querySelectorAll('.view-btn').forEach(b => b.classList.remove('active'));
    btn.classList.add('active');
    
    // 切换视图
    document.querySelectorAll('.view-container').forEach(v => v.style.display = 'none');
    const viewContainer = document.getElementById(view + 'View');
    if (viewContainer) {
      viewContainer.style.display = 'block';
    }
  }

  /**
   * 显示分析结果
   */
  function displayResults(result) {
    const resultsSection = document.getElementById('results');
    resultsSection.style.display = 'block';
    
    // 显示表格视图
    if (window.TableView) {
      window.TableView.render(result);
    }
    
    // 显示树形视图
    if (window.TreeView) {
      window.TreeView.render(result);
    }
    
    // 显示DAG图
    if (window.LineageVisualizer) {
      window.LineageVisualizer.render(result);
    }
  }

  /**
   * 显示/隐藏加载指示器
   */
  function showLoading(show) {
    const loading = document.getElementById('loading');
    if (loading) {
      loading.style.display = show ? 'flex' : 'none';
    }
  }

  /**
   * 显示消息提示
   */
  function showMessage(text, type = 'info') {
    const message = document.createElement('div');
    message.className = `message message-${type}`;
    message.textContent = text;
    document.body.appendChild(message);
    
    // 3秒后自动移除
    setTimeout(() => {
      message.remove();
    }, 3000);
  }

  // 暴露全局方法（供调试使用）
  window.LineageApp = {
    showMessage,
    showLoading
  };

})();
