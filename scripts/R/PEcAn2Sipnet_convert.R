#!/usr/bin/Rscript
#PEcAn2Sipnet
#data
#cf-nc,cf-nc.zip
#clim

# get command line arguments
args <- commandArgs(trailingOnly = TRUE)

filename <-basename(args[1])
underPos <-which(strsplit(filename, "")[[1]]=="_")[1]
isANumber=substr(filename,start=1,stop=underPos-1)
if (  !suppressWarnings(!is.na(as.numeric(isANumber)))   ) {
    underPos <- 0
}

if (length(args) < 2) {
    myCommand <- substr(commandArgs()[4],10,1000000L)
    print(paste0("Usage:   ", myCommand, " cf-nc_Input_File climOutputDir "))
    print(paste0("Example1: ", myCommand, " US-Dk3-2001-2003.cf-nc US-Dk3-2001-2003.clim-nc     [/tmp/watever] "))
    print(paste0("Example2: ", myCommand, " US-Dk3-2001-2003.cf-nc.zip US-Dk3-2001-2003.clim-nc [/tmp/watever] "))
    q()
} else {
    dotPos <- which(strsplit(args[1], "")[[1]]==".")
    extI <- substr(args[1],start=dotPos[length(dotPos)], stop=nchar(args[1]))
    dotPos <- which(strsplit(args[2], "")[[1]]==".")
    extO <- substr(args[2],start=dotPos[length(dotPos)], stop=nchar(args[2]))
    site <-substr(filename,start=1+underPos,stop=6+underPos)
    yearS <- substr(filename,start=8+underPos,stop=11+underPos)
    yearE <- substr(filename,start=13+underPos,stop=16+underPos)
    #site <-substr(args[1],start=1,stop=6)
    #yearS <- substr(args[1],start=8,stop=11)
    #yearE <- substr(args[1],start=13,stop=16)
    newInFile <- paste0(substr(args[1],start=1,stop=dotPos[2]), "nc")
}

if (length(args) > 2) {
    tempDir <- args[3]
} else {
    tempDir <- "temp"    
}

# variables definition
start_date <- as.POSIXlt(paste0(yearS,"-01-01 00:00:00"), tz = "GMT")
end_date <- as.POSIXlt(paste0(yearE,"-12-31 23:59:59"), tz = "GMT")
overwrite <- TRUE
verbose <- TRUE

# 0 create folders
cffolder <- file.path(tempDir,"cf")
dir.create(cffolder, showWarnings=FALSE, recursive=TRUE)

climfolder <- file.path(tempDir,"clim")
dir.create(climfolder, showWarnings=FALSE, recursive=TRUE)

# 1 copy input file to input directory and unzip if needed 
if (extI == ".zip") {
    file.copy(args[1],paste0(cffolder,"/",args[1]))
    wd <- getwd()
    rootZip <- paste0(tempDir,"/cf")
    setwd(rootZip)
    system(paste0("unzip  ./*"))
    file.remove(args[1])
    setwd(wd)
} else {
    file.copy(args[1],paste0(tempDir,"/cf/",site,".",yearS,".nc"))
    end_date <-start_date
    #file.copy(args[1],paste0(tempDir,"/raw"))
} 

# convert CF to output, in this case clim
require(PEcAn.SIPNET)
require(PEcAn.ED2)

met2model.SIPNET(cffolder, site, climfolder, start_date=start_date, end_date=end_date, overwrite=overwrite)

# next rename clim file to arg[2] 
filename <-list.files(climfolder)
#print( paste0(climfolder,"/",filename) )
file.rename(paste0(climfolder,"/",filename),args[2])
unlink(tempDir, recursive = TRUE) 
