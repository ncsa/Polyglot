#!/usr/bin/env python
#streamclip
#shapefile
#xml
#shp.zip
import ogr
import gdal
import subprocess
import sys
import tempfile
import os, os.path
import shutil
import xmltodict
import requests
import re

#clip the shapefile of streamflow with the given polygon 
def shpclip(inshp, clipshp, outshpname):
    m = re.split('\.zip', outshpname)
    #print m[0]
    outshp = m[0]
    o = subprocess.check_output(['/Library/Frameworks/GDAL.framework/Programs/ogr2ogr', "-f", "ESRI Shapefile", "-clipsrc", clipshp, outshp, inshp])
    print o
    zipshp(outshpname)

#zip the clipped shapefile for the streamflow to be output to the user
def zipshp(name):
    m = re.split('\.shp.zip', name)
    subprocess.check_call(['/usr/local/bin/7z', 'a','-tzip', name, m[0]+'*','-x!*.xml'], shell=False)
    return name  

#unzip the shapefile
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
    print dict_data['input']['streamflow_data']['url']
    print dict_data['input']['shp_polygon']['url']
    streamflow_url = dict_data['input']['streamflow_data']['url']
    polygon_url = dict_data['input']['shp_polygon']['url']
    return (streamflow_url,polygon_url)

if __name__ == '__main__':
    inputs = convert_xml_to_dictionary(sys.argv[1])
    streamflow_url = inputs[0]+ '?key=r1ek3rs'
    polygon_url = inputs[1] + '?key=r1ek3rs'    
       
    streamflow_shpfile = download_file(streamflow_url, 'selected-flow.zip')
    polygon_shpfile = download_file(polygon_url,'study-region.zip')
    
    if streamflow_shpfile is not None and os.path.isfile(streamflow_shpfile):
        try:
            print os.path.getsize(streamflow_shpfile)
            if polygon_shpfile is not None and os.path.isfile(polygon_shpfile):
                try:
                    print os.path.getsize(polygon_shpfile)
                    if os.path.exists(sys.argv[3]+"/inshp"):
                        shutil.rmtree(sys.argv[3]+"/inshp")
                    os.makedirs(sys.argv[3]+"/inshp")
                    if os.path.exists(sys.argv[3]+"/clipshp"):
                        shutil.rmtree(sys.argv[3]+"/clipshp")
                    os.makedirs(sys.argv[3]+"/clipshp")
                    inshp = unzip(sys.argv[3]+"/inshp", streamflow_shpfile)
                    clipshp = unzip(sys.argv[3]+"/clipshp", polygon_shpfile)
                    shpclip(inshp, clipshp, sys.argv[2])
                except OSError as oserror:
                    logger.exception("error : %s", oserror)
        except OSError as oserror:
            logger.exception("error removing input file: \n %s", oserror)        



    
    
