package fi.seco.lucene

import org.apache.lucene.analysis.TokenStream
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute
import org.apache.lucene.util.BytesRef
import org.apache.lucene.util.NumericUtils
import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.Analyzer.TokenStreamComponents
import fi.seco.lexical.hfst.HFSTLexicalAnalysisService.WordToResults
import org.apache.lucene.analysis.Tokenizer
import org.apache.lucene.util.IOUtils
import org.apache.lucene.analysis.CharacterUtils
import fi.seco.lexical.combined.CombinedLexicalAnalysisService
import java.util.Locale
import java.util.Collections
import java.util.Collection
import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import org.apache.lucene.store.ByteArrayDataOutput
import org.apache.lucene.util.SmallFloat

class LemmaAnalysisTokenStream(var tokens: Iterable[(Int, String, Iterable[String])] = null, var originalWords: Boolean = true) extends TokenStream {
  
  private val termAttr: CharTermAttribute = addAttribute(classOf[CharTermAttribute])
  private val posAttr: PositionIncrementAttribute = addAttribute(classOf[PositionIncrementAttribute])
  private val offAttr: OffsetAttribute = addAttribute(classOf[OffsetAttribute])
  
  private var wordsIterator: Iterator[(Int, String, Iterable[String])] = null

  override def reset(): Unit = {
    wordsIterator = tokens.iterator
  }
  
  private var analysesIterator: Iterator[String] = Iterator.empty
  
  private var analysisIterator: Iterator[Iterable[String]] = Iterator.empty
  
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
    return true
  }
}

class LemmaTokenizer(locale: Locale, originalWords: Boolean = true, allLemmas: Boolean = false, segmentBaseform: Boolean = false, guessUnknown: Boolean = true, segmentUnknown: Boolean = false, maxEditDistance: Int = 0, depth: Int = 1) extends Tokenizer {
  
  import LemmaAnalyzer._
  
  val analyzer = new CombinedLexicalAnalysisService()
  
  val tokenStream = new LemmaAnalysisTokenStream() {
    override def reset() = {
      LemmaTokenizer.this.reset()
      super.reset()
    }
    override def end() = {
      LemmaTokenizer.this.end()
      super.end()
    }
    override def close() = {
      LemmaTokenizer.this.close()
      super.close()
    }
  }
  
  val arr = new Array[Char](8 * 1024)
  override def reset(): Unit = {
    super.reset()
    val buffer = new StringBuilder();
    var numCharsRead: Int = input.read(arr, 0, arr.length)
    while (numCharsRead != -1) {
      buffer.appendAll(arr, 0, numCharsRead)
      numCharsRead = input.read(arr, 0, arr.length)
    }
    input.close()
    val string = buffer.toString
    tokenStream.originalWords = originalWords
    tokenStream.tokens = analysisToTokenStream(analyzer.analyze(buffer.toString, locale, Collections.EMPTY_LIST.asInstanceOf[java.util.List[String]], segmentBaseform, guessUnknown, segmentUnknown, maxEditDistance, depth), allLemmas)
  }
  
  final override def incrementToken(): Boolean = throw new UnsupportedOperationException("Can't increment")
}

class LemmaAnalyzer(locale: Locale, originalWords: Boolean = true, allLemmas: Boolean = false, segmentBaseform: Boolean = false, guessUnknown: Boolean = true, segmentUnknown: Boolean = false, maxEditDistance: Int = 0, depth: Int = 1) extends Analyzer {

  override def createComponents(fieldName: String): TokenStreamComponents = {
    val tokenizer = new LemmaTokenizer(locale, originalWords, allLemmas, segmentBaseform, guessUnknown, segmentUnknown, maxEditDistance, depth)
    return new TokenStreamComponents(tokenizer, tokenizer.tokenStream)
  }

}

object LemmaAnalyzer {
  
  def analysisToTokenStream(analysis: java.util.List[WordToResults], allLemmas: Boolean = false): Iterable[(Int, String, Iterable[String])] = {
    val wordsToAnalysis = new ArrayBuffer[(Int, String, Iterable[String])] 
    var offset = 0
    for (word <- analysis.asScala)
      if (word.getAnalysis.get(0).getGlobalTags().containsKey("WHITESPACE"))
        offset += word.getWord.length
      else {
        val analyses = new ArrayBuffer[String]
        for (analysis <- word.getAnalysis.asScala) if (allLemmas || analysis.getGlobalTags.containsKey("BEST_MATCH")) {
          var lemma = ""
          for (wordPart <- analysis.getParts.asScala)
            lemma += wordPart.getLemma
          analyses += lemma
        }
        wordsToAnalysis += ((offset, word.getWord, analyses)) 
        offset += word.getWord.length
      }
    wordsToAnalysis
  }
}