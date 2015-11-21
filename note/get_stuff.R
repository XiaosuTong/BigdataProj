


start_phantom <- function() {
  library(RSelenium)
  library(rvest)
  library(magrittr)
  library(stringr)

  # start phantomjs
  pJS <- phantom()
  Sys.sleep(5) # give the binary a moment

  remDr <- remoteDriver(browserName = 'phantomjs')
  remDr$open()

  list(remDr = remDr, pJS = pJS)
}

download_search_links <- function(remDr, pageUrl) {

  wait_till_loaded <- function() {
    waitMore <- TRUE
    while (waitMore) {
      waitMore <- tryCatch({
        Sys.sleep(0.01)
        remDr$findElement(using = "css selector", "#searchResults a")
        FALSE
      }, error = function(e) {
        cat("waiting...\n")
        TRUE
      })

    }
    invisible(TRUE)
  }

  cat("loading page: ", pageUrl, "\n", sep = "")
  # navigate to page and let it load completely.
  # will return when loaded
  remDr$navigate(nyTimesUrl)

  hasNextLink <- TRUE
  linkList <- list()
  iteration <- 1
  while(hasNextLink) {

    cat("loading search page ", iteration, ": ", remDr$getCurrentUrl()[[1]], "\n", sep = "")

    Sys.sleep(0.01)
    wait_till_loaded()

    cat("results available in iteration ", iteration, "\n", sep = "")
    # retrieve processed page source
    Sys.sleep(0.01)
    pageSource <- remDr$getPageSource()[[1]]

    cat("Getting Page Links\n")
    links <- read_html(pageSource) %>%
      html_nodes("#searchResults a") %>%
      html_attr("href") %>%
      unique()

    print(links)
    linkList[[length(linkList) + 1]] <- links

    cat("Checking for next button\n")
    hasNextLink <- tryCatch({
      Sys.sleep(0.01)
      remDr$findElement(using = "css selector", ".next")
      TRUE
    }, error = function(e) {
      FALSE
    })
    print("Done with Next")

    if (hasNextLink) {
      cat("Transitioning to next page\n")
      Sys.sleep(0.01)
      remDr$findElement(using = "css selector", ".next")$clickElement()
      iteration <- iteration + 1
    }

  }

  linkList
}

stop_phantom <- function(phantom) {
  # close the PhantomJS process
  phantom$remDr$close()
  phantom$pJS$stop()
}







nyTimesUrl <-
"http://query.nytimes.com/search/sitesearch/?action=click&contentCollection&region=TopBar&WT.nav=searchWidget&module=SearchSubmit&pgtype=Homepage#/barret/365days/"

phantom <- start_phantom()

links1 <- download_search_links(phantom$remDr, nyTimesUrl)
links2 <- download_search_links(phantom$remDr, nyTimesUrl)
links3 <- download_search_links(phantom$remDr, nyTimesUrl)
links4 <- download_search_links(phantom$remDr, nyTimesUrl)
links5 <- download_search_links(phantom$remDr, nyTimesUrl)
links6 <- download_search_links(phantom$remDr, nyTimesUrl)
# download_search_links(phantom$remDr, nyTimesUrl)
# download_search_links(phantom$remDr, nyTimesUrl)
# download_search_links(phantom$remDr, nyTimesUrl)

stop_phantom(phantom)
