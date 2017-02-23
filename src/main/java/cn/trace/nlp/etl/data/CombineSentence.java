package cn.trace.nlp.etl.data;

import com.alibaba.fastjson.JSON;

/**
 * @author trace
 * 
 */
public class CombineSentence extends Sentence {

    private int startIndex = -1;
    
    private int endIndex = -1;

    public int getStartIndex() {
        return startIndex;
    }

    public void setStartIndex(int startIndex) {
        this.startIndex = startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    public static CombineSentence newInstance() {
        CombineSentence sentence = new CombineSentence();
        return sentence;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + endIndex;
        result = prime * result + startIndex;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        CombineSentence other = (CombineSentence) obj;
        if (endIndex != other.endIndex)
            return false;
        if (startIndex != other.startIndex)
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        return JSON.toJSONString(this);
    }

}
