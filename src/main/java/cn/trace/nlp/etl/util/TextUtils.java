package cn.trace.nlp.etl.util;

/**
 * @author trace
 * 
 */
public class TextUtils {

    /**
     * 判断是否为中文字符
     * 
     * @param ch
     * @return
     */
    public static boolean isChinese(char ch) {
        Character.UnicodeBlock ub = Character.UnicodeBlock.of(ch);
        if (ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS || ub == Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS
                || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A || ub == Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B
                || ub == Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION || ub == Character.UnicodeBlock.HALFWIDTH_AND_FULLWIDTH_FORMS
                || ub == Character.UnicodeBlock.GENERAL_PUNCTUATION) {
            return true;
        }
        return false;
    }
    
    /**
     * 判断是否为英文字符
     *   
     * @param ch
     * @return
     */
    public static boolean isEnglish(char ch) {
        if((ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z')) {
            return true;
        }
        return false;
    }
    
    /**
     * 判断字符是否为数字
     * 
     * @param c
     * @return
     */
    public static boolean isDigital(char ch) {
        if(ch >= '0' && ch <= '9') {
            return true;
        }
        return false;
    }
    
    /**
     * 判断字符是否为标点符号
     * 
     * @param ch
     * @return
     */
    public static boolean isPunctuation(char ch) {
        if(ch == '（' || ch =='(' || ch == '）' || ch ==')' || ch == '{' 
                || ch == '}' || ch == '【' || ch == '】' || ch == ',' 
                || ch == '，' || ch == ';' || ch == '；' || ch == ':' 
                || ch == '：' || ch == '‘' || ch == '’' || ch == '“' 
                || ch == '”' || ch == '<' || ch == '>' || ch == '《' 
                || ch == '》' || ch == '.' || ch == '。' || ch == '?' 
                || ch == '？' || ch == '!' || ch == '！' || ch == '-'
                || ch == '#' || ch == '$' || ch == '%' || ch == '^'
                || ch == '&' || ch == '*' || ch == '+' || ch == '='
                || ch == '/' || ch == '、' || ch == '￥' || ch == '~'
                || ch == ' ' || ch == '\'' || ch == '\"' || ch == '@') {
            return true;
        }
        return false;
    }
    
}
