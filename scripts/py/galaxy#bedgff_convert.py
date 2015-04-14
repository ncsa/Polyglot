#galaxy#bedgff
#genomic
#bed,gff
#bed,gff

import sys
import time
import json
import urllib2

from bioblend.galaxy import GalaxyInstance
from bioblend.galaxy.histories import HistoryClient
from bioblend.galaxy.tools import ToolClient
from bioblend.galaxy.datasets import DatasetClient

galaxy_location = "local"
galaxy_location = "galaxy"

if galaxy_location == "local":
    apikey = "d7a73895e9f9605f7247258734acf4b2"
    base_url = "http://127.0.0.1:8080"
elif galaxy_location == "galaxy":
    apikey = "4973c3c8b10f85ab653857a2d6f62712"
    base_url = "https://usegalaxy.org"

def choose_converter(input_file_type,output_file_type):
    print "\nfiles types =============================="
    print input_file_type, output_file_type
    if input_file_type == "bed" and output_file_type == "gff":          #tested
        tool_id = "bed2gff1"
    elif input_file_type == "gff" and output_file_type == "bed":        #tested
        tool_id = "gff2bed1"
    elif input_file_type == "axt" and output_file_type == "fasta":      #tested
        tool_id = "axt_to_fasta"
    elif input_file_type == "axt" and output_file_type == "lav":        #after 4 minuntes did not finish converstion, no data to temp output file
        tool_id = "axt_to_lav_1"
    elif input_file_type == "lav" and output_file_type == "bed":        #couldn't find file with ~20min looking
        tool_id = "lav_to_bed1"
    elif input_file_type == "maf" and output_file_type == "bed":
        tool_id = "MAF_To_BED1"
    elif input_file_type == "maf" and output_file_type == "interval":
        tool_id = "MAF_To_Interval1"
    elif input_file_type == "maf" and output_file_type == "fasta":
        tool_id = "MAF_To_Fasta1"
        #todo: check name if inteval or simple
    elif input_file_type == "wiggle" and output_file_type == "interval":
        tool_id = "wiggle2simple1"
    # elif input_file_type == "sff" and output_file_type == "":
    #     tool_id = "Sff_extractor"
    elif input_file_type == "gtf" and output_file_type == "bedgraph":
        tool_id = "gtf2bedgraph"
    elif input_file_type == "wig" and output_file_type == "bigwig":
        tool_id = "wig_to_bigWig"
    elif input_file_type == "bed" and output_file_type == "bigbed":
        tool_id = "bed_to_bigBed"
    else:
        print "No conversion software for "+input_file_type+" to "+output_file_type+" found"
        quit()
    return tool_id

def wait_4_process(history_id,wait_about):
# Check that upload and run are complete before continuing
    full_url = base_url + '/api/histories/' + history_id + '/contents/'
    request_output = get( apikey, full_url )
    run_state_all_ok = False
    time_passed = 0
    while(run_state_all_ok == False):
        for i in range(len(request_output)):
            if request_output[i]['state'] != 'ok':
                run_state_all_ok = False
                break
            else:
                run_state_all_ok = True
        print "Waiting for " + wait_about +" to finish at time after started = " + str(time_passed) +"s"
        time_passed+=5
        time.sleep(5.0)
        full_url = base_url + '/api/histories/' + history_id + '/contents/'
        request_output = get( apikey, full_url )
    return

def get( api_key, url ):
    url = make_url( api_key, url )
    return json.loads( urllib2.urlopen( url ).read() )

def make_url( api_key, url, args=None ):
    # Adds the API Key to the URL if it's not already there.
    if args is None:
        args = []
    argsep = '&'
    if '?' not in url:
        argsep = '?'
    if '?key=' not in url and '&key=' not in url:
        args.insert( 0, ( 'key', api_key ) )
    return url + argsep + '&'.join( [ '='.join( t ) for t in args ] )

if __name__ == '__main__':
# GET PATH NAMES AND EXTENSIONS FROM COMMAND LINE INPUT
    input_file_full = sys.argv[1]
    input_file_format = input_file_full[input_file_full.rfind(".")+1:len(input_file_full)]

    output_file_full = sys.argv[2]
    output_file_format = output_file_full[output_file_full.rfind(".")+1:len(output_file_full)]

# CHOOSE CONVERTER
    tool_id = choose_converter(input_file_format,output_file_format)

# INITIALIZE GALAXY
    galaxy_instance = GalaxyInstance(url=base_url, key=apikey)
    history_client = HistoryClient(galaxy_instance)
    tool_client = ToolClient(galaxy_instance)
    dataset_client = DatasetClient(galaxy_instance)
    history = history_client.create_history('tmp')

# UPLOAD FILES
    input_file_1 = tool_client.upload_file(input_file_full, history['id'], type='txt')
    input_file_2 = tool_client.upload_file(input_file_full, history['id'], type='txt')
    params = {'input_numbers_001':{'src': 'hda', 'id': input_file_1['outputs'][0]['id']},'input_numbers_002':{'src': 'hda', 'id': input_file_2['outputs'][0]['id']}}
    wait_4_process(history['id'],"uploading files")

# RUN CONVERSION
    runtool_output = tool_client.run_tool(history_id=history['id'], tool_id=tool_id, tool_inputs=params)
    wait_4_process(history['id'],"running tool")

# DOWNLOAD CONVERTED FILE
    download_output = dataset_client.download_dataset(runtool_output['jobs'][0]['id'],output_file_full, use_default_filename=False)

    print download_output





