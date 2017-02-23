package cn.trace.nlp.etl.data;

import com.alibaba.fastjson.JSON;

/**
 * @author trace
 * 
 */
public class Sentence {

    private String source = null;

    private String translation = null;

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTranslation() {
        return translation;
    }

    public void setTranslation(String translation) {
        this.translation = translation;
    }
    
    public static Sentence newInstance() {
        Sentence sentence = new Sentence();
        return sentence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((source == null) ? 0 : source.hashCode());
        result = prime * result + ((translation == null) ? 0 : translation.hashCode());
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
        Sentence other = (Sentence) obj;
        if (source == null) {
            if (other.source != null)
                return false;
        } else if (!source.equals(other.source))
            return false;
        if (translation == null) {
            if (other.translation != null)
                return false;
        } else if (!translation.equals(other.translation))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }
    
}
