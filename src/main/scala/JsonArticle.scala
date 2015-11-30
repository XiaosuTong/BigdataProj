/**
 * Created by tongx on 11/13/15.
 */
class JsonArticles (_section:String, _links: List[String], _file : String) extends Serializable {

  import java.util.Calendar
  import org.jsoup.{Connection, Jsoup}
  import org.jsoup.nodes.{Element, Document}
  import scala.io.Source
  import net.liftweb.json._
  import net.liftweb.json.JsonDSL._
  import scala.util.{Failure, Success, Try}
  import com.tongx.crawlcorruption._
  import scala.sys.process._

  val section: String = _section

  var cmd = "hdfs getconf -nnRpcAddresses"
  val namenode = "hdfs://" + cmd.!! .toString.trim
  var user = System.getProperty("user.name")
  var dir = "/user/" + user + "/BigDataProj/articles/" + section
  val hdfsPath: String = namenode + dir

  def getLinks(file: String): List[String] = file == "" match {
    case true => _links
    case false => Source.fromFile(_file).getLines().toList
  }

  val Links:List[String] = getLinks(_file)

  def getHrefDoc(url: String): Document = {
    val userAgentString: String = "Mozilla/5.0 (X11; Linux x86_64) " + "AppleWebKit/535.21 (KHTML, like Gecko) " + "Chrome/45.0.2454.101 Safari/535.21"
    Jsoup.connect(url).userAgent(userAgentString).timeout(10*1000).get()
  }

  def getArticleContent(doc: Document): String = {
    if(doc.getElementById("articleBody") != null) {
      doc.getElementById("articleBody").getElementsByTag("p").text() // != null
    } else if(doc.getElementsByClass("story-body-text").toString != ""){
      doc.getElementsByClass("story-body-text").text() // != ""
    } else {
      doc.getElementsByClass("articleBody").text()
    }
  }

  def getArticleTitle(doc: Document): String = {
    if(doc.getElementsByTag("nyt_headline").toString != "") {
      doc.getElementsByTag("nyt_headline").text()
    } else if(doc.select("h1[class=story-heading]").toString != "") {
      doc.select("h1[class=story-heading]").text()
    } else {
      doc.select("h1[class=articleHeadline]").text()
    }
  }

  def getArticleTime(doc: Document): String = {
    if(doc.getElementsByClass("timestamp").toString != "") {
      doc.getElementsByClass("timestamp").text().drop(11)
    } else if(doc.getElementsByClass("dateline").attr("datetime") != "") {
      doc.getElementsByClass("dateline").attr("datetime")
    } else{
      doc.getElementsByClass("dateline").text().drop(11)
    }
  }

  def saveAsJson(link: String): String = {
    val link_doc: Document = getHrefDoc(link)
    val fetchTime = Calendar.getInstance().getTime.toString
    val crawledArticle = new CrawledCorruptionArticle(
      fetchTime,
      getArticleTime(link_doc),
      getArticleTitle(link_doc),
      getArticleContent(link_doc),
      section
    )
    val json =
       ("fetchTime" -> crawledArticle.fetchTime) ~
       ("time" -> crawledArticle.time) ~
       ("title" -> crawledArticle.title) ~
       ("content" -> crawledArticle.content) ~
       ("label" -> crawledArticle.label)
    compact(render(json))
  }

  def pageFilter(link: String): Boolean ={
    Try {
      val link_doc: Document = getHrefDoc(link)
      link_doc.getElementById("articleBody") != null | link_doc.getElementsByClass("story-body-text").toString != "" | link_doc.getElementsByClass("articleBody").toString !=""
    } match {
      case Success(x) => x
      case Failure(e) => println("Exception while parsing: " + e); false
    }
  }

  def saveOnHDFS(partition: Int): Unit = {
    val linksRDD = sc.parallelize(Links, partition)
    linksRDD.filter(x => pageFilter(x)).map(x => saveAsJson(x)).saveAsTextFile(hdfsPath)
  }

  def parseString(article: String) = {
    implicit val formats = DefaultFormats
    val articleJson = parse(article)
    articleJson.extract[CrawledCorruptionArticle]
  }

  def readFromHDFS() =  {
    sc.textFile(hdfsPath).map(x => parseString(x))
  }

}


