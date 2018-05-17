package fi.seco.lucene

import java.util.{Collections, Locale}

import fi.seco.lexical.hfst.HFSTLexicalAnalysisService.WordToResults
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import org.apache.lucene.analysis.core.LowerCaseFilter
import org.apache.lucene.analysis.tokenattributes.{CharTermAttribute, OffsetAttribute, PositionIncrementAttribute}
import org.apache.lucene.analysis.{Analyzer, TokenStream, Tokenizer}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

class LemmaAnalysisTokenStream(var tokens: Iterable[(Int, String, Iterable[String])] = null, var originalWords: Boolean = true) extends TokenStream {
  
  private val termAttr: CharTermAttribute = addAttribute(classOf[CharTermAttribute])
  private val posAttr: PositionIncrementAttribute = addAttribute(classOf[PositionIncrementAttribute])
  private val offAttr: OffsetAttribute = addAttribute(classOf[OffsetAttribute])
  
  private var wordsIterator: Iterator[(Int, String, Iterable[String])] = _

  override def reset(): Unit = {
    wordsIterator = tokens.iterator
  }
  
  private var analysesIterator: Iterator[String] = Iterator.empty

  private var startOffset = 0
  private var endOffset = 0

  final override def incrementToken(): Boolean = {
    clearAttributes()
    val analysisToken = if (!analysesIterator.hasNext) { // end of lemmas
      if (!wordsIterator.hasNext) return false // end of words
      val n = wordsIterator.next // next word
      analysesIterator = n._3.iterator
      posAttr.setPositionIncrement(1)
      startOffset = n._1
      val word = n._2
      endOffset = startOffset + word.length
      if (originalWords) word else analysesIterator.next
    } else {
      posAttr.setPositionIncrement(0)
      analysesIterator.next
    }
    offAttr.setOffset(startOffset, endOffset)
    termAttr.append(analysisToken)
    true
  }
}

class LemmaTokenizer(locale: Locale, originalWords: Boolean = true, allLemmas: Boolean = false, guessUnknown: Boolean = true, maxEditDistance: Int = 0, depth: Int = 1, nonWords: Boolean = false, unique: Boolean = true) extends Tokenizer {
  
  import LemmaAnalyzer._
  import MorphologicalAnalyzer.analyzer
  
  val tokenStream = new LemmaAnalysisTokenStream() {
    override def reset(): Unit = {
      LemmaTokenizer.this.reset()
      super.reset()
    }
    override def end(): Unit = {
      LemmaTokenizer.this.end()
      super.end()
    }
    override def close(): Unit = {
      LemmaTokenizer.this.close()
      super.close()
    }
  }
  
  val arr = new Array[Char](8 * 1024)
  override def reset(): Unit = {
    super.reset()
    val buffer = new StringBuilder()
    var numCharsRead: Int = input.read(arr, 0, arr.length)
    while (numCharsRead != -1) {
      buffer.appendAll(arr, 0, numCharsRead)
      numCharsRead = input.read(arr, 0, arr.length)
    }
    input.close()
    tokenStream.originalWords = originalWords
    tokenStream.tokens = analysisToTokenStream(analyzer.analyze(buffer.toString, locale, Collections.EMPTY_LIST.asInstanceOf[java.util.List[String]], false, guessUnknown, false, maxEditDistance, depth), allLemmas, nonWords, unique)
  }
  
  final override def incrementToken(): Boolean = throw new UnsupportedOperationException("Can't increment")
}

class LemmaAnalyzer(locale: Locale, originalWords: Boolean = true, allLemmas: Boolean = false, guessUnknown: Boolean = true, maxEditDistance: Int = 0, depth: Int = 1, nonWords: Boolean = false, lowercase: Boolean = true, unique: Boolean = true) extends Analyzer {

  override def createComponents(fieldName: String): TokenStreamComponents = {
    val tokenizer = new LemmaTokenizer(locale, originalWords, allLemmas, guessUnknown, maxEditDistance, depth, nonWords, unique)
    var tokenStream: TokenStream = tokenizer.tokenStream
    if (lowercase) tokenStream = new LowerCaseFilter(tokenStream)
    new TokenStreamComponents(tokenizer, tokenStream)
  }

}

object LemmaAnalyzer {
  
  def analysisToTokenStream(analysis: java.util.List[WordToResults], allLemmas: Boolean = false, nonWords: Boolean = false, unique: Boolean = true): Iterable[(Int, String, Iterable[String])] = {
    val wordsToAnalysis = new ArrayBuffer[(Int, String, Iterable[String])] 
    var offset = 0
    for (word <- analysis.asScala)
      if (word.getAnalysis.get(0).getGlobalTags.containsKey("WHITESPACE") || (!nonWords && word.getAnalysis.asScala.exists(_.getParts.asScala.exists(wp => Option(wp.getTags.get("UPOS")).exists(_.contains("PUNCT"))))))
        offset += word.getWord.length
      else {
        val analyses = new ArrayBuffer[String]
        val seen = if (unique) new mutable.HashSet[String] else null
        for (analysis <- word.getAnalysis.asScala) if (allLemmas || analysis.getGlobalTags.containsKey("BEST_MATCH")) {
          var lemma = ""
          for (wordPart <- analysis.getParts.asScala)
            lemma += wordPart.getLemma
          if (!unique || seen.add(lemma))
            analyses += lemma
        }
        wordsToAnalysis += ((offset, word.getWord, analyses)) 
        offset += word.getWord.length
      }
    wordsToAnalysis
  }
}
