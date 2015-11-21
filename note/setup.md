## Installing and running CouchDB
```{r, engine='bash'}
# install the ppa-finding tool
# for 12.04 release
sudo apt-get install python-software-properties -y
# for 14.04 release
sudo apt-get install software-properties-common -y
# add the ppa
sudo add-apt-repository ppa:couchdb/stable -y
# update cached list of packages
sudo apt-get update -y
# remove any existing couchdb binaries
sudo apt-get remove couchdb couchdb-bin couchdb-common -yf
# see my shiny goodness - note the version number displayed and ensure its what you expect
sudo apt-get install -V couchdb
sudo start couchdb
```

## Installing PhantomJS
From the link: http://phantomjs.org/build.html
```{r, engine='bash'}
sudo apt-get install build-essential g++ flex bison gperf ruby perl \
  libsqlite3-dev libfontconfig1-dev libicu-dev libfreetype6 libssl-dev \
  libpng-dev libjpeg-dev python libx11-dev libxext-dev
```
The launch the build
```{r, engine='bash'}
git clone git://github.com/ariya/phantomjs.git
cd phantomjs
git checkout 2.0
./build.sh
```
