#!/usr/bin/python -u
import sys
import json
import urllib
import requests
import os
import time
import smtplib
import socket
import getopt
import netifaces as ni
from os.path import basename

def main():
	"""Run extraction bus tests."""
	host = 'http://' + ni.ifaddresses('eth0')[2][0]['addr']
	suppress = False

	#Arguments
	opts, args = getopt.getopt(sys.argv[1:], 'h:s')

	for o, a in opts:
		if o == '-h':
			host = 'http://' + a
		elif o == '-s':
			suppress = True
		else:
			assert False, "unhandled option"

	print 'Testing: ' + host

	#Remove previous outputs
	for output_filename in os.listdir('tmp'):
		if(output_filename[0] != '.'):
			os.unlink('tmp/' + output_filename)

	#Read in tests
	with open('tests.txt', 'r') as tests_file:
		lines = tests_file.readlines()
		count = 0;
		mailserver = smtplib.SMTP('localhost')
		failure_report = ''
		t0 = time.time()

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
					output_filename = convert(host, input_filename, output, output_filename)

					#Check for expected output
					if os.path.isfile(output_filename) and os.stat(output_filename).st_size > 0:
						print '\t\033[92m[OK]\033[0m'

						os.chmod(output_filename, 0776)		#Give web application permission to overwrite
					else:
						print '\t\033[91m[Failed]\033[0m'

						#Send email notifying watchers	
						message = 'Test-' + str(count) + ' failed.  Output file of type "' + output + '" was not created from:\n\n' + input_filename + '\n\n'
						failure_report += message
						message += 'Report of last run can be seen here: \n\n http://' + socket.getfqdn() + '/dap/tests/tests.php?run=false&start=true\n'
						message = 'Subject: DAP Test Failed\n\n' + message
	
						if not suppress:					
							with open('watchers.txt', 'r') as watchers_file:
								watchers = watchers_file.readlines()
		
								for watcher in watchers:
									watcher = watcher.strip()
									mailserver.sendmail('', watcher, message)

		dt = time.time() - t0;
		print 'Elapsed time: ' + timeToString(dt)

		#Send a final report of failures
		if failure_report:
			failure_report = 'Subject: DAP Test Failure Report\n\n' + failure_report
			failure_report += 'Report of last run can be seen here: \n\n http://' + socket.getfqdn() + '/dap/tests/tests.php?run=false&start=true\n\n'
			failure_report += 'Elapsed time: ' + timeToString(dt)

			with open('watchers.txt', 'r') as watchers_file:
				watchers = watchers_file.readlines()
		
				for watcher in watchers:
					watcher = watcher.strip()
					mailserver.sendmail('', watcher, failure_report)
		else:
			with open('watchers.txt', 'r') as watchers_file:
				watchers = watchers_file.readlines()

				for watcher in watchers:
					message = 'Subject: DAP Tests Passed\n\n';
					message += 'Elapsed time: ' + timeToString(dt)
					mailserver.sendmail('', watcher, message)

		mailserver.quit()

def convert(host, input_filename, output, output_path):
	"""Pass file to Polyglot Steward."""
	headers = {'Accept': 'text/plain'}
	api_call = host + ':8184/convert/' + output + '/' + urllib.quote_plus(input_filename)
	r = requests.get(api_call, headers=headers)

	if(r.status_code != 404):
		result = r.text

		if basename(output_path):
			output_filename = output_path
		else:
			output_filename = output_path + basename(result)

		download_file(result, output_filename, 60)
	else:
		output_filename = ""

	return output_filename

def download_file(url, filename, wait):
	"""Download file at given URL"""
	if not filename:
		filename = url.split('/')[-1]

	r = requests.get(url, stream=True)

	while(wait > 0 and r.status_code == 404):
		time.sleep(1)
		wait -= 1
		r = requests.get(url, stream=True)

	if(r.status_code != 404):	
		with open(filename, 'wb') as f:
			for chunk in r.iter_content(chunk_size=1024): 
				if chunk:		#Filter out keep-alive new chunks
					f.write(chunk)
					f.flush()
	
	return filename

def timeToString(t):
  """Return a string represntation of the give elapsed time"""
  h = int(t / 3600);
  m = int((t % 3600) / 60);
  s = int((t % 3600) % 60);

  if h > 0:
    return str(round(h + m / 60.0, 2)) + ' hours';
  elif m > 0:
    return str(round(m + s / 60.0, 2)) + ' minutes';
  else:
    return str(s) + ' seconds';

if __name__ == '__main__':
	main()
