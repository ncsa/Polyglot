#!/bin/sh
#Prince (v9.0 rev 4)
#document
#html
#ps, pdf, html

prince   "$1" -o "$2"

# This script does not work as expected 100% of the times
# because many web pages save, along with its html file,
# some auxiliary files into a directory having usually the 
# same name as the html file. That directory is not being 
# copied by polyglot to the place where the script will 
# do the actual conversion.
