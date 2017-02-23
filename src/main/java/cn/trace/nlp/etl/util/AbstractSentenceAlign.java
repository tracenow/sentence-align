package cn.trace.nlp.etl.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cn.trace.nlp.etl.api.Similarity;
import cn.trace.nlp.etl.api.Translatable;
import cn.trace.nlp.etl.data.Article;
import cn.trace.nlp.etl.data.CombineSentence;
import cn.trace.nlp.etl.data.Section;
import cn.trace.nlp.etl.data.Sentence;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

/**
 * @author trace
 * 
 */
public abstract class AbstractSentenceAlign implements Translatable, Similarity {
    
    private final static Logger logger = LoggerFactory.getLogger(AbstractSentenceAlign.class);
    
    private static StanfordCoreNLP pipeline = null;
    
    private final static int BASE_FACTOR = 4;
    
    private final static int MIN_CHINESE_TEXT_LENGHT = 3;
    
    private final static int MIN_ENGLISH_TEXT_LENGHT = MIN_CHINESE_TEXT_LENGHT * BASE_FACTOR;
    
    private final static int MAX_SENTENCE_SIZE = 19;
    
    private final static double SECTION_LIMIT = 0.1;
    
    private final static double SENTENCE_LIMIT = 0.1;
    
    private final static double OFFSET_LIMIT = 0.4;
    
    private final static String[] enSearchList = {"？", "！", "“", "”", "：", "，", "。", "（", "）", "’", "【", "】"};
    private final static String[] enReplacementList = {"?", "!", "\"", "\"", ":", ",", ".", "(", ")", "'", "(", ")"};
    private final static String[] zhSearchList = {"?", "!", ":", ",", "(", ")", "【", "】"};
    private final static String[] zhReplacementList = {"？", "！", "：", "，", "（", "）", "（", "）"};
    
    private static Set<String> stopwords = Sets.newHashSet();
    
    static {
        Properties props = new Properties();
        props.put("pos.model", "english-left3words-distsim.tagger");
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma");
        pipeline = new StanfordCoreNLP(props);
        InputStream input = null;
        BufferedReader reader = null;
        String line = null;
        try {
            input = AbstractSentenceAlign.class.getClassLoader().getResourceAsStream("stopwords");
            reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            while ((line = reader.readLine()) != null) {
                stopwords.add(line);
            }
        } catch(Throwable t) {
            logger.error("load file error", t);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    logger.error("close file reader error", e);
                }
            }
        }
    }
    
    /**
     * 转词根
     * 
     * @param text
     * @return
     */
    public static List<String> lemmatize(String text) {
        List<String> lemmas = Lists.newArrayList();
        try {
            if(StringUtils.isBlank(text)) {
                return lemmas;
            }
            Annotation document = new Annotation(text.toLowerCase());
            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(SentencesAnnotation.class);
            for(CoreMap sentence: sentences) {
                for (CoreLabel token: sentence.get(TokensAnnotation.class)) {
                    String word = token.get(LemmaAnnotation.class);
                    if(word.length() > 1 && !word.startsWith("'") && !word.startsWith("`") && !stopwords.contains(word)) {
                        lemmas.add(word);
                    }
                }
            }
        } catch(Throwable t) {
            logger.error("lemmatize error", t);
        }
        return lemmas;
    }
    
    /**
     * 解析英文段落为一到多个英文句子
     * 
     * @param text 英文段落文本
     * @param isTranslate 是否翻译 (true:需Translatable接口实现英译中的功能， Similarity接口实现为中文相似度)
     * @return
     */
    public Section parseEnglishSection(String text, boolean isTranslate) {
        Section section = Section.newInstance();
        if(StringUtils.isBlank(text)) {
            return section;
        }
        Annotation document = new Annotation(standardizingText(text, true));
        pipeline.annotate(document);
        for(CoreMap s : document.get(SentencesAnnotation.class)) {
            String str = s.toString().trim();
            if(StringUtils.isNotBlank(str)) {
                if(str.length() > MIN_ENGLISH_TEXT_LENGHT) {
                    Sentence sentence = Sentence.newInstance();
                    sentence.setSource(str);
                    if(isTranslate) {
                        sentence.setTranslation(translate(str)); 
                    }
                    section.getSentences().add(sentence);
                }
            }
        }
        if(section.getSentences().isEmpty()) {
            if(text.length() > MIN_ENGLISH_TEXT_LENGHT) {
                Sentence sentence = Sentence.newInstance();
                sentence.setSource(text);
                if(isTranslate) {
                    sentence.setTranslation(translate(text)); 
                }
                section.getSentences().add(sentence);
            }
        }
        return section;
    }
    
    /**
     * 解析中文段落为一到多个中文句子
     * 
     * @param text 中文段落文本
     * @param isTranslate 是否翻译 (true:需Translatable接口实现中译英的功能， Similarity接口实现为英文相似度)
     * @return
     */
    public Section parseChineseSection(String text, boolean isTranslate) {
        Section section = Section.newInstance();
        if(StringUtils.isBlank(text)) {
            return section;
        }
        text = standardizingText(text, false);
        int startIndex = 0;
        int endIndex = 0;
        for(int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if(ch == '。' || ch == '！' || ch == '？') {
                String str;
                if(i + 1 < text.length() && text.charAt(i + 1) == '”') {
                    endIndex = i + 2;
                } else {
                    endIndex = i + 1;
                }
                str = text.substring(startIndex, endIndex).trim();
                if(StringUtils.isNotBlank(str) && str.length() > MIN_CHINESE_TEXT_LENGHT) {
                    Sentence sentence = Sentence.newInstance();
                    sentence.setSource(str);
                    if(isTranslate) {
                        sentence.setTranslation(translate(str)); 
                    }
                    section.getSentences().add(sentence);
                    startIndex = endIndex;
                }
            }
        }
        endIndex = text.length();
        if(endIndex - startIndex > MIN_CHINESE_TEXT_LENGHT) {
            String str = text.substring(startIndex, endIndex).trim();
            if(StringUtils.isNotBlank(str)) {
                Sentence sentence = Sentence.newInstance();
                sentence.setSource(str);
                if(isTranslate) {
                    sentence.setTranslation(translate(str)); 
                }
                section.getSentences().add(sentence);
            }
        }
        return section;
    }

    /**
     * 标准化文本，去除不合法字符，规范文本格式
     * 
     * @param text
     * @param isEnglish 是否为英文
     * @return
     */
    public String standardizingText(String text, boolean isEnglish) {
        if(StringUtils.isBlank(text)) {
            return StringUtils.EMPTY;
        }
        StringBuilder result = new StringBuilder();
        int left = 0;
        int right = 0;
        if(isEnglish) {
            text = StringUtils.replaceEach(text, enSearchList, enReplacementList);
        } else {
            text = StringUtils.replaceEach(text, zhSearchList, zhReplacementList);
        }
        for(int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if(ch != 12288 && (TextUtils.isChinese(ch) || TextUtils.isEnglish(ch) 
                    || TextUtils.isDigital(ch) || TextUtils.isPunctuation(ch))) {
                if(ch == '(' || ch == '（') {
                    left++;
                    continue;
                }
                if(ch == ')' || ch == '）') {
                    right++;
                    continue;
                }
                if(left == right) {
                    if(isEnglish && !TextUtils.isChinese(ch)) {
                        result.append(ch);
                    } else if(!isEnglish){
                        result.append(ch);
                    }
                }
            }
        }
        return Jsoup.parse(result.toString(), "UTF-8").text();
    }

    /**
     * 切分n句子成m份， n < 20 并且 n >= m
     * 
     * @param list 句子列表
     * @param m 切分份数
     * @return
     */
    public static List<Section> splitSentences(Section section, int m) {
        if (section.size() >= 20 && m <= section.size()) {
            throw new IllegalArgumentException("size : " + section.size() + " , m : " + m);
        }
        CombineIterator iter = new CombineIterator(section, m);
        List<Section> result = Lists.newArrayListWithCapacity(iter.size());
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        return result;
    }

    /**
     * 句子对齐
     * 1. 中英文段落其一为一句，则段落相似则完成句子对齐；
     * 2. 若中英文段落切分句子数相等，则对应句子相似则句子对齐；
     * 3. 若中英文段落切分句子数不相等，则按如下步骤完成句子对齐：
     *  问题描述：在段落对应的前提下，中文段落存在n个句子，英文段落存在m个句子，并且n != m; 输出为中英文句子对应。
     *  例如n>m的情况，算法步骤为：
     *  1). 将中文段落n个句子拆分成m份（考虑所有情况）；
     *  2). 基于长度相关性过滤步骤1当中计算出的所有m份拆分句子组，减少实际匹配次数；
     *  3). 计算过滤之后的m份中文句子中(译)文与英文句子的相似度之和，求解最优中英文句子对应；
     *  4). 过滤中（译)文英文对应的句子之间的相似度小于设定阈值，输出中英文句子对应结果。
     *  
     * @param article
     * @param isTransZh2En 是否为中译英 
     * @return
     */
    public List<Sentence> sentenceAlignment(Article article, boolean isTransZh2En) {
        List<Sentence> result = Lists.newArrayList();
        if(article == null) {
            throw new NullPointerException();
        }
        List<Section> zhSections = article.getZhSections();
        List<Section> enSections = article.getEnSections();
        if(CollectionUtils.isEmpty(zhSections) || CollectionUtils.isEmpty(enSections)) {
            return result;
        }
        if(zhSections.size() < enSections.size()) {
            article = sectionAlignment(article.getZhSections(), article.getEnSections(), false, isTransZh2En);
            article = sectionAlignment(article.getEnSections(), article.getZhSections(), true, isTransZh2En);
        } else {
            article = sectionAlignment(article.getEnSections(), article.getZhSections(), true, isTransZh2En);
            article = sectionAlignment(article.getZhSections(), article.getEnSections(), false, isTransZh2En);
        }
        zhSections = article.getZhSections();
        enSections = article.getEnSections();
        //句子对齐过程中，重组source为中文,translation为英文
        for(int i = 0; i < zhSections.size(); i++) {
            Section zhSection = zhSections.get(i);
            Section enSection = enSections.get(i);
            if(zhSection.size() == 1 || enSection.size() == 1) {
                Sentence sentence = Sentence.newInstance();
                sentence.setSource(zhSection.getSource());
                sentence.setTranslation(enSection.getSource());
                result.add(sentence);
            } else {
                if(zhSection.size() == enSection.size()) {
                    for(int j = 0; j < zhSection.size(); j++) {
                        boolean isSimilar = false;
                        if(isTransZh2En && (similar(zhSection.getSentence(j).getTranslation(), enSection.getSentence(j).getSource()) > SENTENCE_LIMIT)) {
                            isSimilar = true;
                        } else if(!isTransZh2En && similar(enSection.getSentence(j).getTranslation(), zhSection.getSentence(j).getSource()) > SENTENCE_LIMIT) {
                            isSimilar = true;
                        }
                        if(isSimilar) {
                            Sentence sentence = Sentence.newInstance();
                            sentence.setSource(zhSection.getSentence(j).getSource());
                            sentence.setTranslation(enSection.getSentence(j).getSource());
                            result.add(sentence);
                        }
                    }
                } else {
                    Section less = zhSection;
                    Section more = enSection;
                    boolean isLessEnglish = false;
                    if(zhSection.size() > enSection.size()) {
                        less = enSection;
                        more = zhSection;
                        isLessEnglish = true;
                    }
                    //若超出切分数量限制，则不作处理
                    if(less.size() > MAX_SENTENCE_SIZE) {
                        logger.warn(String.format("split size [%s] is too large ! zhSection : %s | enSection : %s", less.size(), zhSection, enSection));
                        continue; 
                    }
                    List<Section> filterList = filterSplitSentences(less, splitSentences(more, less.size()), OFFSET_LIMIT, isLessEnglish);
                    double bestValue = Double.MIN_VALUE;
                    int bestIndex = -1;
                    Map<String, Double> Map = Maps.newHashMap();
                    for(int j = 0; j < filterList.size(); j++) {
                        double value = 0;
                        for(int k = 0; k < less.size(); k++) {
                            CombineSentence sentence = (CombineSentence)filterList.get(j).getSentence(k);
                            String cacheKey = String.format("%s:%s:%s", k, sentence.getStartIndex(), sentence.getEndIndex());
                            Double cacheValue = Map.get(cacheKey);
                            if(isLessEnglish) {
                                if(isTransZh2En) {
                                    cacheValue = (cacheValue == null ? similar(less.getSentence(k).getSource(), sentence.getTranslation()) : cacheValue);
                                } else {
                                    cacheValue = (cacheValue == null ? similar(sentence.getSource(), less.getSentence(k).getTranslation()) : cacheValue);
                                }
                            } else {
                                if(isTransZh2En) {
                                    cacheValue = (cacheValue == null ? similar(sentence.getSource(), less.getSentence(k).getTranslation()) : cacheValue);
                                } else {
                                    cacheValue = (cacheValue == null ? similar(less.getSentence(k).getSource(), sentence.getTranslation()) : cacheValue);
                                }
                            }
                            value += cacheValue;
                            Map.put(cacheKey, cacheValue);
                        }
                        if(bestValue < value) {
                            bestValue = value;
                            bestIndex = j; 
                        }
                    }
                    for(int j = 0; j < less.size() && bestIndex > -1; j++) {
                        Sentence sentence = Sentence.newInstance();
                        Sentence lessSentence = less.getSentence(j);
                        CombineSentence combineSentence = (CombineSentence)filterList.get(bestIndex).getSentence(j);
                        String Key = String.format("%s:%s:%s", j, combineSentence.getStartIndex(), combineSentence.getEndIndex());
                        if(isLessEnglish) {
                            sentence.setSource(combineSentence.getSource());
                            sentence.setTranslation(lessSentence.getSource());
                        } else {
                            sentence.setSource(lessSentence.getSource());
                            sentence.setTranslation(combineSentence.getSource());
                        }
                        Double Value = Map.get(Key);
                        if(Value > SENTENCE_LIMIT) {
                            result.add(sentence);
                        }
                    }
                    Map.clear();
                }
            }
        }
        return result;
    }
    
    /**
     * 1:m 段落对齐
     * 
     * @param one 
     * @param many
     * @param isLessEnglish
     * @return
     */
    private Article sectionAlignment(List<Section> one, List<Section> many, boolean isOneEnglish, boolean isTransZh2En) {
        Article article = Article.newInstance();
        if(CollectionUtils.isEmpty(one) || CollectionUtils.isEmpty(many)) {
            return article;
        }
        List<Section> uniqueOne = Lists.newArrayList();
        Set<Section> uniqueSet = Sets.newHashSet();
        for(int i = 0; i < one.size(); i++) {
            if(!uniqueSet.contains(one.get(i))) {
                uniqueOne.add(one.get(i));
            }
            uniqueSet.add(one.get(i));
        }
        one = uniqueOne;
        uniqueOne = null;
        for(int i = 0; i < one.size(); i++) {
            double bestValue = Double.MIN_VALUE;
            int bestIndex = -1;
            for(int j = i - 1; j < i + 2 && j < many.size(); j++) {
                if(j >= 0) {
                    double value = Double.MIN_VALUE;
                    if(isOneEnglish) {
                        if(isTransZh2En) {
                            value = similar(one.get(i).getSource(), many.get(j).getTranslation());
                        } else {
                            value = similar(many.get(j).getSource(), one.get(i).getTranslation());
                        }
                    } else {
                        if(isTransZh2En) {
                            value = similar(many.get(j).getSource(), one.get(i).getTranslation());
                        } else {
                            value = similar(one.get(i).getSource(), many.get(j).getTranslation());
                        }
                    }
                    if(value > bestValue) {
                        bestValue = value;
                        bestIndex = j;
                    }
                }
            }
            if(bestIndex > -1 && bestValue > SECTION_LIMIT) {
                if(isOneEnglish) {
                    article.getZhSections().add(many.get(bestIndex));
                    article.getEnSections().add(one.get(i));
                } else {
                    article.getEnSections().add(many.get(bestIndex));
                    article.getZhSections().add(one.get(i));
                }
            }
        }
        return article;
    }
    
    /**
     * 根据句子长度相关性过滤切分的句子
     * 
     * @param less
     * @param allList
     * @param limit 最大偏差百分比 （0.0 <= limit <= 1.0）
     * @param isLessEnglish 
     * @return
     */
    private static List<Section> filterSplitSentences(Section less, List<Section> allList, double limit, boolean isLessEnglish) {
        if(limit < 0.0 || limit > 1.0) {
            throw new IllegalArgumentException("limit : " + limit);
        }
        List<Section> result = Lists.newArrayList();
        int sumLen = less.getSourceLength();
        if(sumLen == 0) {
            return result;
        }
        double[] ratio = new double[less.size()];
        for(int i = 0; i < less.size(); i++) {
            ratio[i] = less.getSentence(i).getSource().length() * 1.0 / sumLen;
        }
        for(Section section : allList) {
            sumLen = section.getSourceLength();
            if(sumLen != 0) {
                boolean isUpdate = true;
                for(int i = 0; i < section.size(); i++) {
                    double value = section.getSentence(i).getSource().length() * 1.0 / sumLen;
                    if(Math.abs(value - ratio[i]) > limit) {
                        isUpdate = false;
                        break;
                    }
                }
                if(isUpdate) {
                    result.add(section);
                }
            }
        }
        return result;
    }
}
