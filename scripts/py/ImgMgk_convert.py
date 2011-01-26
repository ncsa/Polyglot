#ImageMagick (v6.5.2)
#image
#bmp, dib, eps, fig, gif, ico, jpg, jpeg, pdf, pgm, pict, pix, png, pnm, ppm, ps, rgb, rgba, sgi, sun, svg, tga, tif, tiff, ttf, x, xbm, xcf, xpm, xwd, yuv
#bmp, dib, eps, gif, jpg, jpeg, pdf, pgm, pict, png, pnm, ppm, ps, rgb, rgba, sgi, sun, svg, tga, tif, tiff, ttf, x, xbm, xpm, xwd, yuv
import subprocess;
import sys;

subprocess.call(["C:\Program Files (x86)\ImageMagick-6.5.2-Q16\convert.exe", sys.argv[1], sys.argv[2]]);
