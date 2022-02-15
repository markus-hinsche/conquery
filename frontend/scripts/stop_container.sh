#!/bin/bash

if [ -z "$1" ]; then
  echo "usage: $0 <container_id>"
  exit
fi

if [ -z $2 ]
then
	if ! cr=$(./scripts/get_cr.sh)
	then
		exit 1
	fi

	echo "Using $cr as container runtime"
else
	cr=$2
fi

$cr rm -f $1
