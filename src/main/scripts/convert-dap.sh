#!/bin/bash

host="http://dap-dev.ncsa.illinois.edu:8184"
input="jp2"
output="jpg"

url=$host/convert/$output/

for input_file in `ls *.$input` ; do
	output_url=`curl -s -H "Accept:text/plain" -F "file=@$input_file" $url`
	output_file=${input_file%.*}.$output
	echo "Converting: $input_file to $output_file"

	while : ; do
		wget -q -O $output_file $output_url
		
		if [ ${?} -eq 0 ] ; then
			break
		fi

		sleep 1
	done
done
