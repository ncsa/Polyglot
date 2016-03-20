# Install polyglot server
FROM ubuntu:14.04
MAINTAINER Rob Kooper <kooper@illinois.edu>

# expose some properties of the container
EXPOSE 8184 8182
ENV POL_BRANCH=POL-MAIN25 \
	POL_BUILD=latestSuccessful \
	POL_AUTH="" \
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
RUN cd /home/polyglot && \
	# which software server to install
	case ${POL_BUILD} in latest*) BB="${POL_BRANCH}/${POL_BUILD}" ;; *) BB="${POL_BRANCH}-${POL_BUILD}" ;;	esac && \
	/usr/bin/wget -q -e robots=off -A "polyglot-*-bin.zip" -nd -r -N -l1 https://opensource.ncsa.illinois.edu/bamboo/browse/${BB}/artifact/JOB1/dist/  && \
	LATEST=$( /bin/ls -1rt polyglot-*-bin.zip | tail -1 )  && \
	/usr/bin/unzip -q ${LATEST} && \
	/bin/mv $( basename ${LATEST} -bin.zip ) polyglot && \
	/bin/sed -i -e 's/^\([^#]*Scripts=\)/#\1/' polyglot/SoftwareServer.conf && \
	/bin/sed -i -e 's/^#*MongoLogging=.*$/MongoLogging=false/' polyglot/PolyglotRestlet.conf

# command to run when starting docker
COPY entrypoint.sh /
ENTRYPOINT ["/entrypoint.sh"]
CMD ["polyglot"]

