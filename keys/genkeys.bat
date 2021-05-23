@echo off

rem Generate Janez's public/private key pair into private keystore
echo Generating  Janez's public private key pair
keytool -genkey -alias janezprivate -keystore janez.private -storetype PKCS12 -keyalg rsa -dname "CN=Janez" -storepass janezpwd -keypass janezpwd -validity 365

rem Generate milka's public/private key pair into private keystore
echo Generating Milkas's public private key pair
keytool -genkey -alias milkaprivate -keystore milka.private -storetype PKCS12 -keyalg rsa -dname "CN=Milka" -storepass milkapwd -keypass milkapwd -validity 365

rem Generate Luka's public/private key pair into private keystore
echo Generating  Lukas's public private key pair
keytool -genkey -alias lukaprivate -keystore luka.private -storetype PKCS12 -keyalg rsa -dname "CN=Luka" -storepass lukapwd -keypass lukapwd -validity 365

rem Generate server public/private key pair
echo Generating server public private key pair
keytool -genkey -alias serverprivate -keystore server.private -storetype PKCS12 -keyalg rsa -dname "CN=localhost" -storepass serverpwd -keypass serverpwd -validity 365

rem Export client public key and import it into public keystore
echo Generating client public key file (Rdeca Kapica, Milka)
keytool -export -alias janezprivate -keystore janez.private -file temp.key -storepass janezpwd
keytool -import -noprompt -alias janezpublic -keystore client.public -file temp.key -storepass public
del temp.key
keytool -export -alias milkaprivate -keystore milka.private -file temp.key -storepass milkapwd
keytool -import -noprompt -alias milkapublic -keystore client.public -file temp.key -storepass public
del temp.key
keytool -export -alias lukaprivate -keystore luka.private -file temp.key -storepass lukapwd
keytool -import -noprompt -alias lukapublic -keystore client.public -file temp.key -storepass public
del temp.key


rem Export server public key and import it into public keystore
echo Generating server public key file
keytool -export -alias serverprivate -keystore server.private -file temp.key -storepass serverpwd
keytool -import -noprompt -alias serverpublic -keystore server.public -file temp.key -storepass public
del temp.key
