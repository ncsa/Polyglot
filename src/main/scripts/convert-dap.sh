#!/bin/bash
url="http://dap-dev.ncsa.illinois.edu:8184/convert/$2/"

for input_file in `ls *.$1` ; do
	output_url=`curl -u user1:password -s -H "Accept:text/plain" -F "file=@$input_file" $url`
	output_file=${input_file%.*}.$2
	echo "Converting: $input_file to $output_file"

	while : ; do
		wget -q --user=user1 --password=password -O $output_file $output_url
		if [ ${?} -eq 0 ] ; then break ; fi
		sleep 1
	done
done
