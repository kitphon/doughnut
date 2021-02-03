#!/bin/sh

# Set the metadata server to the get projct id
PROJECTID=$(curl -s "http://metadata.google.internal/computeMetadata/v1/project/project-id" -H "Metadata-Flavor: Google")
BUCKET=$(curl -s "http://metadata.google.internal/computeMetadata/v1/instance/attributes/BUCKET" -H "Metadata-Flavor: Google")
ARTIFACT="doughnut"
VERSION="0.0.1-SNAPSHOT"
ARCH="linux_amd64"
TRAEFIK_VERSION="v2.4.2"

echo "Project ID: ${PROJECTID} Bucket: ${BUCKET}"

# Get the files we need
mkdir -p /opt/doughnut_app
gsutil cp gs://${BUCKET}/${ARTIFACT}-${VERSION}.jar /opt/doughnut_app/${ARTIFACT}-${VERSION}.jar

# Install dependencies
apt-get update
apt-get -y install jq openjdk-11-jdk gnupg gnupg-agent mariadb-client mariadb-backup

# Make Java 11 default
update-alternatives --set java /usr/lib/jvm/java-11-openjdk-amd64/jre/bin/java

# Download and setup traefik-v2
mkdir -p /opt/traefik/{logs,dynamic/conf}
curl -sL https://github.com/traefik/traefik/releases/download/${TRAEFIK_VERSION}/traefik_${TRAEFIK_VERSION}_${ARCH}.tar.gz > /opt/traefik/traefik_${TRAEFIK_VERSION}_${ARCH}.tar.gz | tar xz -C /opt/traefik/

# traefik static toml config
cat <<'EOF' >/opt/traefik/traefik.toml
defaultEntryPoints = ["http"]

[entryPoints]
  [entryPoints.web]
    address = ":80"

[providers]
  [providers.file]
    directory = /opt/traefik/dynamic/conf"
EOF

# traefik dynamic toml config
cat <<'EOF' >/opt/traefik/dynamic/conf/dynamic.toml
[http]
  [http.routers]
    # Define a connection between requests and services
    [http.routers.to-doughnut-app]
      rule = "Host(`localhost`) && PathPrefix(`/`)"
      # If the rule matches, applies the middleware
      # If the rule matches, forward to the doughnut-app service
      service = "doughnut-app"

  [http.services]
    # Define how to reach an existing service on our infrastructure
    [http.services.doughnut-app.loadBalancer]
      [[http.services.doughnut-app.loadBalancer.servers]]
        url = "http://127.0.0.1:8081"
EOF

sh -c "/opt/traefik/traefik --configfile=/opt/traefik/traefik.toml --accesslog=true --accesslog.filepath=/opt/traefik/logs/access.log --log=true --log.filepath=/opt/traefik/logs/traefik.log --log.level=INFO" &

ACCESS_TOKEN=$(curl "http://metadata.google.internal/computeMetadata/v1/instance/service-accounts/default/token" \
	-H "Metadata-Flavor: Google" | jq -r ".access_token")

MYSQL_PASSWORD=$(curl "https://secretmanager.googleapis.com/v1/projects/${PROJECTID}/secrets/mysql_password/versions/1:access" \
	--request "GET" \
	--header "authorization: Bearer ${ACCESS_TOKEN}" \
	--header "content-type: application/json" \
	--header "x-goog-user-project: ${PROJECTID}" |
	jq -r ".payload.data" | base64 --decode)

# define db-server entry pointing to IP address of mariadb instance
echo "10.142.0.18	db-server" >> /etc/hosts

# Start server
sh -c "java -jar -Dspring-boot.run.profiles=prod -Dspring.datasource.password=${MYSQL_PASSWORD} /opt/doughnut_app/${ARTIFACT}-${VERSION}.jar" &
