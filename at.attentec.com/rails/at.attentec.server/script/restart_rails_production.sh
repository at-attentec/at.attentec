#!/bin/sh
rails_env="production"
port="3000"
if [[ $# -eq 1 ]]; then
	port=$1
fi
dir=`dirname $0`
dir=$dir"/../"
cd $dir

#kill the last server
pid=`cat tmp/pids/server.pid`
echo "Killing old server with PID "$pid
kill $pid

echo "Starting rails with ENV=$rails_env, PORT=$port"
#migrate changes in database
rake db:migrate RAILS_ENV=$rails_env
ruby script/server mongrel -e $rails_env -d -p $port