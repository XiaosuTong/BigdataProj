/**
 * Created by tongx on 11/20/15.
 */

import com.tongx.crawlcorruption.CrawledCorruptionArticle
import net.liftweb.json._

sc.addJar("CrawledCorruptionArticle.jar")
val test = new JsonArticles("tmp", List(), "NYTimesLinks_tmp.txt")


val rst = sc.textFile(test.hdfsPath).
  map(x => test.tokenize(x)).
  map( x => (x._1, test.filterStopWords(x._2))).
  flatMap(x => test.words(x._1, x._2)).
  reduceByKey(_+_).
  map(words => (words._1._1, (words._1._2, words._2))).
  groupByKey().
  map(x => (x._1, x._2.toArray.sortBy(y => (-y._2, y._1)))).
  map(x => (x._1, x._2.take(5))).saveAsObjectFile("/user/tongx/BigDataProj/token/tmp")

sc.objectFile[Array[(String, Array[(String, Int)])]]("/user/tongx/BigDataProj/token/tmp")



/*
TODO:
1) save corruption dictionary words to Hardrvie as Json string     (done)
2) write a function which input is HDFS path of articles, output is RDD[my CorruptionArticle]  (done)
3) redefine my CorruptionArticle class which include a method named tokenize  (done)
*/

val hdfsPath: String = "/user/tongx/BigDataProj/articles/tmp"

def parseString(article: String) = {
  implicit val formats = DefaultFormats
  val articleJson = parse(article)
  articleJson.extract[CrawledCorruptionArticle]
}


sc.textFile(hdfsPath).map(x => parseString(x)).map(_.tokenize(true)).take(10)

