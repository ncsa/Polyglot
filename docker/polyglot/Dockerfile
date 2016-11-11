# Install polyglot server
FROM openjdk:jre
MAINTAINER Rui Liu <ruiliu@illinois.edu>

# expose some properties of the container
EXPOSE 8184 8182
ENV 	POL_IP="" \
	POL_AUTH="" \
	DOWNLOAD_SS_FILE="" \
	MONGO_SERVER="" \
	MONGO_DATABASE="" \
	MONGO_LOGGING="false" \
	RABBITMQ_URI="" \
	PUBLICIP=""

# install requirements
RUN wget -qO - https://bintray.com/user/downloadSubjectPublicKey?username=bintray | apt-key add -
RUN echo "deb http://dl.bintray.com/siegfried/debian wheezy main" | tee -a /etc/apt/sources.list
RUN apt-get update && apt-get -y install siegfried \
		wget && \
	useradd -m -s /bin/bash polyglot

# install polyglot
# POL rewrites tmp/iograph.txt; user 'polyglot' does not have permission to do so. So keep using 'root'.
COPY files /home/polyglot/
RUN cd /home/polyglot && \
	/bin/sed -i -e 's/^\([^#]*Scripts=\)/#\1/' polyglot/SoftwareServer.conf && \
	/bin/sed -i -e 's/^#*MongoLogging=.*$/MongoLogging=false/' polyglot/PolyglotRestlet.conf

# command to run when starting docker
COPY entrypoint.sh /home/polyglot/
ENTRYPOINT ["/home/polyglot/entrypoint.sh"]
CMD ["polyglot"]
