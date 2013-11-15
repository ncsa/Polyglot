#!/bin/bash

host="http://localhost:8182"
application="A3DReviewer"
task="convert"
output="igs"
input="stp"
url=$host/software/$application/$task/$output

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
