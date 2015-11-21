/**
 * Created by Xiaosu on 11/05/15.
 */

import java.util.Calendar
import org.jsoup.{Connection, Jsoup}
import org.jsoup.nodes.{Element, Document}
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.phantomjs.PhantomJSDriver
import org.openqa.selenium.Platform
import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}
import java.io._
import scala.io.Source
import net.liftweb.json._
import net.liftweb.json.JsonDSL._
import scala.util.{Failure, Success, Try}

object parallelCrawl extends App {

  val section: String = "health"
  val file:String = "NYTimesLinks_" + section + ".txt"
  val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))
  val phantom_path: String = "/home/tongx/Github/phantomjs/bin/phantomjs" //on Hathi
  //  val path: String = "/home/tongx/Github/others/phantomjs/bin/phantomjs"  \\on my local laptop
  
  def createMainpageDriver(url: String, path_to_phantomjs: String): PhantomJSDriver = {

    val capabilities = new DesiredCapabilities()
    // Set PhantomJS Path
    capabilities.setCapability("phantomjs.binary.path", path_to_phantomjs)
    var driver = new PhantomJSDriver(capabilities)
    driver.get(url)
    driver

  }

  def stopMainpageDriver(driver: PhantomJSDriver): Unit = {

    driver.close()
    driver.quit()

  }

  def getAllLinks(driver: PhantomJSDriver): List[String] = {

    var nextpage: Boolean = true
    var allLinks: List[String] = List()
    var iter: Int = 0

    while (nextpage) {

      iter = iter + 1
      println(s"In iter $iter")
      println("getting newlinks... \n")
      //driver.getCurrentUrl()

      val newlinks: List[String] = driver.findElementsByCssSelector("#searchResults a").map(x => x.getAttribute("href")).toList.distinct

      println(newlinks.length)

      println("appending new links to allLinks \n")

      allLinks = List.concat(allLinks, newlinks)

      //contents.map {_.length}

      nextpage = driver.findElementsByCssSelector(".next").nonEmpty

      if (nextpage & iter <= 101) {
        driver.findElementByCssSelector(".next").click()
        Thread.sleep(5000)
      }

      if (iter >= 101) {
        nextpage = false
      }

    }
    allLinks

  }

//  def myfun(year: Int): String = {
//
//    var section: String = "politics"
//    var start_Date: String = year + "0101"
//    var end_Date: String = year + "1231"
//    var NYTimesurl: String = "http://query.nytimes.com/search/sitesearch/#/*/from" + start_Date + "to" + end_Date + "/allresults/1/allauthors/relevance/" + section.capitalize
//
//    NYTimesurl
//  }
//

  for(year <- 2007 to 2015; month <- List(1,4,7,10)) {
    val start_Date: String = year + "%02d".format(month) + "01"
    val end_Date: String = year + "%02d".format(month+2) + "30"
    //val NYTimesurl: String = "http://query.nytimes.com/search/sitesearch/#/*/from" + start_Date + "to" + end_Date + "/allresults/1/allauthors/relevance/" + section.capitalize
    val NYTimesurl: String = "http://query.nytimes.com/search/sitesearch/#/*/from" + start_Date + "to" + end_Date + "/document_type%3A%22article%22/1/allauthors/relevance/" + section.capitalize
    val NYdriver: PhantomJSDriver = createMainpageDriver(NYTimesurl, phantom_path)
    val allLinks: List[String] = getAllLinks(NYdriver)

    for (x <- allLinks) {
      writer.write(x + "\n")  // however you want to format it
    }
    stopMainpageDriver(NYdriver)

  }

  writer.close()


  /* Function for processing each article */
  //TODO can create object extend of Document which including following methods
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

  val links:List[String] = Source.fromFile(file).getLines.toList
  //val links:List[String] = Source.fromFile("/home/tongx//Github/IdeaProjects/BigdataProj/NYTimesLinks.txt").getLines.toList


  def saveContents(link: String): String = {
    case class CrawledCorruptionArticle(fetchTime: String, time: String, title: String, content: String, label: String = section)
    val link_doc: Document = getHrefDoc(link)
    val fetchTime = Calendar.getInstance().getTime().toString
    val crawledArticle = new CrawledCorruptionArticle(
      fetchTime,
      getArticleTime(link_doc),
      getArticleTitle(link_doc),
      getArticleContent(link_doc),
      section
    )
    val json = 
      ( 
        ("fetchTime" -> crawledArticle.fetchTime) ~
        ("time" -> crawledArticle.time) ~
        ("title" -> crawledArticle.title) ~
        ("content" -> crawledArticle.content) ~
        ("label" -> crawledArticle.label)
      )
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

  var linksRDD = sc.parallelize(links, 100)
  val hdfsPath: String = "hdfs://" + "hathi-adm.rcac.purdue.edu:8020" + "/user/tongx/BigDataProj/articles/" + section


  linksRDD.filter(x => pageFilter(x)).map(x => saveContents(x)).saveAsTextFile(hdfsPath)

  case class CrawledCorruptionArticle(fetchTime: String, time: String, title: String, content: String, label: String = section)


  def parseString(article: String) = {
    implicit val formats = DefaultFormats
    val articleJson = parse(article)
    articleJson.extract[CrawledCorruptionArticle]
  }

  val textFile = sc.textFile(hdfsPath).map(x => parseString(x)).take(1)


}
