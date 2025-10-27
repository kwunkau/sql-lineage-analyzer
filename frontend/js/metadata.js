/**
 * 元数据管理页面脚本
 */

// API基础URL
const API_BASE = '/api/metadata';

// 当前编辑的数据源/表ID
let editingDataSourceId = null;
let editingTableId = null;

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    initTabs();
    loadDataSources();
    initForms();
});

/**
 * 初始化标签页切换
 */
function initTabs() {
    const tabBtns = document.querySelectorAll('.tab-btn');
    tabBtns.forEach(btn => {
        btn.addEventListener('click', function() {
            const tabName = this.dataset.tab;
            
            // 切换按钮状态
            tabBtns.forEach(b => b.classList.remove('active'));
            this.classList.add('active');
            
            // 切换内容
            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('active');
            });
            document.getElementById(`${tabName}-tab`).classList.add('active');
            
            // 加载对应数据
            if (tabName === 'datasource') {
                loadDataSources();
            } else if (tabName === 'tables') {
                loadTables();
            }
        });
    });
}

/**
 * 初始化表单提交事件
 */
function initForms() {
    // 数据源表单
    document.getElementById('datasource-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        await saveDataSource();
    });
    
    // 表表单
    document.getElementById('table-form').addEventListener('submit', async function(e) {
        e.preventDefault();
        await saveTable();
    });
    
    // 数据源筛选
    document.getElementById('datasource-filter').addEventListener('change', function() {
        loadTables();
    });
}

// ==================== 数据源管理 ====================

/**
 * 加载数据源列表
 */
async function loadDataSources() {
    showLoading(true);
    try {
        const response = await axios.get(`${API_BASE}/datasource`, {
            params: { page: 1, size: 100 }
        });
        
        if (response.data.code === 200) {
            const datasources = response.data.data.records || [];
            renderDataSources(datasources);
            updateDataSourceFilter(datasources);
        } else {
            showMessage('加载失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        console.error('加载数据源失败:', error);
        showMessage('加载数据源失败', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 渲染数据源列表
 */
function renderDataSources(datasources) {
    const tbody = document.getElementById('datasource-list');
    
    if (datasources.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无数据源</td></tr>';
        return;
    }
    
    tbody.innerHTML = datasources.map(ds => `
        <tr>
            <td><strong>${ds.name}</strong></td>
            <td><span class="badge badge-success">${ds.type.toUpperCase()}</span></td>
            <td>${ds.url}</td>
            <td>${ds.databaseName || '-'}</td>
            <td>${ds.description || '-'}</td>
            <td>
                <button class="action-btn btn-test" onclick="testConnection(${ds.id})">测试连接</button>
                <button class="action-btn btn-import" onclick="importMetadata(${ds.id})">导入元数据</button>
                <button class="action-btn btn-edit" onclick="editDataSource(${ds.id})">编辑</button>
                <button class="action-btn btn-delete" onclick="deleteDataSource(${ds.id})">删除</button>
            </td>
        </tr>
    `).join('');
}

/**
 * 更新数据源筛选下拉框
 */
function updateDataSourceFilter(datasources) {
    const select = document.getElementById('datasource-filter');
    const tableSelect = document.getElementById('table-datasource');
    
    const options = datasources.map(ds => 
        `<option value="${ds.id}">${ds.name} (${ds.type})</option>`
    ).join('');
    
    select.innerHTML = '<option value="">全部数据源</option>' + options;
    tableSelect.innerHTML = options;
}

/**
 * 显示添加数据源模态框
 */
function showAddDataSourceModal() {
    editingDataSourceId = null;
    document.getElementById('datasource-modal-title').textContent = '添加数据源';
    document.getElementById('datasource-form').reset();
    document.getElementById('datasource-modal').classList.add('show');
}

/**
 * 编辑数据源
 */
async function editDataSource(id) {
    try {
        const response = await axios.get(`${API_BASE}/datasource/${id}`);
        if (response.data.code === 200) {
            const ds = response.data.data;
            editingDataSourceId = id;
            
            document.getElementById('datasource-modal-title').textContent = '编辑数据源';
            document.getElementById('datasource-id').value = ds.id;
            document.getElementById('datasource-name').value = ds.name;
            document.getElementById('datasource-type').value = ds.type;
            document.getElementById('datasource-url').value = ds.url;
            document.getElementById('datasource-username').value = ds.username || '';
            document.getElementById('datasource-password').value = ds.password || '';
            document.getElementById('datasource-database').value = ds.databaseName || '';
            document.getElementById('datasource-description').value = ds.description || '';
            
            document.getElementById('datasource-modal').classList.add('show');
        }
    } catch (error) {
        showMessage('加载数据源失败', 'error');
    }
}

/**
 * 保存数据源
 */
async function saveDataSource() {
    const data = {
        name: document.getElementById('datasource-name').value,
        type: document.getElementById('datasource-type').value,
        url: document.getElementById('datasource-url').value,
        username: document.getElementById('datasource-username').value,
        password: document.getElementById('datasource-password').value,
        databaseName: document.getElementById('datasource-database').value,
        description: document.getElementById('datasource-description').value
    };
    
    try {
        let response;
        if (editingDataSourceId) {
            response = await axios.put(`${API_BASE}/datasource/${editingDataSourceId}`, data);
        } else {
            response = await axios.post(`${API_BASE}/datasource`, data);
        }
        
        if (response.data.code === 200) {
            showMessage(editingDataSourceId ? '更新成功' : '添加成功', 'success');
            closeDataSourceModal();
            loadDataSources();
        } else {
            showMessage('保存失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        console.error('保存数据源失败:', error);
        showMessage('保存失败', 'error');
    }
}

/**
 * 删除数据源
 */
async function deleteDataSource(id) {
    if (!confirm('确定要删除此数据源吗？')) {
        return;
    }
    
    try {
        const response = await axios.delete(`${API_BASE}/datasource/${id}`);
        if (response.data.code === 200) {
            showMessage('删除成功', 'success');
            loadDataSources();
        } else {
            showMessage('删除失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        showMessage('删除失败', 'error');
    }
}

/**
 * 测试数据源连接
 */
async function testConnection(id) {
    showLoading(true);
    try {
        const response = await axios.get(`${API_BASE}/test-connection/${id}`);
        if (response.data.code === 200 && response.data.data === true) {
            showMessage('连接成功！', 'success');
        } else {
            showMessage('连接失败', 'error');
        }
    } catch (error) {
        showMessage('连接失败', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 从数据源导入元数据
 */
async function importMetadata(datasourceId) {
    if (!confirm('确定要从此数据源导入元数据吗？此操作可能需要较长时间。')) {
        return;
    }
    
    showLoading(true);
    try {
        const response = await axios.post(`${API_BASE}/import`, {
            dataSourceId: datasourceId,
            tableNames: null  // 导入所有表
        });
        
        if (response.data.code === 200) {
            showMessage(`导入成功！共导入 ${response.data.data} 个表`, 'success');
            loadTables();
        } else {
            showMessage('导入失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        console.error('导入元数据失败:', error);
        showMessage('导入失败', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 关闭数据源模态框
 */
function closeDataSourceModal() {
    document.getElementById('datasource-modal').classList.remove('show');
    editingDataSourceId = null;
}

// ==================== 表元数据管理 ====================

/**
 * 加载表列表
 */
async function loadTables() {
    showLoading(true);
    try {
        const datasourceId = document.getElementById('datasource-filter').value;
        const params = { page: 1, size: 100 };
        if (datasourceId) {
            params.datasourceId = datasourceId;
        }
        
        const response = await axios.get(`${API_BASE}/table`, { params });
        
        if (response.data.code === 200) {
            const tables = response.data.data.records || [];
            renderTables(tables);
        } else {
            showMessage('加载失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        console.error('加载表失败:', error);
        showMessage('加载表列表失败', 'error');
    } finally {
        showLoading(false);
    }
}

/**
 * 渲染表列表
 */
function renderTables(tables) {
    const tbody = document.getElementById('tables-list');
    
    if (tables.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center;">暂无表元数据</td></tr>';
        return;
    }
    
    tbody.innerHTML = tables.map(table => `
        <tr>
            <td><strong>${table.tableName}</strong></td>
            <td>${table.schemaName || '-'}</td>
            <td><span class="badge badge-success">${table.tableType}</span></td>
            <td>${table.tableComment || '-'}</td>
            <td><button class="action-btn btn-edit" onclick="viewColumns(${table.id})">查看字段</button></td>
            <td>
                <button class="action-btn btn-edit" onclick="editTable(${table.id})">编辑</button>
                <button class="action-btn btn-delete" onclick="deleteTable(${table.id})">删除</button>
            </td>
        </tr>
    `).join('');
}

/**
 * 显示添加表模态框
 */
function showAddTableModal() {
    editingTableId = null;
    document.getElementById('table-modal-title').textContent = '添加表';
    document.getElementById('table-form').reset();
    document.getElementById('table-modal').classList.add('show');
}

/**
 * 编辑表
 */
async function editTable(id) {
    try {
        const response = await axios.get(`${API_BASE}/table/${id}`);
        if (response.data.code === 200) {
            const table = response.data.data;
            editingTableId = id;
            
            document.getElementById('table-modal-title').textContent = '编辑表';
            document.getElementById('table-id').value = table.id;
            document.getElementById('table-datasource').value = table.datasourceId;
            document.getElementById('table-name').value = table.tableName;
            document.getElementById('table-schema').value = table.schemaName || '';
            document.getElementById('table-type').value = table.tableType;
            document.getElementById('table-comment').value = table.tableComment || '';
            
            document.getElementById('table-modal').classList.add('show');
        }
    } catch (error) {
        showMessage('加载表失败', 'error');
    }
}

/**
 * 保存表
 */
async function saveTable() {
    const data = {
        datasourceId: parseInt(document.getElementById('table-datasource').value),
        tableName: document.getElementById('table-name').value,
        schemaName: document.getElementById('table-schema').value,
        tableType: document.getElementById('table-type').value,
        tableComment: document.getElementById('table-comment').value
    };
    
    try {
        let response;
        if (editingTableId) {
            response = await axios.put(`${API_BASE}/table/${editingTableId}`, data);
        } else {
            response = await axios.post(`${API_BASE}/table`, data);
        }
        
        if (response.data.code === 200) {
            showMessage(editingTableId ? '更新成功' : '添加成功', 'success');
            closeTableModal();
            loadTables();
        } else {
            showMessage('保存失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        console.error('保存表失败:', error);
        showMessage('保存失败', 'error');
    }
}

/**
 * 删除表
 */
async function deleteTable(id) {
    if (!confirm('确定要删除此表吗？相关字段也会被删除。')) {
        return;
    }
    
    try {
        const response = await axios.delete(`${API_BASE}/table/${id}`);
        if (response.data.code === 200) {
            showMessage('删除成功', 'success');
            loadTables();
        } else {
            showMessage('删除失败: ' + response.data.message, 'error');
        }
    } catch (error) {
        showMessage('删除失败', 'error');
    }
}

/**
 * 查看字段列表
 */
async function viewColumns(tableId) {
    try {
        const response = await axios.get(`${API_BASE}/column`, {
            params: { tableId }
        });
        
        if (response.data.code === 200) {
            const columns = response.data.data || [];
            
            if (columns.length === 0) {
                alert('该表暂无字段信息');
                return;
            }
            
            const columnList = columns.map(col => 
                `${col.columnName} (${col.columnType}) - ${col.columnComment || '无描述'}`
            ).join('\n');
            
            alert(`字段列表 (共${columns.length}个):\n\n${columnList}`);
        }
    } catch (error) {
        showMessage('加载字段失败', 'error');
    }
}

/**
 * 关闭表模态框
 */
function closeTableModal() {
    document.getElementById('table-modal').classList.remove('show');
    editingTableId = null;
}

// ==================== 工具函数 ====================

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
