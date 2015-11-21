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

  val section: String = _section
  val Links:List[String] = getLinks(_file)
  val hdfsPath: String = "hdfs://" + "hathi-adm.rcac.purdue.edu:8020" + "/user/tongx/BigDataProj/articles/" + section

  def getLinks(file: String): List[String] = (file == "") match {
    case true => _links
    case false => Source.fromFile(_file).getLines.toList
  }

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
    val fetchTime = Calendar.getInstance().getTime().toString
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

  def readFromHDFS(sample: Int, asRDD: Boolean) = asRDD match {
    case true => sc.textFile(hdfsPath).map(x => parseString(x))
    case false => sc.textFile(hdfsPath).map(x => parseString(x)).take(sample)
  }

  def tokenize(article: String) = {
    val time = parseString(article).time
    val title = parseString(article).title
    val content = parseString(article).content
    (title, content.replaceAll("[^A-Za-z0-9 ]+", "").toLowerCase().split(" +").toList)
  }

  def filterStopWords(target: List[String]) = {
    object Stopwords {
      val EN = """a
a's
able
about
above
according
accordingly
across
actually
after
afterwards
again
against
ain't
all
allow
allows
almost
alone
along
already
also
although
always
am
among
amongst
an
and
another
any
anybody
anyhow
anyone
anything
anyway
anyways
anywhere
apart
appear
appreciate
appropriate
are
aren't
around
as
aside
ask
asking
associated
at
available
away
awfully
b
be
became
because
become
becomes
becoming
been
before
beforehand
behind
being
believe
below
beside
besides
best
better
between
beyond
both
brief
but
by
c
c'mon
c's
came
can
can't
cannot
cant
cause
causes
certain
certainly
changes
clearly
co
com
come
comes
concerning
consequently
consider
considering
contain
containing
contains
corresponding
could
couldn't
course
currently
d
definitely
described
despite
did
didn't
different
do
does
doesn't
doing
don't
done
down
downwards
during
e
each
edu
eg
eight
either
else
elsewhere
enough
entirely
especially
et
etc
even
ever
every
everybody
everyone
everything
everywhere
ex
exactly
example
except
f
far
few
fifth
first
five
followed
following
follows
for
former
formerly
forth
four
from
further
furthermore
g
get
gets
getting
given
gives
go
goes
going
gone
got
gotten
greetings
h
had
hadn't
happens
hardly
has
hasn't
have
haven't
having
he
he's
hello
help
hence
her
here
here's
hereafter
hereby
herein
hereupon
hers
herself
hi
him
himself
his
hither
hopefully
how
howbeit
however
i
i'd
i'll
i'm
i've
ie
if
ignored
immediate
in
inasmuch
inc
indeed
indicate
indicated
indicates
inner
insofar
instead
into
inward
is
isn't
it
it'd
it'll
it's
its
itself
j
just
k
keep
keeps
kept
know
knows
known
l
last
lately
later
latter
latterly
least
less
lest
let
let's
like
liked
likely
little
look
looking
looks
ltd
m
mainly
many
may
maybe
me
mean
meanwhile
merely
might
more
moreover
most
mostly
much
must
my
myself
n
name
namely
nd
near
nearly
necessary
need
needs
neither
never
nevertheless
new
next
nine
no
nobody
non
none
noone
nor
normally
not
nothing
novel
now
nowhere
o
obviously
of
off
often
oh
ok
okay
old
on
once
one
ones
only
onto
or
other
others
otherwise
ought
our
ours
ourselves
out
outside
over
overall
own
p
particular
particularly
per
perhaps
placed
please
plus
possible
presumably
probably
provides
q
que
quite
qv
r
rather
rd
re
really
reasonably
regarding
regardless
regards
relatively
respectively
right
s
said
same
saw
say
saying
says
second
secondly
see
seeing
seem
seemed
seeming
seems
seen
self
selves
sensible
sent
serious
seriously
seven
several
shall
she
should
shouldn't
since
six
so
some
somebody
somehow
someone
something
sometime
sometimes
somewhat
somewhere
soon
sorry
specified
specify
specifying
still
sub
such
sup
sure
t
t's
take
taken
tell
tends
th
than
thank
thanks
thanx
that
that's
thats
the
their
theirs
them
themselves
then
thence
there
there's
thereafter
thereby
therefore
therein
theres
thereupon
these
they
they'd
they'll
they're
they've
think
third
this
thorough
thoroughly
those
though
three
through
throughout
thru
thus
to
together
too
took
toward
towards
tried
tries
truly
try
trying
twice
two
u
un
under
unfortunately
unless
unlikely
until
unto
up
upon
us
use
used
useful
uses
using
usually
uucp
v
value
various
very
via
viz
vs
w
want
wants
was
wasn't
way
we
we'd
we'll
we're
we've
welcome
well
went
were
weren't
what
what's
whatever
when
whence
whenever
where
where's
whereafter
whereas
whereby
wherein
whereupon
wherever
whether
which
while
whither
who
who's
whoever
whole
whom
whose
why
will
willing
wish
with
within
without
won't
wonder
would
would
wouldn't
x
y
yes
yet
you
you'd
you'll
you're
you've
your
yours
yourself
yourselves
z
zero"""
    }
    val stopWords = Stopwords.EN.split("\n").toList
    target.filterNot(xInstance => stopWords.exists(yInstance => yInstance == xInstance))
  }

  def words(title: String, bagOfWords: List[String]) = {
    bagOfWords.map(x => ((title, x), 1))
  }

}


