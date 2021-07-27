@echo off
title Development v40 Beta Edition
set CLASSPATH=.;dist\odinms.jar;dist\mina-core.jar;dist\slf4j-api.jar;dist\slf4j-jdk14.jar;dist\mysql-connector-java-bin.jar
java -Dnet.sf.odinms.recvops=recvops.properties -Dnet.sf.odinms.sendops=sendops.properties -Dnet.sf.odinms.wzpath=wz/ -Djavax.net.ssl.keyStore=login.keystore -Djavax.net.ssl.keyStorePassword=football -Djavax.net.ssl.trustStore=login.truststore -Djavax.net.ssl.trustStorePassword=football net.sf.odinms.net.login.LoginServer
pause