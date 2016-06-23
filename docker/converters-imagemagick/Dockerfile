# Create softwareserver for polyglot.
FROM ncsa/polyglot
MAINTAINER Rob Kooper <kooper@illinois.edu>

USER root
# - install requirements
# - enable shellscripts to be scanned
# - enable imagemagick conversion by adding to .aliases.txt
RUN apt-get update && apt-get -y install imagemagick && \
	/bin/sed -i -e 's/^\([^#]*Scripts=\)/#\1/' -e 's/^#\(ShellScripts=\)/\1/' /home/polyglot/polyglot/SoftwareServer.conf && \
	echo "ImageMagick" > /home/polyglot/polyglot/scripts/sh/.aliases.txt 

# copy convert file to scripts/sh folder in container
# this is done to keep cache so you can debug script easily
COPY ImageMagick_convert.sh /home/polyglot/polyglot/scripts/sh/
RUN chown polyglot /home/polyglot/polyglot/scripts/sh/ImageMagick_convert.sh && \
    chmod +x /home/polyglot/polyglot/scripts/sh/ImageMagick_convert.sh

# SS creates a public/ folder at run time; user 'polyglot' does not have permission to do so. So keep using 'root'.
CMD ["softwareserver"]
