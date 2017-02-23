package cn.trace.nlp.etl.api;

/**
 * @author trace
 * 
 */
public interface Similarity {
    
    Double similar(String source, String translation);
}
