#!/usr/bin/env python
#demclip
#dem
#xml
#tif
import ogr
import gdal
import subprocess
import sys
import tempfile
import os, os.path
import xmltodict
import requests

# clip the input DEM for a given polygon
def clip(inraster, inshape, fieldname, outputfile):
    # gathering nodata info
    raster = gdal.Open(inraster)
    nodata = raster.GetRasterBand(1).GetNoDataValue()
    ds = ogr.Open(inshape)
    lyr = ds.GetLayer(0)

    lyr.ResetReading()
    ft = lyr.GetNextFeature()

    while ft:
        # if there is a field contains the output file name
        value = ft.GetFieldAsString(fieldname)
        #out_name = value.replace(' ', '_')
        #outraster = outdir+'/'+out_name.strip()+".tif"   
        outraster = outputfile 
        print inraster
        print outraster

        # the following code selects a polygon
        o  = subprocess.check_output(['/Library/Frameworks/GDAL.framework/Programs/gdalwarp', inraster, outraster, '-srcnodata', str(nodata), '-dstnodata', str(nodata), '-cutline', inshape, '-crop_to_cutline', '-cwhere', "'%s'='%s'" % (fieldname, value)])
        
        print o
        ft = lyr.GetNextFeature()

    ds = None
    raster = None

# unzips the zipped shapefile and returns the .shp file
def unzip(tmpDir, inzip):
    subprocess.check_call(['/usr/local/bin/7z', 'x', '-o%s' % tmpDir, inzip], shell=False)
    files = [os.path.join(tmpDir,f) for f in os.listdir(tmpDir) ]
    for f in files:
        if f.endswith(".shp"):
            return f

# downloads the file from the given url
def download_file(url, local_filename):
    print url
    r = requests.get(url, stream=True, verify=False)
    print r.status_code
    with open(local_filename, 'wb') as f:
        for chunk in r.iter_content(chunk_size=10*1024): 
            if chunk: 
                f.write(chunk)
                f.flush()
        print 'downloading file',local_filename
    return local_filename


# Convert input XML file into dictionary
def convert_xml_to_dictionary(input_xml_filename) :
    dict_data = xmltodict.parse(file(input_xml_filename))
    print dict_data['input']['dem_data']['url']
    print dict_data['input']['shp_polygon']['url']
    dem_url = dict_data['input']['dem_data']['url']
    polygon_url = dict_data['input']['shp_polygon']['url']
    fieldname = dict_data['input']['shp_polygon']['fieldname']
    return (dem_url,polygon_url,fieldname)

if __name__ == '__main__':
    inputs = convert_xml_to_dictionary(sys.argv[1])
    dem_url = inputs[0]+ '?key=r1ek3rs'
    polygon_url = inputs[1] + '?key=r1ek3rs'    
    #print inputs[2]    
    dem_file = download_file(dem_url, 'dem_input.tif')
    shpfile = download_file(polygon_url,'polygon_shp.zip')

    if shpfile is not None and os.path.isfile(shpfile):
        try:
            print os.path.getsize(shpfile)
            if dem_file is not None and os.path.isfile(dem_file):
                try:
                    print os.path.getsize(dem_file)
                    #tmpDir = tempfile.mkdtemp()
                    #shp = unzip(tmpDir, shpfile)
                    shp = unzip(sys.argv[3], shpfile)
                    clip(dem_file,shp,str(inputs[2]),sys.argv[2])
                except OSError as oserror:
                    logger.exception("error : %s", oserror)
        except OSError as oserror:
            logger.exception("error removing input file: \n %s", oserror)        

