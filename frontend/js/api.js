// SQL字段级血缘分析平台 - API调用封装

(function() {
  'use strict';

  const API_BASE_URL = 'http://localhost:8080/api';

  /**
   * API客户端类
   */
  class LineageAPI {
    /**
     * 分析SQL血缘
     * @param {string} sql - SQL语句
     * @param {string} dbType - 数据库类型
     * @returns {Promise} 分析结果
     */
    static async analyze(sql, dbType) {
      const response = await axios.post(`${API_BASE_URL}/lineage/analyze`, {
        sql: sql,
        dbType: dbType
      });
      return response.data.data;
    }

    /**
     * 导出Excel
     * @param {Object} lineageResult - 血缘分析结果
     * @returns {Promise} 导出结果
     */
    static async exportExcel(lineageResult) {
      const response = await axios.post(`${API_BASE_URL}/export/excel`, lineageResult, {
        responseType: 'blob'
      });
      
      // 创建下载链接
      const url = window.URL.createObjectURL(new Blob([response.data]));
      const link = document.createElement('a');
      link.href = url;
      link.setAttribute('download', `lineage_${Date.now()}.xlsx`);
      document.body.appendChild(link);
      link.click();
      link.remove();
    }

    /**
     * 获取元数据列表
     * @returns {Promise} 元数据列表
     */
    static async getMetadata() {
      const response = await axios.get(`${API_BASE_URL}/metadata/list`);
      return response.data.data;
    }

    /**
     * 上传Kettle文件
     * @param {File} file - Kettle文件
     * @returns {Promise} 上传结果
     */
    static async uploadKettle(file) {
      const formData = new FormData();
      formData.append('file', file);
      
      const response = await axios.post(`${API_BASE_URL}/kettle/upload`, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });
      return response.data.data;
    }
  }

  // 暴露到全局
  window.LineageAPI = LineageAPI;

})();
