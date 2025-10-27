package com.lineage.kettle.parser;

import com.lineage.kettle.model.KettleHop;
import com.lineage.kettle.model.KettleStep;
import com.lineage.kettle.model.KettleTransformation;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.List;

/**
 * Kettle转换文件解析器
 */
@Slf4j
@Component
public class KettleParser {
    
    /**
     * 解析.ktr文件
     */
    public KettleTransformation parse(File file) throws Exception {
        log.info("开始解析Kettle文件: {}", file.getAbsolutePath());
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(file);
        
        return parseDocument(document);
    }
    
    /**
     * 解析.ktr文件（InputStream）
     */
    public KettleTransformation parse(InputStream inputStream) throws Exception {
        log.info("开始解析Kettle文件流");
        
        SAXReader reader = new SAXReader();
        Document document = reader.read(inputStream);
        
        return parseDocument(document);
    }
    
    /**
     * 解析XML文档
     */
    private KettleTransformation parseDocument(Document document) {
        KettleTransformation transformation = new KettleTransformation();
        
        Element root = document.getRootElement();
        
        // 解析transformation基本信息
        Element infoElement = root.element("info");
        if (infoElement != null) {
            transformation.setName(getElementText(infoElement, "name"));
            transformation.setDescription(getElementText(infoElement, "description"));
        }
        
        // 解析步骤（steps）
        List<Element> stepElements = root.elements("step");
        for (Element stepElement : stepElements) {
            KettleStep step = parseStep(stepElement);
            transformation.addStep(step);
        }
        
        // 解析连接（hops）
        Element orderElement = root.element("order");
        if (orderElement != null) {
            List<Element> hopElements = orderElement.elements("hop");
            for (Element hopElement : hopElements) {
                KettleHop hop = parseHop(hopElement);
                transformation.addHop(hop);
            }
        }
        
        log.info("解析完成: name={}, steps={}, hops={}", 
                transformation.getName(), 
                transformation.getSteps().size(), 
                transformation.getHops().size());
        
        return transformation;
    }
    
    /**
     * 解析步骤
     */
    private KettleStep parseStep(Element stepElement) {
        KettleStep step = new KettleStep();
        
        step.setName(getElementText(stepElement, "name"));
        step.setType(getElementText(stepElement, "type"));
        
        // 解析SQL（TableInput）
        String sql = getElementText(stepElement, "sql");
        if (sql != null && !sql.isEmpty()) {
            step.addAttribute("sql", sql);
        }
        
        // 解析表名和Schema（TableOutput）
        String tableName = getElementText(stepElement, "table");
        if (tableName != null && !tableName.isEmpty()) {
            step.addAttribute("table", tableName);
        }
        
        String schemaName = getElementText(stepElement, "schema");
        if (schemaName != null && !schemaName.isEmpty()) {
            step.addAttribute("schema", schemaName);
        }
        
        // 解析数据库连接
        String connection = getElementText(stepElement, "connection");
        if (connection != null && !connection.isEmpty()) {
            step.addAttribute("connection", connection);
        }
        
        // 解析其他常用属性
        parseCommonAttributes(stepElement, step);
        
        return step;
    }
    
    /**
     * 解析常用属性
     */
    private void parseCommonAttributes(Element stepElement, KettleStep step) {
        // lookup表名（用于lookup步骤）
        String lookup = getElementText(stepElement, "lookup");
        if (lookup != null && !lookup.isEmpty()) {
            step.addAttribute("lookup", lookup);
        }
        
        // 字段列表
        Element fieldsElement = stepElement.element("fields");
        if (fieldsElement != null) {
            List<Element> fieldElements = fieldsElement.elements("field");
            StringBuilder fields = new StringBuilder();
            for (Element fieldElement : fieldElements) {
                String fieldName = getElementText(fieldElement, "name");
                if (fieldName != null && !fieldName.isEmpty()) {
                    if (fields.length() > 0) {
                        fields.append(",");
                    }
                    fields.append(fieldName);
                }
            }
            if (fields.length() > 0) {
                step.addAttribute("fields", fields.toString());
            }
        }
    }
    
    /**
     * 解析连接
     */
    private KettleHop parseHop(Element hopElement) {
        KettleHop hop = new KettleHop();
        
        hop.setFromStep(getElementText(hopElement, "from"));
        hop.setToStep(getElementText(hopElement, "to"));
        
        String enabled = getElementText(hopElement, "enabled");
        if (enabled != null) {
            hop.setEnabled("Y".equalsIgnoreCase(enabled) || "true".equalsIgnoreCase(enabled));
        }
        
        return hop;
    }
    
    /**
     * 获取元素文本（处理CDATA）
     */
    private String getElementText(Element parent, String elementName) {
        if (parent == null) {
            return null;
        }
        
        Element element = parent.element(elementName);
        if (element == null) {
            return null;
        }
        
        String text = element.getTextTrim();
        return text.isEmpty() ? null : text;
    }
}
