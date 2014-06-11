#!/bin/bash
#Video2Audio (0.8.9-6)
#video to audio
#ogg, aac, m4a, mp3, flac, wav, mpeg, aiff, mov, avi
#ogg, aac, m4a, mp3, flac, wav, mpeg, aiff, mov, avi

avconv -i "$1"  -map 0:a  -strict experimental  -y "$2"

# The -strict experimental is required to create mov and avi
# formats in avconv 9.13-6
