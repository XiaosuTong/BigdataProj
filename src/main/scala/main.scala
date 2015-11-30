/**
 * Created by tongx on 11/20/15.
 */

//add Jar files for each worker
sc.addJar("CrawledCorruptionArticle.jar")

//////////////////////////////////////////////////////////////////
// First Step: get url of all articles, and save in a text file //
//////////////////////////////////////////////////////////////////
val links = new Links("politics", 2001, false)
val mylinks = links.getAllLinks()
/*
A headless browser is a web browser without a graphical user interface.
Headless browsers provide automated control of a web page in an environment similar to popular web browsers,
but are executed via a command line interface or using network communication.

PhantomJS is a scripted, headless browser used for automating web page interaction. PhantomJS provides a
JavaScript API enabling automated navigation, screenshots, user behavior

PhantomJS mimics legitimate user traffic and behaviors
*/
links.saveAsTextFile("./", mylinks)


///////////////////////////////////////////////////////////////////////////////////////////
// Second Step: Download article from each url, save each article as JSON string on HDFS //
///////////////////////////////////////////////////////////////////////////////////////////
val test = new JsonArticles("politics", List(), "NYTimesLinks_politics.txt")
// alternate //
val test_alter = new JsonArticles("politics", mylinks, "")

/*
parsing the url using jsoup package, save article information as an object named CrawledCorruptionArticle
and then save the object as Json string on HDFS
*/
test.saveOnHDFS(100)

///////////////////////////////////////////////////////////////////////////////////////////////////
// Third Step: read in JSON string from HDFS and tokenize to single words and order by frequency //
///////////////////////////////////////////////////////////////////////////////////////////////////
/* readback is RDD of CrawledCorruptionArticle */
val readback = test.readFromHDFS().map(_.tokenize(true))


val dictionary = new FBIarticles

val dict = dictionary.topWords("/user/panc/stat598bigdata/fbi-public-corruption-articles", 1000)

dictionary.dictToTextFile("./", dict)