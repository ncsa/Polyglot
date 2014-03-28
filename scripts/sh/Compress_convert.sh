#!/bin/bash
#Compressed_Conversion_Format (v0.9)
#Convert among different compression formats
#tgz, tar.bz2, zip, 7z, rar
#tar.gz, tgz, tar.bz2,tbz2, zip, 7z, rar
#note: tgz=tar.gz
#note: tbz2=tar.bz2


randomDir=$RANDOM

#inputFile=`echo ${1##*/}`
inputFile=$(basename "$1")
outputFile=$(basename "$2")


inputExt="${inputFile##*.}"
outputExt="${outputFile##*.}"

filenameI=${inputFile%.*}
extI=${inputFile##*.}
isTarI=${filenameI##*.}
newIFN=${1%%.*}

filenameO=${outputFile%.*}
extO=${outputFile##*.}
isTarO=${filenameO##*.}
newOFN=${2%%.*}



zipNoTar() {
    # just add more cases as needed
    if   [ "$2" == "zip" ]; then
        zip -qr9  "$1".x  `ls`
    elif [ "$2" == "7z" ]; then
        7za a "$1".x  `ls`  > /dev/null
    elif [ "$2" == "rar" ]; then
        rar a -r -inul "$1".x `ls`
    elif [ "$2" == "tgz" ]; then
        tar czf "$1".x `ls`
    elif [ "$2" == "tbz2" ]; then
        tar cjf "$1".x `ls`
    fi
    mv "$1".x "$3"
} # end of zipNoTar()

unzipNoTar() {
    # just add more cases as needed
    if   [ "$2" == "zip" ]; then
        unzip -qo "$1"
    elif [ "$2" == "7z" ]; then
        7za x "$1" > /dev/null
    elif [ "$2" == "rar" ]; then
        unrar x -inul "$1"
    elif [ "$2" == "tgz" ]; then
        tar xzf $1
    elif [ "$2" == "tbz2" ]; then
        tar xjf $1
    fi
    rm "$1"
} # end of unzipNoTar()


if [ "$inputFile" == "$outputFile" ]; then
    #echo 'This is a simple copy'
    cp "$1" "$2"
else 
    mkdir /tmp/CompressedConversion_$randomDir
    cp "$1" /tmp/CompressedConversion_$randomDir
    cd /tmp/CompressedConversion_$randomDir/
    
    if   [ "$isTarI" == "tar" -a "$isTarI" != "$filenameI" -a "$isTarO" == "tar" -a "$isTarO" != "$filenameO" ]; then
        #echo 'this section is for tar input files AND tar output files'
        
        # unzip first
        if [ "$extI" == "gz" -a  "$isTarI" == "tar" -a "$isTarI" != "$filenameI" ]; then
            tar xzf $inputFile 
        elif [ "$extI" == "bz2" -a  "$isTarI" == "tar" -a "$isTarI" != "$filenameI" ]; then
            tar xjf $inputFile 
        fi    
        rm $inputFile
        
        # then rezip it

        if [ "$extO" == "gz" ]; then
            tar czf $inputFile.x `ls`
        elif [ "$extO" == "bz2" ]; then
            tar cjf $inputFile.x `ls`
        else
            zipNoTar "$inputFile" "$extO" "$2"
        fi
        mv "$inputFile.x" "$2"
        
    elif [ "$isTarI" == "tar" -a "$isTarI" != "$filenameI"  ]; then
        #echo 'this section is for tar input files and notar output files'
        # unzip first
        if [ "$extI" == "gz" -a  "$isTarI" == "tar" -a "$isTarI" != "$filenameI" ]; then
            tar xzf $inputFile 
        elif [ "$extI" == "bz2" -a  "$isTarI" == "tar" -a "$isTarI" != "$filenameI" ]; then
            tar xjf $inputFile 
        fi    
        rm $inputFile

        # then rezip it  
        zipNoTar "$inputFile" "$2" "$extO"
        
    elif [ "$isTarO" == "tar" -a "$isTarO" != "$filenameO"  ]; then
        #echo 'this section is for notar input files and tar output files'
        
        # unzip first
        unzipNoTar "$inputFile" "$extI"
        
        # then rezip it  
        if [ "$extO" == "gz" ]; then
            tar czf $inputFile.x `ls`
        elif [ "$extO" == "bz2" ]; then
            tar cjf $inputFile.x `ls`
        fi
        mv "$inputFile.x" "$2"
        
    else
        #echo 'this section is for no tar cases'
        
        # unzip first
        unzipNoTar "$inputFile" "$extI"
        
        # then rezip it  
        zipNoTar "$inputFile" "$extO" "$2"
    fi
    rm -rf /tmp/CompressedConversion_$randomDir
fi

