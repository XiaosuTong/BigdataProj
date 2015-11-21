
/**
 * Created by tongx on 11/13/15.
 */

class Links(_section: String, _start_year: Int, _allRst: Boolean) {

  import org.openqa.selenium.remote.DesiredCapabilities
  import org.openqa.selenium.phantomjs.PhantomJSDriver
  import scala.collection.JavaConversions._
  import java.io._

  def allRst(x: Boolean): String = x match {
    case true => "/allresults"
    case false => "/document_type%3A%22article%22"
  }

  def getSomeLinks(driver: PhantomJSDriver): List[String] = {
    var nextpage: Boolean = true
    var partAllLinks: List[String] = List()
    var iter: Int = 0
    while (nextpage) {
      iter = iter + 1
      println(s"In iteration $iter")
      println("getting new links... \n")
      val newlinks: List[String] = driver.findElementsByCssSelector("#searchResults a").map(x => x.getAttribute("href")).toList.distinct
      println(newlinks.length)
      println("appending new links to allLinks \n")
      partAllLinks = List.concat(partAllLinks, newlinks)
      nextpage = driver.findElementsByCssSelector(".next").nonEmpty
      if (nextpage & iter <= 101) {
        driver.findElementByCssSelector(".next").click()
        Thread.sleep(5000)
      }
      if (iter >= 101) {
        nextpage = false
      }
    }
    partAllLinks
  }

  val docType: String = allRst(_allRst)
  val phantom_path: String = "/home/tongx/Github/phantomjs/bin/phantomjs" //on Hathi
  val section: String = _section
  var allLinks:List[String] = List()

  def getAllLinks(): List[String] = {
    for (year <- _start_year to 2015; month <- List(1, 4, 7, 10)) {
      val capabilities = new DesiredCapabilities()
      capabilities.setCapability("phantomjs.binary.path", phantom_path)
      val myDriver = new PhantomJSDriver(capabilities)
      val start_Date: String = year + "%02d".format(month) + "01"
      val end_Date: String = year + "%02d".format(month + 2) + "30"
      val mainPage: String = "http://query.nytimes.com/search/sitesearch/#/*/from" + start_Date + "to" + end_Date + docType + "/1/allauthors/relevance/" + section.capitalize
      myDriver.get(mainPage)
      val someLinks: List[String] = getSomeLinks(myDriver)
      allLinks = List.concat(allLinks, someLinks)
      myDriver.close()
      myDriver.quit()
    }
    val total: Int = allLinks.length
    println(s"Crawled $total links in total.")
    allLinks
  }

  def saveAsTextFile(localPath: String, Links: List[String]): Unit = {
    val file:String = "NYTimesLinks_" + section + ".txt"
    val writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(localPath + file)))
    for (x <- Links) {
      writer.write(x + "\n")  // however you want to format it
    }
    writer.close()
  }

}
