#!/usr/bin/python -u
#Clowder
#data
#jpg, jpeg, png, txt, pdf, obj
#json
import sys
import json
import requests
import os
import time
from auxiliary import bd

bds = 'https://bd-api-dev.ncsa.illinois.edu/'
api_key = ''
token = bd.get_token(bds, api_key)
wait = 60

input_filename = sys.argv[1]
metadata = bd.extract(bds, input_filename, token, wait)

#Save extracted metadata
with open(sys.argv[2], "w") as output_file:
	output_file.write(json.dumps(metadata))
