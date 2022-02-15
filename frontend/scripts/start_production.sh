#!/bin/bash
set -e
set -x

# Execute from root folder

export $(grep -v '^#' .env | xargs)


if ! cr="$(./scripts/get_cr.sh)"
then
	exit 1
fi 

echo "Using $cr as container runtime"

$cr build -t conquery:v1 .
./scripts/stop_container.sh conquery
$cr run --env-file=".env" -p $PORT:80 --name conquery conquery:v1
