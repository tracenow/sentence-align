package cn.trace.nlp.etl.data;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.alibaba.fastjson.JSON;
import com.google.common.collect.Lists;

/**
 * @author trace
 * 
 */
public class Section {
    
    private List<Sentence> sentences = null;

    public List<Sentence> getSentences() {
        return sentences;
    }

    public void setSentences(List<Sentence> sentences) {
        this.sentences = sentences;
    }

    public String getSource() {
        return getSourceOrTranslation(true);
    }

    public String getTranslation() {
        return getSourceOrTranslation(false);
    }

    private String getSourceOrTranslation(boolean isSource) {
        StringBuilder line = new StringBuilder();
        if(CollectionUtils.isNotEmpty(sentences)) {
            boolean first = true;
            for(Sentence sentence : sentences) {
                if(first) {
                    if(isSource) {
                        line.append(sentence.getSource());
                    } else {
                        line.append(sentence.getTranslation());
                    }
                    first = false;
                } else {
                    if(isSource) {
                        line.append(" " + sentence.getSource());
                    } else {
                        line.append(" " + sentence.getTranslation());
                    }
                }
            }
        }
        return line.toString();
    }

    public Sentence getSentence(int index) {
        if(index < 0 || index >= size()) {
            throw new IllegalArgumentException("size : " + size() + " , index : " + index);
        }
        return sentences.get(index);
    }
    
    public int size() {
        return sentences == null ? 0 : sentences.size();
    }

    public int getSourceLength() {
        int sum = 0;
        if(sentences == null) {
            return 0;
        }
        for(Sentence sentence : sentences) {
            sum += sentence.getSource().length();
        }
        return sum;
    }
    
    public static Section newInstance() {
        Section section = new Section();
        List<Sentence> sentences = Lists.newArrayList();
        section.setSentences(sentences);
        return section;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((sentences == null) ? 0 : sentences.hashCode());
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
        Section other = (Section) obj;
        if (sentences == null) {
            if (other.sentences != null)
                return false;
        } else if (!sentences.equals(other.sentences))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
    
}
