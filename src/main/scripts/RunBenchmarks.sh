#!/bin/bash
server="http://admin:password@141.142.224.231:8182"
data="tmp/Benchmarks/Data"
results="tmp/Benchmarks/Results"

function benchmark(){
	curl $server/reboot
	echo
	sleep 120
	./Benchmarks.sh -a $1 $data/$2
	curl -s $server/screen -o $results/$1-$2.jpg
	echo
}

benchmark 3DS 3DS20_valid
benchmark 3DS 3DS20_mixed
benchmark A3DReviewer A3DReviewer20_valid
benchmark A3DReviewer A3DReviewer20_mixed
benchmark Blender Blender20_valid
benchmark Blender Blender20_mixed
benchmark GSkUp GSkUp20_valid
benchmark GSkUp GSkUp20_mixed
benchmark ImgMgk ImgMgk20_valid
benchmark ImgMgk ImgMgk20_mixed
benchmark IrfanView ImgMgk20_valid
benchmark IrfanView ImgMgk20_mixed
benchmark Paint5 ImgMgk20_valid
benchmark Paint5 ImgMgk20_mixed
benchmark Word Word20_valid
benchmark Word Word20_mixed
benchmark ParaView ParaView20_valid
benchmark ParaView ParaView20_mixed
benchmark VTK 3DS20_valid
benchmark VTK 3DS20_mixed

echo [Finished]
