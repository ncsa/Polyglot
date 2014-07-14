#!/bin/bash
#daffodil
#file

SCRIPT_DIR="$(dirname $(readlink -f ${BASH_SOURCE[0]}))"
SCRIPT_NAME=${0%.*}
SCRIPT_NAME=${SCRIPT_NAME##*/}
IFS='_' read -a PARTS <<< "$SCRIPT_NAME"
FORMAT=${PARTS[2]}

$SCRIPT_DIR/auxiliary/daffodil.sh parse -s $SCRIPT_DIR/../../schemas/dfdl/$FORMAT.dfdl.xsd $1 > $2
