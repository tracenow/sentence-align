# sentence-align

网页中英文句子对齐算法

需继承cn.trace.nlp.etl.util.AbstractSentenceAlign抽象类，实现Similarity、Translatable接口。

Translatable接口可通过谷歌、有道、百度等多种机器翻译接口实现，Similarity接口可自定义实现中文或者英文相似度算法。
