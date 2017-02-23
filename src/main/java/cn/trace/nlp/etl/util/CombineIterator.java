package cn.trace.nlp.etl.util;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import cn.trace.nlp.etl.data.CombineSentence;
import cn.trace.nlp.etl.data.Section;
import cn.trace.nlp.etl.data.Sentence;

import com.google.common.collect.Lists;

/**
 * @author trace
 * 
 */
public class CombineIterator implements Iterator<Section> {
    
    // 源数据
    private Section section;
    // 结果数组大小
    private int resultSize;
    // 结果数组个数
    private int size;
    // 当前元素索引
    private int[] index;
    // 当前序列索引
    private int offset = 0;

    public CombineIterator(Section section, int resultSize) {
        if (section == null)
            throw new NullPointerException();
        int n = section.size();
        if (n < resultSize || resultSize <= 0)
            throw new IllegalArgumentException("size : " + n + ", m : " + resultSize);
        this.section = section;
        this.size = clacSize(n - 1, resultSize - 1);
        this.resultSize = resultSize;
        this.index = new int[resultSize + 1];
        for (int i = 0; i < resultSize; i++) {
            this.index[i] = i;
        }
        this.index[resultSize - 1] -= 1;
        this.index[resultSize] = n;
    }

    /**
     * 计算结果数组个数
     * 
     * @param n
     * @param m
     * @return
     */
    private int clacSize(int n, int m) {
        return m == 0 ? 1 : factorial(n - m + 1, n).divide(factorial(m)).intValue();
    }
    
    /**
     * 计算1到n的阶乘,0! = 1
     * 
     * @param n
     * @return 1 * 2 *3 * ... * (n - 1) * n
     */
    private static BigInteger factorial(int n) {
        if (n == 0)
            return new BigInteger("1");
        return factorial(1, n);
    }

    /**
     * 计算start到end的阶乘,不支持0参数
     * 
     * @param start 起始数(包含)
     * @param end 终止数(包含)
     * @return start * (start + 1) * ... *(end - 1) * end
     */
    private static BigInteger factorial(int start, int end) {
        if (start <= 0 || end < start) {
            throw new IllegalArgumentException("start : " + start + ",end : " + end);
        }
        BigInteger result = new BigInteger("1");
        for (int i = start; i <= end; i++) {
            result = result.multiply(new BigInteger(i + ""));
        }
        return result;
    }

    /**
     * 获取迭代器内元素总数
     * 
     * @return
     */
    public int size() {
        return size;
    }

    @Override
    public boolean hasNext() {
        return offset < size;
    }

    @Override
    public Section next() {
        int idx = resultSize - 1;
        if (index[idx] < index[resultSize] - 1) {
            index[idx] += 1;
        } else {
            idx -= 1;
            while (idx > 0 && index[idx] == index[idx + 1] - 1) {
                idx -= 1;
            }
            index[idx] += 1;
            for (int i = idx + 1; i <= resultSize - 1; i++) {
                index[i] = index[idx] + (i - idx);
            }
        }
        List<Sentence> sentences = Lists.newArrayListWithCapacity(resultSize);
        for (int i = 0; i < resultSize; i++) {
            CombineSentence sentence = CombineSentence.newInstance();
            StringBuilder source = new StringBuilder();
            StringBuilder translation = new StringBuilder();
            boolean first = true;
            for (Sentence s : section.getSentences().subList(index[i], index[i + 1])) {
                if(!first) {
                    if(StringUtils.isNotBlank(s.getSource())) {
                        source.append(" ");
                    }
                    if(StringUtils.isNotBlank(s.getTranslation())) {
                        translation.append(" ");
                    }
                } else {
                    first = false;
                }
                if(StringUtils.isNotBlank(s.getSource())) {
                    source.append(s.getSource());
                }
                if(StringUtils.isNotBlank(s.getTranslation())) {
                    translation.append(s.getTranslation());
                }
            }
            sentence.setSource(source.toString());
            sentence.setTranslation(translation.toString());
            sentence.setStartIndex(index[i]);
            sentence.setEndIndex(index[i + 1]);
            sentences.add(sentence);
        }
        offset++;
        Section result = Section.newInstance();
        result.setSentences(sentences);
        return result;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }
}