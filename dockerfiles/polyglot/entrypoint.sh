#!/bin/bash
set -e

if [ -n "$RABBITMQ_PORT_5672_TCP_PORT" ]; then
	RABBITMQURI="amqp://guest:guest@${RABBITMQ_PORT_5672_TCP_ADDR}:${RABBITMQ_PORT_5672_TCP_PORT}/%2F"
fi
if [ -n "$MONGO_PORT_27017_TCP_ADDR" ]; then
	MONGO_SERVER="$MONGO_PORT_27017_TCP_ADDR"
fi

if [ "$1" = 'polyglot' ]; then
	# setup polyglot
	cd /home/polyglot/polyglot
	if [ "$POL_IP" != "" ]; then
		/bin/echo "POLYGLOT_IP=${POL_IP}" >> PolyglotStewardAMQ.conf
	fi
	if [ "$POL_AUTH" != "" ]; then
		/bin/sed -i -e "s#.*Authentication=.*#Authentication=${POL_AUTH}#" PolyglotRestlet.conf
	fi
	/bin/echo "DownloadSSFile=true" >> PolyglotRestlet.conf

	# connect to other servers
	if [ "$RABBITMQURI" != "" ]; then
		/bin/sed -i -e "s#RabbitMQURI=.*#RabbitMQURI=${RABBITMQURI}#" PolyglotStewardAMQ.conf
	fi	
	if [ "$MONGO_SERVER" != "" ]; then
		/bin/sed -i -e "s/server=.*$/server=${MONGO_SERVER}/" mongo.properties
	fi
	if [ "$MONGO_DATABASE" != "" ]; then
		/bin/sed -i -e "s/database=.*$/database=${MONGO_DATABASE}/" mongo.properties
	fi
	if [ "$MONGO_LOGGING" != "false" ]; then
		/bin/sed -i -e "s/^#*MongoLogging=.*$/MongoLogging=${MONGO_LOGGING}/" PolyglotRestlet.conf
	fi

	# Sleep a while to wait for rabbitmq and mongo to start.
	/bin/sleep 10

	# start polyglot
	exec /usr/bin/java -cp polyglot.jar:lib/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.polyglot.PolyglotRestlet

elif [ "$1" = 'softwareserver' ]; then
	# setup softwareserver
	cd /home/polyglot/polyglot	
	if [ "$RABBITMQURI" != "" ]; then
		/bin/sed -i -e "s#RabbitMQURI=.*#RabbitMQURI=${RABBITMQURI}#" SoftwareServerRestlet.conf
	fi
	if [ "$PUBLICIP" != "" ]; then
		/bin/echo "PublicIp=${PUBLICIP}" >> SoftwareServerRestlet.conf
	fi

	# Sleep a while to wait for rabbitmq and mongo to start.
	/bin/sleep 10

	# start softwareserver
	exec /usr/bin/java -cp polyglot.jar:lib/* -Xmx1g edu.illinois.ncsa.isda.softwareserver.SoftwareServerRestlet
fi

exec "$@"

