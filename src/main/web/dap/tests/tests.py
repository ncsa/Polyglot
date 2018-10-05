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
import thread
import threading
import datetime
from os.path import basename
from pymongo import MongoClient

failure_report = ''
enable_threads = False
threads = 0
timeout = 300 # 5 minutes
lock = threading.Lock()
total_success = 0

def main():
	"""Run extraction bus tests."""
	global failure_report
	global enable_threads
	global threads
	global lock
	global total_success
	host = ni.ifaddresses('eth0')[2][0]['addr']
	hostname = ''
	all_failures = False
	total_tests = 0

	#Arguments
	opts, args = getopt.getopt(sys.argv[1:], 'h:n:at')

	for o, a in opts:
		if o == '-h':
			host = a
		elif o == '-n':
			hostname = a
		elif o == '-a':
			all_failures = True
		elif o == '-t':
			enable_threads = True
		else:
			assert False, "unhandled option"

	#Set hostname if not set
	if not hostname:
		if '@' in host:
			hostname = host.split('@')[1]
		else:
			hostname = host
		
	print 'Testing: ' + host + '\n'

	#Remove previous outputs
	for output_filename in os.listdir('tmp'):
		if(output_filename[0] != '.' and output_filename != 'failures.txt'):
			os.unlink('tmp/' + output_filename)

	#Read in tests
	with open('tests.txt', 'r') as tests_file:
		lines = tests_file.readlines()
		count = 0;
		t0 = time.time()
		comment = ''
		runs = 1

		for line in lines:
			line = line.strip();

			if line and line.startswith('@'):
				comment = line[1:]

				if not enable_threads:
					print comment + ': '
			elif line and line.startswith('*'):
				runs = int(line[1:])
			elif line and not line.startswith('#'):
				parts = line.split(' ')
				input_filename = parts[0]
				outputs = parts[1].split(',')

				for output in outputs:
					output = output.strip();
					count += 1

					#Run the test
					total_tests += runs
					if enable_threads:
						for i in range(0, runs):
							with lock:
								threads += 1

							thread.start_new_thread(run_test, (host, hostname, input_filename, output, count, comment, all_failures))
					else:
						for i in range(0, runs):
							run_test(host, hostname, input_filename, output, count, comment, all_failures)

					#Set runs back to one for next test
					comment = ''
					runs = 1

		#Wait for threads if any
		if enable_threads:
			while threads:
				time.sleep(1)

		dt = time.time() - t0;
		print 'Elapsed time: ' + timeToString(dt)

		#Save to mongo
		client = MongoClient()
		db = client['tests']
		collection = db['dap']
		
		if failure_report:
			document = {'time': int(round(time.time()*1000)), 'elapsed_time': dt, 'failures': True}
		else:
			document = {'time': int(round(time.time()*1000)), 'elapsed_time': dt, 'failures': False}

		collection.insert(document)

		report(host, hostname, 'DAP', total_tests, total_success, failure_report, dt)
		
		#Send a final report of failures
		if failure_report:
			#Save current failure report to a file
			with open('tmp/failures.txt', 'w') as output_file:
				output_file.write(failure_report)

			#Send failure notification emails
			with open('failure_watchers.txt', 'r') as watchers_file:
				watchers = watchers_file.read().splitlines()
				
				message = 'From: \"' + hostname + '\" <devnull@ncsa.illinois.edu>\n'
				message += 'To: ' + ', '.join(watchers) + '\n'
				message += 'Subject: DAP Test Failure Report\n\n'
				message += failure_report	
				message += 'Report of last run can be seen here: \n\n http://' + socket.getfqdn() + '/dap/tests/tests.php?dap=' + urllib.quote(host) + '&run=false&start=true\n\n'
				message += 'Elapsed time: ' + timeToString(dt)

				mailserver = smtplib.SMTP('localhost')
				for watcher in watchers:
					mailserver.sendmail('', watcher, message)
				mailserver.quit()
		else:
			if os.path.isfile('tmp/failures.txt'):
				#Send failure rectification emails
				with open('tmp/failures.txt', 'r') as report_file:
					failure_report = report_file.read()
					os.unlink('tmp/failures.txt')

					with open('failure_watchers.txt', 'r') as watchers_file:
						watchers = watchers_file.read().splitlines()

						message = 'From: \"' + hostname + '\" <devnull@ncsa.illinois.edu>\n'
						message += 'To: ' + ', '.join(watchers) + '\n'
						message += 'Subject: DAP Tests Now Passing\n\n'
						message += 'Previous failures:\n\n'
						message += failure_report
						message += 'Report of last run can be seen here: \n\n http://' + socket.getfqdn() + '/dap/tests/tests.php?dap=' + urllib.quote(host) + '&run=false&start=true\n\n'
						message += 'Elapsed time: ' + timeToString(dt)

						mailserver = smtplib.SMTP('localhost')
						for watcher in watchers:
							mailserver.sendmail('', watcher, message)
						mailserver.quit()
			else:
		#Send success notification emails
				with open('pass_watchers.txt', 'r') as watchers_file:
					watchers = watchers_file.read().splitlines()
			
					message = 'From: \"' + hostname + '\" <devnull@ncsa.illinois.edu>\n'
					message += 'To: ' + ', '.join(watchers) + '\n'
					message += 'Subject: DAP Tests Passed\n\n'
					message += 'Elapsed time: ' + timeToString(dt)

					mailserver = smtplib.SMTP('localhost')
					for watcher in watchers:
						mailserver.sendmail('', watcher, message)
					mailserver.quit()

def run_test(host, hostname, input_filename, output, count, comment, all_failures):
	"""Run a test."""
	global failure_report
	global enable_threads
	global threads
	global lock
	global total_success

	#Print out test
	if enable_threads:
		with lock:
			print input_filename + ' -> "' + output + '"\t\033[94m[Running]\033[0m'
	else:
		print(input_filename + ' -> "' + output + '"'),

	output_filename = 'tmp/' + str(count) + '_' + os.path.splitext(basename(input_filename))[0] + '.' + output
	output_filename = convert(host, input_filename, output, output_filename)

	#Display result
	if enable_threads:
		with lock:
			print(input_filename + ' -> "' + output + '"'),

			if os.path.isfile(output_filename) and os.stat(output_filename).st_size > 0:
				total_success += 1
				print '\t\033[92m[OK]\033[0m'
			else:
				print '\t\033[91m[Failed]\033[0m'

	#Check for expected output and add to report
	if os.path.isfile(output_filename) and os.stat(output_filename).st_size > 0:
		if not enable_threads:
			total_success += 1
			print '\t\033[92m[OK]\033[0m\n'

		os.chmod(output_filename, 0776)		#Give web application permission to overwrite
	else:
		if not enable_threads:
			print '\t\033[91m[Failed]\033[0m\n'

		report = ''

		if comment:
			report = 'Test-' + str(count) + ' failed: ' + comment + '.  Output file of type "' + output + '" was not created from:\n\n' + input_filename + '\n\n'
		else:
			report = 'Test-' + str(count) + ' failed.  Output file of type "' + output + '" was not created from:\n\n' + input_filename + '\n\n'

		if enable_threads:
			with lock:
				failure_report += report
		else:
			failure_report += report
				
		#Send email notifying watchers	
		if all_failures:					
			with open('failure_watchers.txt', 'r') as watchers_file:
				watchers = watchers_file.read().splitlines()
						
				message = 'From: \"' + hostname + '\" <devnull@ncsa.illinois.edu>\n'
				message += 'To: ' + ', '.join(watchers) + '\n'
				message += 'Subject: DAP Test Failed\n\n'
				message += report
				message += 'Report of last run can be seen here: \n\n http://' + socket.getfqdn() + '/dap/tests/tests.php?dap=' + urllib.quote(host) + '&run=false&start=true\n'
		
				mailserver = smtplib.SMTP('localhost')
				for watcher in watchers:
					mailserver.sendmail('', watcher, message)
				mailserver.quit()

	#If in a thread decrement the thread counter
	with lock:
		if threads:
			threads -= 1

def convert(host, input_filename, output, output_path):
	"""Pass file to Polyglot Steward."""
	username = ''
	password = ''
	output_filename = ''
	
	#Check for authentication
	if '@' in host:
		parts = host.rsplit('@', 1)
		host = parts[1]
		parts = parts[0].split(':')
		username = parts[0]
		password = parts[1]

	headers = {'Accept': 'text/plain'}
	api_call = 'http://' + host + ':8184/convert/' + output + '/' + urllib.quote_plus(input_filename)

	try:
		r = requests.get(api_call, auth=(username, password), headers=headers, timeout=timeout)

		if(r.status_code != 404):
			result = r.text

			if basename(output_path):
				output_filename = output_path
			else:
				output_filename = output_path + basename(result)

			download_file(result, output_filename, 90, username, password)
	except KeyboardInterrupt:
		sys.exit()
	except:
		e = sys.exc_info()[0]
		print repr(e)

	return output_filename

def download_file(url, filename, wait, username='', password=''):
	"""Download file at given URL"""
	if not filename:
		filename = url.split('/')[-1]

	try:
		r = requests.get(url, auth=(username, password), stream=True)

		while(wait > 0 and r.status_code == 404):
			time.sleep(1)
			wait -= 1
			r = requests.get(url, auth=(username, password), stream=True)

		if(r.status_code != 404):	
			with open(filename, 'wb') as f:
				for chunk in r.iter_content(chunk_size=1024): 
					if chunk:		#Filter out keep-alive new chunks
						f.write(chunk)
						f.flush()
	except:
		raise

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

def report(host, hostname, type, total, success, message, elapsed_time):
	"""Write the test results to mongo database"""
	# create the message
	document = { "host": host, "hostname": hostname, "type": type,
				 "total": total, "success": success, "failures": (total - success),
				 "message": message,
				 "elapsed_time": elapsed_time, "date": datetime.datetime.utcnow() }
	mc = MongoClient("mongodb://mongo.ncsa.illinois.edu:27017/")
	db = mc['browndog']
	tests = db['tests']
	tests.insert(document)

if __name__ == '__main__':
	main()
