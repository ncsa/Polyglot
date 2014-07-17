#!/bin/bash
#avconv (0.8.9-6)
#audio
#mp4, asf, wmv, mpeg, flv, ogg, ogv, mkv, vob, asf, avi, mov, flac
#ogg, ogv, aac, m4a, mp3, flac, wav, mpeg, aiff, avi, mov

avconv -i "$1"  -map 0:a -strict experimental -y "$2"

# The -strict experimental is required to create mov and avi
# formats in avconv 9.13-6
