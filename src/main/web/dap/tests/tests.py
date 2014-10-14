#!/usr/bin/python -u
import sys
import json
import urllib
import requests
import os
import time
import smtplib
import socket
from os.path import basename

host = 'http://kgm-d3.ncsa.illinois.edu'

def main():
	"""Run extraction bus tests."""
	with open('tests.txt', 'r') as tests_file:
		#Read in tests
		lines = tests_file.readlines()
		count = 0;
		mailserver = smtplib.SMTP('localhost')

		for line in lines:
			if not line.startswith('#'):
				parts = line.split(' ')
				input_filename = parts[0]
				outputs = parts[1].split(',')

				for output in outputs:
					output = output.strip();
					count += 1

					print(input_filename + ' -> "' + output + '"'),

					#Run test
					output_filename = 'tmp/' + str(count) + '_' + os.path.splitext(basename(input_filename))[0] + '.' + output
					download_file(host + ':8184/convert/' + output + '/' + urllib.quote_plus(input_filename), output_filename);

					#Check for expected output
					if os.path.isfile(output_filename) and os.stat(output_filename).st_size > 0:
						print '\t\033[92m[OK]\033[0m'
					else:
						print '\t\033[91m[Failed]\033[0m'

						#Send email notifying watchers	
						with open('watchers.txt', 'r') as watchers_file:
							watchers = watchers_file.readlines()
		
							for watcher in watchers:
								watcher = watcher.strip()

								message = 'Subject: DAP Test Failed\n\n'
								message += 'Test-' + str(count) + ' failed.  Output file of type "' + output + '" was not created from:\n\n' + input_filename + '\n\n'
								message += 'Report of last run can be seen here: \n\n http://' + socket.getfqdn() + '/dap/tests/tests.php?run=false&start=true\n'
								
								mailserver.sendmail('', watcher, message)

		mailserver.quit()

def download_file(url, filename):
	"""Download file at given URL"""
	if not filename:
		filename = url.split('/')[-1]

	r = requests.get(url, stream=True)

	if(r.status_code != 404):	
		with open(filename, 'wb') as f:
			for chunk in r.iter_content(chunk_size=1024): 
				if chunk:		#Filter out keep-alive new chunks
					f.write(chunk)
					f.flush()
	
	return filename

if __name__ == '__main__':
	main()
