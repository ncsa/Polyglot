#!/bin/sh
#ImageMagick (v6.5.2)
#image
#bmp, dib, eps, fig, gif, ico, jpg, jpeg, pdf, pgm, pict, pix, png, pnm, ppm, ps, rgb, rgba, sgi, sun, svg, tga, tif, tiff, ttf, x, xbm, xcf, xpm, xwd, yuv
#bmp, dib, eps, gif, jpg, jpeg, pdf, pgm, pict, png, pnm, ppm, ps, rgb, rgba, sgi, sun, svg, tga, tif, tiff, ttf, x, xbm, xpm, xwd, yuv

output_filename=$(basename "$2")
output_format="${output_filename##*.}" 

#Output PGM files as ASCII
if [ "$output_format" = "pgm" ]; then
	convert "$1" -compress none "$2"
else
	convert "$1" "$2"
fi
