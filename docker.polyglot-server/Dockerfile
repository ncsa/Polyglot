# Install polyglot server
FROM ubuntu:14.04
MAINTAINER Rob Kooper <kooper@illinois.edu>

# expose some properties of the container
EXPOSE 8184 8182
ENV 	POL_AUTH="" \
	MONGO_SERVER="" \
	MONGO_DATABASE="" \
	MONGO_LOGGING="false" \
	RABBITMQURI="" \
	PUBLICIP=""

# install requirements
RUN apt-get update && apt-get -y install \
		openjdk-7-jre-headless \
		unzip \
		wget && \
	useradd -m -s /bin/bash polyglot

# install polyglot
USER polyglot
COPY files /home/polyglot/
RUN cd /home/polyglot && \
	/bin/sed -i -e 's/^\([^#]*Scripts=\)/#\1/' polyglot/SoftwareServer.conf && \
	/bin/sed -i -e 's/^#*MongoLogging=.*$/MongoLogging=false/' polyglot/PolyglotRestlet.conf

# command to run when starting docker
COPY entrypoint.sh /home/polyglot/
ENTRYPOINT ["/home/polyglot/entrypoint.sh"]
CMD ["polyglot"]

