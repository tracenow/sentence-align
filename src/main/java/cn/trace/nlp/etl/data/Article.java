package cn.trace.nlp.etl.data;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;

/**
 * @author trace
 * 
 */
public class Article {
    
    private List<Section> zhSections = null;
    
    private List<Section> enSections = null;

    public List<Section> getZhSections() {
        return zhSections;
    }

    public void setZhSections(List<Section> zhSections) {
        this.zhSections = zhSections;
    }

    public List<Section> getEnSections() {
        return enSections;
    }

    public void setEnSections(List<Section> enSections) {
        this.enSections = enSections;
    }
    
    public String getChineseSource() {
        return getSourceOrTranslation(false, true);
    }
    
    public String getEnglishSource() {
        return getSourceOrTranslation(true, true);
    }
    
    public String getChineseTranslation() {
        return getSourceOrTranslation(false, false);
    }
    
    public String getEnglishTranslation() {
        return getSourceOrTranslation(true, false);
    }
    
    private String getSourceOrTranslation(boolean isEnglish, boolean isSource) {
        StringBuilder line = new StringBuilder();
        List<Section> sections = zhSections;
        if(isEnglish) {
            sections = enSections;
        }
        if(CollectionUtils.isNotEmpty(sections)) {
            boolean first = true;
            for(Section section : sections) {
                if(first) {
                    if(isSource) {
                        line.append(section.getSource());
                    } else {
                        line.append(section.getTranslation());
                    }
                    first = false;
                } else {
                    if(isSource) {
                        line.append("\n" + section.getSource());
                    } else {
                        line.append("\n" + section.getTranslation());
                    }
                }
            }
        }
        return line.toString();
    }
    
    public static Article newInstance() {
        Article article = new Article();
        List<Section> zhSections = Lists.newArrayList();
        List<Section> enSections = Lists.newArrayList();
        article.setZhSections(zhSections);
        article.setEnSections(enSections);
        return article;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((enSections == null) ? 0 : enSections.hashCode());
        result = prime * result + ((zhSections == null) ? 0 : zhSections.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Article other = (Article) obj;
        if (enSections == null) {
            if (other.enSections != null)
                return false;
        } else if (!enSections.equals(other.enSections))
            return false;
        if (zhSections == null) {
            if (other.zhSections != null)
                return false;
        } else if (!zhSections.equals(other.zhSections))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
}
