
if command docker >> /dev/null
then
	cr="docker"
elif command -v podman >> /dev/null
then
	cr="podman"
else 
	>&2 echo "No contianer runtime found"
	exit 1
fi
echo $cr
