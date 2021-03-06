#!/usr/bin/Rscript
#PEcAn
#data
#xml
#pecan.zip, pecan.nc

#send all output to stdout (incl stderr)
sink(stdout(), type="message")

# get command line arguments
args <- commandArgs(trailingOnly = TRUE)
if (length(args) < 2) {
  allargs <- commandArgs(trailingOnly = FALSE)
  myCommand <- sub('--file=', '', allargs[grep('--file=', allargs)])
  print(paste0("Usage:    ", myCommand, " xml_Input_File  Output_File [tempDirectory] [cacheDirectory]"))
  print(paste0("Example1: ", myCommand, " US-Dk3.xml US-Dk3.pecan.nc [/tmp/watever] [/tmp/cache]"))
  print(paste0("Example2: ", myCommand, " US-Dk3.xml US-Dk3.pecan.zip [/tmp/watever] [/tmp/cache]"))
  q()
}

# load required libraries
library(XML)
library(RPostgreSQL)
library(PEcAn.data.atmosphere)
library(PEcAn.DB)

# 1st argument is the input xml file
input <- XML::xmlToList(xmlParse(args[1]))

# 2nd argument is the output file
outputfile <- args[2]

# 3rd argument is the temp folder
ifelse(length(args) > 2, tempDir <- args[3], tempDir <- ".")

# 4th argument is the cachefolder
ifelse(length(args) > 3, cacheDir <- args[4], cacheDir <- tempDir)
cacheDir <- "/home/polyglot/cache/PEcAn"

dbparams <- list(user = "bety", dbname = "bety", password="bety", host="localhost")
# variables definitioni
site_lat   <- ifelse(is.null(input$lat), NA, input$lat)
site_lon   <- ifelse(is.null(input$lon), NA, input$lon)

#connect DB and get site name
con      <- db.open(dbparams)
#query site based on location
site <- db.query(paste0("SELECT id, sitename AS name FROM sites WHERE geometry = ST_GeogFromText('POINT(", site_lon, " ", site_lat, ")')"),con)
if(length(site) < 0){
  #query site based on name
  site <- db.query(paste0("SELECT id, sitename AS name FROM sites WHERE sitename LIKE '%", input$site, "%'"),con)
}
if(length(site) < 0){
  #insert site info
  quit(status=-1)
} else {
  #remove multiple entries. 
  site <-list(id = site$id[1], name = site$name[1])
}


mettype    <- ifelse(is.null(input$type), 'CRUNCEP', input$type)
input_met <- list(username = "pecan", source = mettype)
start_date <- input$start_date
end_date   <- input$end_date
host <- list(name = "localhost")


print("Using met.process to download files")
outfile_met <- met.process(site, input_met, start_date, end_date, NULL, host, dbparams, cacheDir)

# get start/end year code works on whole years only
start_year <- lubridate::year(start_date)
end_year <- lubridate::year(end_date)

# if more than 1 year for *.pecan.nc, or zip specified, zip result
if (grepl("\\.zip$", outputfile) || (end_year - start_year > 1) && grepl("\\.pecan.nc$", outputfile)) {
  # folder for files with gapfilling 
  folder  <- dirname(outfile_met)
  outname <- basename(outfile_met)
  # get list of files we need to zip by matching years, may need matching outname
  files <- c()
  for(year in start_year:end_year) {
    files <- c(files, file.path(folder, list.files(folder, pattern = paste0("*", year, "*"))))
  }
  if(length(files)==0){
    # convert csv file that doesn't have year in title to .nc. this is the special case for AmeriFluxLBL
    files <- file.path(folder, list.files(folder, pattern = paste0(outname, "*")))
    bety <- list(user='bety', password='bety', host='localhost', dbname='bety', driver='PostgreSQL', write=TRUE, "con" = con)
    # get file formt csv
    format <- query.format.vars(input.id=2000000128,bety = bety)
    outfolder <- paste0(cacheDir, "/CF")
    if (!file.exists(outfolder)) {
      dir.create(outfolder, showWarnings = FALSE, recursive = TRUE)
    }
    CF <- met2CF.csv(folder, outname, outfolder, start_date, end_date, format)
    files <- CF$file
  } 
  # put the input XML in zip. not necessary. 
  # files <- c(files, args[1])
  # use intermediate file so it does not get marked as done until really done
  dir.create(tempDir, showWarnings=FALSE, recursive=TRUE)
  zipfile <- file.path(tempDir, "temp.zip")
  zip(zipfile, files, extras="-j")
  # move file should be fast
  file.rename(zipfile, outputfile)
} else {
  if(!file.exists(outfile_met)){
    outfile_met = paste0(outfile_met, ".", start_year, ".nc")
  }
  file.link(outfile_met, outputfile)
}

db.close(con)


