#!/bin/bash
set -o errexit
set -o xtrace

# Install jq
dpkg -s jq || sudo apt-get install -y jq

# Install Confluent platform (includes Kafka schema registry)
wget -qO - http://packages.confluent.io/deb/3.3/archive.key | sudo apt-key add -
sudo add-apt-repository "deb http://packages.confluent.io/deb/3.3 stable main"
sudo apt-get update
dpkg -s confluent-platform-oss-2.11 || sudo apt-get install -y confluent-platform-oss-2.11

# Install SBT (Scala build tool)
echo "deb https://dl.bintray.com/sbt/debian /" | sudo tee -a /etc/apt/sources.list.d/sbt.list
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 2EE0EA64E40A89B84B2DF73499E82A75642AC823
sudo apt-get update
dpkg -s sbt || sudo apt-get install -y sbt

echo "Installation successful."
