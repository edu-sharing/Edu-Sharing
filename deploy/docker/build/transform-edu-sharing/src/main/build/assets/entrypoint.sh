#!/bin/bash
set -eux

########################################################################################################################

my_bind="${REPOSITORY_TRANSFORM_EDU_SHARING_BIND:-"0.0.0.0"}"
my_port="${REPOSITORY_TRANSFORM_EDU_SHARING_PORT_HTTP:-8091}"
my_ms_port="${REPOSITORY_TRANSFORM_EDU_SHARING_MS_PORT_HTTP:-8092}"

########################################################################################################################

touch application.properties
sed -i -r 's|^[#]*\s*server\.address=.*|server.address='"${my_bind}"'|' "application.properties"
grep -q '^[#]*\s*server\.address=' "application.properties" || echo "server.address=${my_bind}" >>"application.properties"

sed -i -r 's|^[#]*\s*server\.port=.*|server.port='"${my_port}"'|' "application.properties"
grep -q '^[#]*\s*server\.port=' "application.properties" || echo "server.port=${my_port}" >>"application.properties"

sed -i -r 's|^[#]*\s*management\.server\.address=.*|management.server.address='"${my_bind}"'|' "application.properties"
grep -q '^[#]*\s*management\.server\.address=' "application.properties" || echo "management.server.address=${my_bind}" >>"application.properties"

sed -i -r 's|^[#]*\s*management\.server\.port=.*|management.server.port='"${my_ms_port}"'|' "application.properties"
grep -q '^[#]*\s*management\.server\.port=' "application.properties" || echo "management.server.port=${my_ms_port}" >>"application.properties"
########################################################################################################################

#exec "java" "-jar" "edu_sharing-community-repository-backend-transform-${org.edu_sharing:edu_sharing-community-repository-backend-transform:jar.version}.jar" "${JAVA_OPTS:-}" "$@"
exec java ${JAVA_OPTS:-} -jar "edu_sharing-community-repository-backend-transform-${org.edu_sharing:edu_sharing-community-repository-backend-transform:jar.version}.jar" $@