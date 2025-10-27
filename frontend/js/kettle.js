/**
 * Kettle解析页面脚本
 */

// API基础URL
const API_BASE = '/api/kettle';

// 当前选择的文件列表
let selectedFiles = [];

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    initUploadZone();
    initButtons();
});

/**
 * 初始化上传区域
 */
function initUploadZone() {
    const uploadZone = document.getElementById('upload-zone');
    const fileInput = document.getElementById('file-input');
    
    // 点击上传区域触发文件选择
    uploadZone.addEventListener('click', function() {
        fileInput.click();
    });
    
    // 文件选择事件
    fileInput.addEventListener('change', function(e) {
        handleFiles(e.target.files);
    });
    
    // 拖拽事件
    uploadZone.addEventListener('dragover', function(e) {
        e.preventDefault();
        uploadZone.classList.add('dragover');
    });
    
    uploadZone.addEventListener('dragleave', function() {
        uploadZone.classList.remove('dragover');
    });
    
    uploadZone.addEventListener('drop', function(e) {
        e.preventDefault();
        uploadZone.classList.remove('dragover');
        handleFiles(e.dataTransfer.files);
    });
}

/**
 * 初始化按钮事件
 */
function initButtons() {
    document.getElementById('parse-btn').addEventListener('click', parseFiles);
    document.getElementById('clear-btn').addEventListener('click', clearFiles);
}

/**
 * 处理选择的文件
 */
function handleFiles(files) {
    const ktrFiles = Array.from(files).filter(file => file.name.endsWith('.ktr'));
    
    if (ktrFiles.length === 0) {
        alert('请选择 .ktr 文件');
        return;
    }
    
    // 添加到文件列表
    selectedFiles = [...selectedFiles, ...ktrFiles];
    renderFileList();
    
    // 启用解析按钮
    document.getElementById('parse-btn').disabled = false;
}

/**
 * 渲染文件列表
 */
function renderFileList() {
    const fileList = document.getElementById('file-list');
    
    if (selectedFiles.length === 0) {
        fileList.innerHTML = '';
        return;
    }
    
    fileList.innerHTML = selectedFiles.map((file, index) => `
        <div class="file-item">
            <div class="file-info">
                <span class="file-name">📄 ${file.name}</span>
                <span class="file-size">${formatFileSize(file.size)}</span>
            </div>
            <button class="remove-file" onclick="removeFile(${index})">删除</button>
        </div>
    `).join('');
}

/**
 * 移除文件
 */
function removeFile(index) {
    selectedFiles.splice(index, 1);
    renderFileList();
    
    if (selectedFiles.length === 0) {
        document.getElementById('parse-btn').disabled = true;
    }
}

/**
 * 清空文件列表
 */
function clearFiles() {
    selectedFiles = [];
    renderFileList();
    document.getElementById('parse-btn').disabled = true;
    document.getElementById('stats-section').style.display = 'none';
    document.getElementById('sql-results').style.display = 'none';
    document.getElementById('file-input').value = '';
}

/**
 * 解析文件
 */
async function parseFiles() {
    if (selectedFiles.length === 0) {
        return;
    }
    
    showLoading(true);
    
    try {
        // 只解析第一个文件（可以扩展为批量解析）
        const file = selectedFiles[0];
        const formData = new FormData();
        formData.append('file', file);
        
        const response = await axios.post(`${API_BASE}/parse-sql`, formData, {
            headers: {
                'Content-Type': 'multipart/form-data'
            }
        });
        
        if (response.data.code === 200) {
            const result = response.data.data;
            displayResults(result);
            showMessage('解析成功！', 'success');
        } else {
            showMessage('解析失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        console.error('解析失败:', error);
        showMessage('解析失败: ' + (error.response?.data?.message || error.message), 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 显示解析结果
 */
function displayResults(result) {
    // 显示统计信息
    document.getElementById('stat-steps').textContent = result.stepCount || 0;
    document.getElementById('stat-sqls').textContent = result.sqlCount || 0;
    document.getElementById('stat-hops').textContent = result.hopCount || 0;
    document.getElementById('stats-section').style.display = 'block';
    
    // 显示SQL列表
    const sqls = result.sqls || [];
    if (sqls.length > 0) {
        renderSqlList(sqls);
        document.getElementById('sql-results').style.display = 'block';
    } else {
        document.getElementById('sql-results').style.display = 'none';
        showMessage('未找到SQL语句', 'warning');
    }
}

/**
 * 渲染SQL列表
 */
function renderSqlList(sqls) {
    const sqlList = document.getElementById('sql-list');
    
    sqlList.innerHTML = sqls.map((sqlInfo, index) => {
        const sql = sqlInfo.sql || '';
        const stepName = sqlInfo.stepName || '未知步骤';
        const stepType = sqlInfo.stepType || '未知类型';
        const sourceTable = sqlInfo.sourceTable || '-';
        const targetTable = sqlInfo.targetTable || '-';
        const connectionName = sqlInfo.connectionName || '-';
        
        return `
            <div class="sql-item">
                <div class="sql-header">
                    <h4>#${index + 1} ${stepName}</h4>
                    <button class="btn-analyze" onclick="analyzeSql('${escapeHtml(sql)}', '${stepType}')">
                        分析血缘
                    </button>
                </div>
                <div class="sql-meta">
                    <span class="meta-tag"><strong>类型:</strong> ${stepType}</span>
                    ${sourceTable !== '-' ? `<span class="meta-tag"><strong>源表:</strong> ${sourceTable}</span>` : ''}
                    ${targetTable !== '-' ? `<span class="meta-tag"><strong>目标表:</strong> ${targetTable}</span>` : ''}
                    ${connectionName !== '-' ? `<span class="meta-tag"><strong>连接:</strong> ${connectionName}</span>` : ''}
                </div>
                <div class="sql-content">${escapeHtml(sql)}</div>
            </div>
        `;
    }).join('');
}

/**
 * 分析SQL血缘
 */
function analyzeSql(sql, stepType) {
    // 跳转到主页面并传递SQL
    if (sql && sql.trim()) {
        // 将SQL存储到sessionStorage
        sessionStorage.setItem('analyzeSql', sql);
        sessionStorage.setItem('dbType', guessDbType(stepType));
        
        // 跳转到分析页面
        window.location.href = 'index.html';
    } else {
        alert('该步骤没有有效的SQL语句');
    }
}

/**
 * 根据步骤类型猜测数据库类型
 */
function guessDbType(stepType) {
    // 简单猜测，可以根据实际情况调整
    if (stepType.includes('Hive') || stepType.includes('HBase')) {
        return 'hive';
    } else if (stepType.includes('Spark')) {
        return 'spark';
    } else {
        return 'mysql';
    }
}

/**
 * 格式化文件大小
 */
function formatFileSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
}

/**
 * HTML转义
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 显示/隐藏加载指示器
 */
function showLoading(show) {
    document.getElementById('loading').style.display = show ? 'flex' : 'none';
}

/**
 * 显示消息提示
 */
function showMessage(message, type) {
    alert(message);  // 简单实现，可以替换为更好的UI组件
}
