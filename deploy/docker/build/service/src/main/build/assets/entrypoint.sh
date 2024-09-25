#!/bin/bash
[[ -n $DEBUG ]] && set -x
set -eu

########################################################################################################################

my_admin_pass="${REPOSITORY_SERVICE_ADMIN_PASS:-admin}"
my_admin_pass_md4="$(printf '%s' "$my_admin_pass" | iconv -t utf16le | openssl md4 -provider legacy | awk '{ print $2 }')"

my_guest_user="${REPOSITORY_SERVICE_GUEST_USER:-}"
my_guest_pass="${REPOSITORY_SERVICE_GUEST_PASS:-}"

my_bind="${REPOSITORY_SERVICE_BIND:-"0.0.0.0"}"

my_prot_external="${REPOSITORY_SERVICE_PROT_EXTERNAL:-http}"
my_host_external="${REPOSITORY_SERVICE_HOST_EXTERNAL:-repository.127.0.0.1.nip.io}"
my_port_external="${REPOSITORY_SERVICE_PORT_EXTERNAL:-8100}"
my_path_external="${REPOSITORY_SERVICE_PATH_EXTERNAL:-/edu-sharing}"
my_base_external="${my_prot_external}://${my_host_external}:${my_port_external}${my_path_external}"
my_auth_external="${my_base_external}/services/authentication"
my_pool_external="${REPOSITORY_SERVICE_POOL_EXTERNAL:-200}"
my_wait_external="${REPOSITORY_SERVICE_WAIT_EXTERNAL:--1}"
my_proxy_buffer_size="${REPOSITORY_SERVICE_PROXY_BUFFER_SIZE:-65536}"

my_home_appid="${REPOSITORY_SERVICE_HOME_APPID:-local}"
my_home_auth="${REPOSITORY_SERVICE_HOME_AUTH:-}"
my_home_auth_external="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL:-false}"
my_home_auth_external_login="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGIN:-$my_path_external/shibboleth}"

my_home_auth_external_logout="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGOUT:-/Shibboleth.sso/Logout}"
my_home_auth_external_logout_destroy_session="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGOUT_DESTROY_SESSION:-"false"}"
my_home_auth_external_logout_redirect="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGOUT_REDIRECT:-"false"}"
my_home_auth_external_logout_redirect_url="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGOUT_REDIRECT_URL:-$my_base_external}"

my_home_auth_external_login_providers_url="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGIN_PROVIDERS_URL:-}"
my_home_auth_external_login_provider_target_url="${REPOSITORY_SERVICE_HOME_AUTH_EXTERNAL_LOGIN_PROVIDER_TARGET_URL:-}"
my_home_provider="${REPOSITORY_SERVICE_HOME_PROVIDER:-}"
my_home_url_dynamic="${REPOSITORY_SERVICE_HOME_URL_DYNAMIC:-}"
my_home_cookie_attr="${REPOSITORY_SERVICE_HOME_COOKIE_ATTRIBUTES:-}"
my_home_custom_html_headers="${REPOSITORY_SERVICE_HOME_CUSTOM_HTML_HEADERS:-}"

my_allow_origin="${REPOSITORY_SERVICE_ALLOW_ORIGIN:-}"
if [[ ! -z "$my_allow_origin" ]]; then
  my_allow_origin=",${my_allow_origin}"
fi

my_host_internal="${REPOSITORY_SERVICE_HOST_INTERNAL:-repository-service}"
my_port_internal="${REPOSITORY_SERVICE_PORT_INTERNAL:-8080}"
my_pool_internal="${REPOSITORY_SERVICE_POOL_INTERNAL:-200}"
my_wait_internal="${REPOSITORY_SERVICE_WAIT_INTERNAL:--1}"

my_mail_from="${REPOSITORY_SERVICE_MAIL_FROM:-}"
my_mail_addreplyto="${REPOSITORY_SERVICE_MAIL_ADDREPLYTO:-}"
my_mail_register_receiver="${REPOSITORY_SERVICE_MAIL_REGISTER_RECEIVER:-}"
my_mail_report_receiver="${REPOSITORY_SERVICE_MAIL_REPORT_RECEIVER:-}"
my_mail_server_smtp_host="${REPOSITORY_SERVICE_MAIL_SERVER_SMTP_HOST:-}"
my_mail_server_smtp_port="${REPOSITORY_SERVICE_MAIL_SERVER_SMTP_PORT:-}"
my_mail_server_smtp_username="${REPOSITORY_SERVICE_MAIL_SERVER_SMTP_USERNAME:-}"
my_mail_server_smtp_password="${REPOSITORY_SERVICE_MAIL_SERVER_SMTP_PASSWORD:-}"
my_mail_server_smtp_authtype="${REPOSITORY_SERVICE_MAIL_SERVER_SMTP_AUTHTYPE:-}"

my_http_client_disablesni4hosts="${REPOSITORY_SERVICE_HTTP_CLIENT_DISABLE_SNI4HOSTS:-}"
my_http_client_proxy_host="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_HOST:-}"
my_http_client_proxy_nonproxyhosts="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_NONPROXYHOSTS:-}"
my_http_client_proxy_proxyhost="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYHOST:-}"
my_http_client_proxy_proxypass="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYPASS:-}"
my_http_client_proxy_proxyport="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYPORT:-}"
my_http_client_proxy_proxyuser="${REPOSITORY_SERVICE_HTTP_CLIENT_PROXY_PROXYUSER:-}"

my_http_server_csp_connect="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_CONNECT:-}"
my_http_server_csp_default="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_DEFAULT:-}"
my_http_server_csp_font="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_FONT:-}"
my_http_server_csp_frame="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_FRAME:-}"
my_http_server_csp_img="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_IMG:-}"
my_http_server_csp_media="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_MEDIA:-}"
my_http_server_csp_object="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_OBJECT:-}"
my_http_server_csp_script="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_SCRIPT:-}"
my_http_server_csp_style="${REPOSITORY_SERVICE_HTTP_SERVER_CSP_STYLE:-}"

my_http_server_session_timeout="${REPOSITORY_SERVICE_HTTP_SERVER_SESSION_TIMEOUT:-60}"

my_http_accesslog_enabled="${REPOSITORY_SERVICE_HTTP_ACCESSLOG_ENABLED:-}"
my_http_jvmroute="${REPOSITORY_SERVICE_HTTP_JVMROUTE:-}"

my_jobs_primary_hostname="${REPOSITORY_SERVICE_JOBS_PRIMARY_HOSTNAME:-}"

cache_cluster="${CACHE_CLUSTER:-false}"
cache_database="${CACHE_DATABASE:-0}"
cache_host="${CACHE_HOST:-}"
cache_port="${CACHE_PORT:-}"

repository_database_driv="${REPOSITORY_DATABASE_DRIV:-"org.postgresql.Driver"}"
repository_database_host="${REPOSITORY_DATABASE_HOST:-repository-database}"
repository_database_name="${REPOSITORY_DATABASE_NAME:-repository}"
repository_database_opts="${REPOSITORY_DATABASE_OPTS:-}"
repository_database_pass="${REPOSITORY_DATABASE_PASS:-repository}"
repository_database_pool_max="${REPOSITORY_DATABASE_POOL_MAX:-80}"
repository_database_pool_sql="${REPOSITORY_DATABASE_POOL_SQL:-SELECT 1}"
repository_database_port="${REPOSITORY_DATABASE_PORT:-5432}"
repository_database_prot="${REPOSITORY_DATABASE_PROT:-"postgresql"}"
repository_database_user="${REPOSITORY_DATABASE_USER:-repository}"
repository_database_jdbc="jdbc:${repository_database_prot}://${repository_database_host}:${repository_database_port}/${repository_database_name}${repository_database_opts}"

repository_search_solr_host="${REPOSITORY_SEARCH_SOLR_HOST:-repository-search-solr}"
repository_search_solr_port="${REPOSITORY_SEARCH_SOLR_PORT:-8080}"

repository_transform_enabled="${REPOSITORY_TRANSFORM_ENABLED:-"true"}"
repository_transform_host="${REPOSITORY_TRANSFORM_HOST:-}"
repository_transform_port="${REPOSITORY_TRANSFORM_PORT:-}"
repository_transform_aio_host="${REPOSITORY_TRANSFORM_AIO_HOST:-}"
repository_transform_aio_port="${REPOSITORY_TRANSFORM_AIO_PORT:-}"
repository_transform_es_host="${REPOSITORY_TRANSFORM_ES_HOST:-}"
repository_transform_es_port="${REPOSITORY_TRANSFORM_ES_PORT:-}"

catSConf="tomcat/conf/server.xml"
catCConf="tomcat/conf/Catalina/localhost/edu-sharing.xml"
catEduWConf="tomcat/webapps/edu-sharing/WEB-INF/web.xml"
catAlfWConf="tomcat/webapps/alfresco/WEB-INF/web.xml"
catAlfLog="tomcat/webapps/alfresco/WEB-INF/log4j2-alfresco.properties"

eduCConf="tomcat/shared/classes/config/defaults/client.config.xml"
eduCConfX="tomcat/shared/classes/config/defaults/client.config.override.xml"

alfProps="tomcat/shared/classes/config/cluster/alfresco-global.properties"
eduSConf="tomcat/shared/classes/config/cluster/edu-sharing.deployment.conf"
homeProp="tomcat/shared/classes/config/cluster/applications/homeApplication.properties.xml"

alfExt="tomcat/shared/classes/alfresco/extension"

### Wait ###############################################################################################################

[[ -n "${cache_host}" && -n "${cache_port}" ]] && {

  until wait-for-it "${cache_host}:${cache_port}" -t 3; do sleep 1; done

  [[ "${cache_cluster}" == "true" ]] && {
    until [[ $(redis-cli --cluster info "${cache_host}" "${cache_port}" | grep '[OK]' | cut -d ' ' -f5) -gt 1 ]]; do
      echo >&2 "Waiting for ${cache_host} ..."
      sleep 3
    done
  }

}

until wait-for-it "${repository_database_host}:${repository_database_port}" -t 3; do sleep 1; done

until PGPASSWORD="${repository_database_pass}" \
  psql -h "${repository_database_host}" -p "${repository_database_port}" -U "${repository_database_user}" -d "${repository_database_name}" -c '\q'; do
  echo >&2 "Waiting for ${repository_database_host} ..."
  sleep 3
done

# jodconverter
[[ -n "${repository_transform_host}" && -n "${repository_transform_port}" ]] && {
  until wait-for-it "${repository_transform_host}:${repository_transform_port}" -t 3; do sleep 1; done
}

# core aio transformer
[[ -n "${repository_transform_aio_host}" && -n "${repository_transform_aio_port}" ]] && {
  until wait-for-it "${repository_transform_aio_host}:${repository_transform_aio_port}" -t 3; do sleep 1; done
}

# edu-sharing custom transformer
[[ -n "${repository_transform_es_host}" && -n "${repository_transform_es_port}" ]] && {
  until wait-for-it "${repository_transform_es_host}:${repository_transform_es_port}" -t 3; do sleep 1; done
}

### config #############################################################################################################

[[ -f "${eduCConfX}" ]] && {
  cp "${eduCConfX}" "${eduCConf}"
}


if [[ -f tomcat/shared/classes/config/defaults/project-version.json ]]; then
  versionFile="tomcat/shared/classes/config/defaults/project-version.json"
else
  versionFile="tomcat/shared/classes/config/defaults/repository-version.json"
fi

configs=(cluster node)
for config in "${configs[@]}"; do
  if [[ ! -f tomcat/shared/classes/config/$config/version.json ]]; then
    mkdir -p tomcat/shared/classes/config/$config
    for jar in tomcat/shared/assets/$config/*.jar; do
      if [[ -f $jar ]]; then
        unzip -o $jar -d tomcat/shared/classes/config/$config -x 'META-INF/*'
      fi
    done

    cp "${versionFile}" tomcat/shared/classes/config/$config/version.json
    cp "${versionFile}" tomcat/shared/classes/config/$config/version.json.$(date +%d-%m-%Y_%H-%M-%S)

  else
      cmp -s "${versionFile}" tomcat/shared/classes/config/$config/version.json || {
        cp "${versionFile}" tomcat/shared/classes/config/$config/version.json
        cp "${versionFile}" tomcat/shared/classes/config/$config/version.json.$(date +%d-%m-%Y_%H-%M-%S)
      }
  fi
done

reinstall.sh

########################################################################################################################

export CATALINA_OUT="/dev/stdout"

export CATALINA_OPTS="-Dfile.encoding=UTF-8 $CATALINA_OPTS"
export CATALINA_OPTS="-Duser.country=DE $CATALINA_OPTS"
export CATALINA_OPTS="-Duser.language=de $CATALINA_OPTS"

export CATALINA_OPTS="-Dorg.xml.sax.parser=com.sun.org.apache.xerces.internal.parsers.SAXParser $CATALINA_OPTS"
export CATALINA_OPTS="-Djavax.xml.parsers.DocumentBuilderFactory=com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl $CATALINA_OPTS"
export CATALINA_OPTS="-Djavax.xml.parsers.SAXParserFactory=com.sun.org.apache.xerces.internal.jaxp.SAXParserFactoryImpl $CATALINA_OPTS"
# hazelcast 5.3
#TODO Enabling XXE protection failed. The attribute http://javax.xml.XMLConstants/property/accessExternalDTD is not supported by the TransformerFactory. This usually mean an outdated XML processor is present on the classpath (e.g. Xerces, Xalan). If you are not able to resolve the issue by fixing the classpath, the hazelcast.ignoreXxeProtectionFailures system property can be used to disable XML External Entity protections. We don't recommend disabling the XXE as such the XML processor configuration is unsecure!
export CATALINA_OPTS="-Dhazelcast.ignoreXxeProtectionFailures=true $CATALINA_OPTS"
export CATALINA_OPTS="--add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED $CATALINA_OPTS"

xmlstarlet ed -L \
  -d '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/@hostConfigClass' \
  -i '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]' -t attr -n 'hostConfigClass' -v 'org.edu_sharing.catalina.startup.OrderedHostConfig' \
  ${catSConf}

xmlstarlet ed -L \
  -d '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/Valve[@className="org.apache.catalina.valves.ErrorReportValve"]' \
  -s '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]' -t elem -n 'Valve' -v '' \
  --var valve '$prev' \
  -i '$valve' -t attr -n "className" -v "org.apache.catalina.valves.ErrorReportValve" \
  -i '$valve' -t attr -n "showReport" -v "false" \
  -i '$valve' -t attr -n "showServerInfo" -v "false" \
  ${catSConf}

xmlstarlet ed -L \
  -d '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]/Valve[@className="org.apache.catalina.valves.AccessLogValve"]' \
  ${catSConf}

[[ -n $my_http_jvmroute ]] && {
  xmlstarlet ed -L \
    -i '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]]' -t attr -n 'jvmRoute' -v "${my_http_jvmroute}" \
    ${catSConf}
}

[[ -n $my_http_accesslog_enabled ]] && {
  xmlstarlet ed -L \
    -s '/Server/Service[@name="Catalina"]/Engine[@name="Catalina"]/Host[@name="localhost"]' -t elem -n 'Valve' -v '' \
    --var valve '$prev' \
    -i '$valve' -t attr -n "className" -v "org.edu_sharing.catalina.valves.StdoutAccessLogValve" \
    -i '$valve' -t attr -n "pattern" -v "%h %l %u %t &quot;%r&quot; %s %b" \
    ${catSConf}
}

xmlstarlet ed -L \
  -d '/Server/Service[@name="Catalina"]/Connector' \
  -s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
  --var internal '$prev' \
  -i '$internal' -t attr -n "address" -v "${my_bind}" \
  -i '$internal' -t attr -n "port" -v "8080" \
  -i '$internal' -t attr -n "scheme" -v "http" \
  -i '$internal' -t attr -n "protocol" -v "org.apache.coyote.http11.Http11NioProtocol" \
  -i '$internal' -t attr -n "connectionTimeout" -v "${my_wait_internal}" \
  -i '$internal' -t attr -n "maxThreads" -v "${my_pool_internal}" \
  -s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
  --var external1 '$prev' \
  -i '$external1' -t attr -n "address" -v "${my_bind}" \
  -i '$external1' -t attr -n "port" -v "8081" \
  -i '$external1' -t attr -n "scheme" -v "${my_prot_external}" \
  -i '$external1' -t attr -n "protocol" -v "org.apache.coyote.http11.Http11NioProtocol" \
  -i '$external1' -t attr -n "connectionTimeout" -v "${my_wait_external}" \
  -i '$external1' -t attr -n "maxThreads" -v "${my_pool_external}" \
  -s '/Server/Service[@name="Catalina"]' -t elem -n 'Connector' -v '' \
  --var external2 '$prev' \
  -i '$external2' -t attr -n "address" -v "${my_bind}" \
  -i '$external2' -t attr -n "port" -v "8009" \
  -i '$external2' -t attr -n "scheme" -v "${my_prot_external}" \
  -i '$external2' -t attr -n "protocol" -v "org.apache.coyote.ajp.AjpNioProtocol" \
  -i '$external2' -t attr -n "URIEncoding" -v "UTF-8" \
  -i '$external2' -t attr -n "connectionTimeout" -v "${my_wait_external}" \
  -i '$external2' -t attr -n "maxThreads" -v "${my_pool_external}" \
  -i '$external2' -t attr -n "secretRequired" -v "false" \
  -i '$external2' -t attr -n "tomcatAuthentication" -v "false" \
  -i '$external2' -t attr -n "allowedRequestAttributesPattern" -v ".*" \
  -i '$external2' -t attr -n "packetSize" -v "${my_proxy_buffer_size}" \
  ${catSConf}

[[ -n "${cache_host}" && -n "${cache_port}" ]] && {
  xmlstarlet ed -L \
    -d '/Context/Manager[@className="org.redisson.tomcat.RedissonSessionManager"]' \
    -s '/Context' -t elem -n "Manager" -v "" \
    --var redis '$prev' \
    -i '$redis' -t attr -n "className" -v "org.redisson.tomcat.RedissonSessionManager" \
    -i '$redis' -t attr -n "configPath" -v "tomcat/conf/redisson.yaml" \
    -i '$redis' -t attr -n "readMode" -v "REDIS" \
    -i '$redis' -t attr -n "updateMode" -v "DEFAULT" \
    -i '$redis' -t attr -n "broadcastSessionEvents" -v "false" \
    -i '$redis' -t attr -n "broadcastSessionUpdates" -v "false" \
    ${catCConf}

  if [[ "${cache_cluster}" == "true" ]]; then
    {
      echo "clusterServersConfig:"
      echo "  nodeAddresses:"
      echo "    - \"redis://${cache_host}:${cache_port}\""
    } >>tomcat/conf/redisson.yaml
  else
    {
      echo "singleServerConfig:"
      echo "  address: \"redis://${CACHE_HOST}:${CACHE_PORT}\""
      echo "  database: ${cache_database}"
    } >>tomcat/conf/redisson.yaml
  fi
  echo "codec: !<org.edu_sharing.redisson.Kryo5Codec> {}" >>tomcat/conf/redisson.yaml
}

### Alfresco platform ##################################################################################################

sed -i -r 's|^[#]*\s*dir\.root=.*|dir.root='"$ALF_HOME/alf_data"'|' "${alfProps}"
grep -q '^[#]*\s*dir\.root=' "${alfProps}" || echo "dir.root=$ALF_HOME/alf_data" >>"${alfProps}"

sed -i -r 's|^[#]*\s*dir\.keystore=.*|dir.keystore='"$ALF_HOME/${alfExt}/keystore"'|' "${alfProps}"
grep -q '^[#]*\s*dir\.keystore=' "${alfProps}" || echo "dir.keystore=$ALF_HOME/${alfExt}/keystore" >>"${alfProps}"

sed -i -r 's|^[#]*\s*img\.root=.*|img.root=/usr|' "${alfProps}"
grep -q '^[#]*\s*img\.root=' "${alfProps}" || echo 'img.root=/usr' >>"${alfProps}"

sed -i -r 's|^[#]*\s*img\.gslib=.*|img.gslib=/usr/bin|' "${alfProps}"
grep -q '^[#]*\s*img\.gslib=' "${alfProps}" || echo 'img.gslib=/usr/bin' >>"${alfProps}"

sed -i -r 's|^[#]*\s*exiftool\.dyn=.*|exiftool.dyn=/usr/bin|' "${alfProps}"
grep -q '^[#]*\s*exiftool\.dyn=' "${alfProps}" || echo 'exiftool.dyn=/usr/bin' >>"${alfProps}"

sed -i -r 's|^[#]*\s*exiftool\.exe=.*|exiftool.exe=${exiftool.dyn}/exiftool|' "${alfProps}"
grep -q '^[#]*\s*exiftool\.exe=' "${alfProps}" || echo 'exiftool.exe=${exiftool.dyn}/exiftool' >>"${alfProps}"

#sed -i -r 's|^[#]*\s*ffmpeg\.dyn=.*|ffmpeg.dyn=/usr/bin|' "${alfProps}"
#grep -q '^[#]*\s*ffmpeg\.dyn=' "${alfProps}" || echo 'ffmpeg.dyn=/usr/bin' >>"${alfProps}"

#sed -i -r 's|^[#]*\s*ffmpeg\.exe=.*|ffmpeg.exe=${ffmpeg.dyn}/ffmpeg|' "${alfProps}"
#grep -q '^[#]*\s*ffmpeg\.exe=' "${alfProps}" || echo 'ffmpeg.exe=${ffmpeg.dyn}/ffmpeg' >>"${alfProps}"

sed -i -r 's|^[#]*\s*img\.dyn=.*|img.dyn=/usr/bin|' "${alfProps}"
grep -q '^[#]*\s*img\.dyn=' "${alfProps}" || echo 'img.dyn=/usr/bin' >>"${alfProps}"

sed -i -r 's|^[#]*\s*img\.exe=.*|img.exe=${img.dyn}/convert|' "${alfProps}"
grep -q '^[#]*\s*img\.exe=' "${alfProps}" || echo 'img.exe=${img.dyn}/convert' >>"${alfProps}"

sed -i -r 's|^[#]*\s*alfresco_user_store\.adminpassword=.*|alfresco_user_store.adminpassword='"${my_admin_pass_md4}"'|' "${alfProps}"
grep -q '^[#]*\s*alfresco_user_store\.adminpassword=' "${alfProps}" || echo "alfresco_user_store.adminpassword=${my_admin_pass_md4}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*db\.driver=.*|db.driver='"${repository_database_driv}"'|' "${alfProps}"
grep -q '^[#]*\s*db\.driver=' "${alfProps}" || echo "db.driver=${repository_database_driv}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*db\.url=.*|db.url='"${repository_database_jdbc}"'|' "${alfProps}"
grep -q '^[#]*\s*db\.url=' "${alfProps}" || echo "db.url=${repository_database_jdbc}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*db\.username=.*|db.username='"${repository_database_user}"'|' "${alfProps}"
grep -q '^[#]*\s*db\.username=' "${alfProps}" || echo "db.username=${repository_database_user}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*db\.password=.*|db.password='"${repository_database_pass}"'|' "${alfProps}"
grep -q '^[#]*\s*db\.password=' "${alfProps}" || echo "db.password=${repository_database_pass}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*db\.pool\.max=.*|db.pool.max='"${repository_database_pool_max}"'|' "${alfProps}"
grep -q '^[#]*\s*db\.pool\.max=' "${alfProps}" || echo "db.pool.max=${repository_database_pool_max}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*db\.pool\.validate\.query=.*|db.pool.validate.query='"${repository_database_pool_sql}"'|' "${alfProps}"
grep -q '^[#]*\s*db\.pool\.validate\.query=' "${alfProps}" || echo "db.pool.validate.query=${repository_database_pool_sql}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*alfresco\.protocol=.*|alfresco.protocol='"${my_prot_external}"'|' "${alfProps}"
grep -q '^[#]*\s*alfresco\.protocol=' "${alfProps}" || echo "alfresco.protocol=${my_prot_external}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*alfresco\.host=.*|alfresco.host='"${my_host_external}"'|' "${alfProps}"
grep -q '^[#]*\s*alfresco\.host=' "${alfProps}" || echo "alfresco.host=${my_host_external}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*alfresco\.port=.*|alfresco.port='"${my_port_external}"'|' "${alfProps}"
grep -q '^[#]*\s*alfresco\.port=' "${alfProps}" || echo "alfresco.port=${my_port_external}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*alfresco-pdf-renderer\.root=.*|alfresco-pdf-renderer.root='"$ALF_HOME/common/alfresco-pdf-renderer"'|' "${alfProps}"
grep -q '^[#]*\s*alfresco-pdf-renderer\.root=' "${alfProps}" || echo "alfresco-pdf-renderer.root=$ALF_HOME/common/alfresco-pdf-renderer" >>"${alfProps}"

sed -i -r 's|^[#]*\s*alfresco-pdf-renderer\.exe=.*|alfresco-pdf-renderer.exe=${alfresco-pdf-renderer.root}/alfresco-pdf-renderer|' "${alfProps}"
grep -q '^[#]*\s*alfresco-pdf-renderer\.exe=' "${alfProps}" || echo 'alfresco-pdf-renderer.exe=${alfresco-pdf-renderer.root}/alfresco-pdf-renderer' >>"${alfProps}"

sed -i -r 's|^[#]*\s*ooo\.enabled=.*|ooo.enabled='"${repository_transform_enabled}"'|' "${alfProps}"
grep -q '^[#]*\s*ooo\.enabled=' "${alfProps}" || echo "ooo.enabled=${repository_transform_enabled}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*ooo\.exe=.*|ooo.exe=|' "${alfProps}"
grep -q '^[#]*\s*ooo\.exe=' "${alfProps}" || echo "ooo.exe=" >>"${alfProps}"

sed -i -r 's|^[#]*\s*ooo\.port=.*|ooo.port='"${repository_transform_port}"'|' "${alfProps}"
grep -q '^[#]*\s*ooo\.port=' "${alfProps}" || echo "ooo.port=${repository_transform_port}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*share\.protocol=.*|share.protocol='"${my_prot_external}"'|' "${alfProps}"
grep -q '^[#]*\s*share\.protocol=' "${alfProps}" || echo "share.protocol=${my_prot_external}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*share\.host=.*|share.host='"${my_host_external}"'|' "${alfProps}"
grep -q '^[#]*\s*share\.host=' "${alfProps}" || echo "share.host=${my_host_external}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*share\.port=.*|share.port='"${my_port_external}"'|' "${alfProps}"
grep -q '^[#]*\s*share\.port=' "${alfProps}" || echo "share.port=${my_port_external}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*solr\.host=.*|solr.host='"${repository_search_solr_host}"'|' "${alfProps}"
grep -q '^[#]*\s*solr\.host=' "${alfProps}" || echo "solr.host=${repository_search_solr_host}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*solr\.port=.*|solr.port='"${repository_search_solr_port}"'|' "${alfProps}"
grep -q '^[#]*\s*solr\.port=' "${alfProps}" || echo "solr.port=${repository_search_solr_port}" >>"${alfProps}"

sed -i -r 's|^[#]*\s*solr\.secureComms=.*|solr.secureComms=none|' "${alfProps}"
grep -q '^[#]*\s*solr\.secureComms=' "${alfProps}" || echo "solr.secureComms=none" >>"${alfProps}"

#sed -i -r 's|^[#]*\s*messaging\.broker\.url=.*|messaging.broker.url=failover:(nio://repository-activemq:61616)?timeout=3000\&jms.useCompression=true|' "${alfProps}"
#grep -q '^[#]*\s*messaging\.broker\.url=' "${alfProps}" || echo "messaging.broker.url=failover:(nio://repository-activemq:61616)?timeout=3000\&jms.useCompression=true" >>"${alfProps}"

sed -i -r 's|^[#]*\s*repo\.event2\.enabled=.*|repo.event2.enabled=false|' "${alfProps}"
grep -q '^[#]*\s*repo\.event2\.enabled=' "${alfProps}" || echo "repo.event2.enabled=false" >>"${alfProps}"

sed -i -r 's|^[#]*\s*messaging\.subsystem\.autoStart=.*|messaging.subsystem.autoStart=false|' "${alfProps}"
grep -q '^[#]*\s*messaging\.subsystem\.autoStart=' "${alfProps}" || echo "messaging.subsystem.autoStart=false" >>"${alfProps}"

sed -i -r 's|^[#]*\s*localTransform\.edu-sharing\.url=.*|localTransform.edu-sharing.url=http://'"${repository_transform_es_host}"':'"${repository_transform_es_port}"'/|' "${alfProps}"
grep -q '^[#]*\s*localTransform\.edu-sharing\.url=' "${alfProps}" || echo "localTransform.edu-sharing.url=http://${repository_transform_es_host}:${repository_transform_es_port}/" >>"${alfProps}"

sed -i -r 's|^[#]*\s*localTransform\.core-aio\.url=.*|localTransform.core-aio.url=http://'"${repository_transform_aio_host}"':'"${repository_transform_aio_port}"'/|' "${alfProps}"
grep -q '^[#]*\s*localTransform\.core-aio\.url=' "${alfProps}" || echo "localTransform.core-aio.url=http://${repository_transform_aio_host}:${repository_transform_aio_port}/" >>"${alfProps}"

xmlstarlet ed -L \
  -s '_:web-app/_:filter[_:filter-name="X509AuthFilter"]' -t elem -n "init-param" -v '' \
  --var param '$prev' \
  -s '$param' -t elem -n "param-name" -v "allow-unauthenticated-solr-endpoint" \
  -s '$param' -t elem -n "param-value" -v "true" \
  ${catAlfWConf}

[[ -f ${catAlfLog} ]] && {

  cp ${catAlfLog} tomcat/webapps/alfresco/WEB-INF/classes/log4j2.properties

}

### edu-sharing ########################################################################################################

my_origin="${my_prot_external}://${my_host_external}"
[[ $my_port_external != "80" && $my_port_external != "443" ]] && {
  my_origin="${my_origin}:${my_port_external}"
}

xmlstarlet ed -L \
  -u '/properties/entry[@key="appid"]' -v "${my_home_appid}" \
  -u '/properties/entry[@key="authenticationwebservice"]' -v "${my_auth_external}" \
  -u '/properties/entry[@key="clientport"]' -v "${my_port_external}" \
  -u '/properties/entry[@key="clientprotocol"]' -v "${my_prot_external}" \
  -u '/properties/entry[@key="domain"]' -v "${my_host_external}" \
  -u '/properties/entry[@key="host"]' -v "${my_host_internal}" \
  -u '/properties/entry[@key="password"]' -v "${my_admin_pass}" \
  -u '/properties/entry[@key="port"]' -v "${my_port_internal}" \
  -u '/properties/entry[@key="allow_origin"]' -v "${my_origin},http://localhost:54361${my_allow_origin}" \
  ${homeProp}

xmlstarlet ed -L \
  -d '/properties/entry[@key="cookie_attributes"]' \
  ${homeProp}

[[ -n "${my_home_cookie_attr}" ]] && {
  xmlstarlet ed -L \
    -s '/properties' -t elem -n "entry" -v "${my_home_cookie_attr}" \
    --var entry '$prev' \
    -i '$entry' -t attr -n "key" -v "cookie_attributes" \
    ${homeProp}
}

xmlstarlet ed -L \
  -d '/properties/entry[@key="custom_html_headers"]' \
  ${homeProp}

[[ -n "${my_home_custom_html_headers}" ]] && {
  xmlstarlet ed -L \
    -s '/properties' -t elem -n "entry" -v "${my_home_custom_html_headers}" \
    --var entry '$prev' \
    -i '$entry' -t attr -n "key" -v "custom_html_headers" \
    ${homeProp}
}

xmlstarlet ed -L \
  -d '/properties/entry[@key="guest_username"]' \
  ${homeProp}

[[ -n "${my_guest_user}" ]] && {
  xmlstarlet ed -L \
    -s '/properties' -t elem -n "entry" -v "${my_guest_user}" \
    --var entry '$prev' \
    -i '$entry' -t attr -n "key" -v "guest_username" \
    ${homeProp}
}

xmlstarlet ed -L \
  -d '/properties/entry[@key="guest_password"]' \
  ${homeProp}

[[ -n "${my_guest_pass}" ]] && {
  xmlstarlet ed -L \
    -s '/properties' -t elem -n "entry" -v "${my_guest_pass}" \
    --var entry '$prev' \
    -i '$entry' -t attr -n "key" -v "guest_password" \
    ${homeProp}
}

[[ -n "${my_home_auth}" ]] && {

  if [[ "${my_home_auth_external}" == "true" ]]; then
    xmlstarlet ed -L \
      -s '/config/values' -t elem -n 'loginUrl' -v '' \
      -d '/config/values/loginUrl[position() != 1]' \
      -u '/config/values/loginUrl' -v "${my_home_auth_external_login}" \
      -s '/config/values' -t elem -n 'loginProvidersUrl' -v '' \
      -d '/config/values/loginProvidersUrl[position() != 1]' \
      -u '/config/values/loginProvidersUrl' -v "${my_home_auth_external_login_providers_url}" \
      -s '/config/values' -t elem -n 'loginProviderTargetUrl' -v '' \
      -d '/config/values/loginProviderTargetUrl[position() != 1]' \
      -u '/config/values/loginProviderTargetUrl' -v "${my_home_auth_external_login_provider_target_url}" \
      -s '/config/values' -t elem -n 'logout' -v '' \
      -d '/config/values/logout[position() != 1]' \
      -s '/config/values/logout' -t elem -n 'url' -v '' \
      -d '/config/values/logout/url[position() != 1]' \
      -u '/config/values/logout/url' -v "${my_home_auth_external_logout}" \
      -s '/config/values/logout' -t elem -n 'destroySession' -v '' \
      -d '/config/values/logout/destroySession[position() != 1]' \
      -u '/config/values/logout/destroySession' -v "${my_home_auth_external_logout_destroy_session}" \
      ${eduCConf}

    if [[ "${my_home_auth_external_logout_redirect}" == "true" ]]; then
      xmlstarlet ed -L \
        -s '/config/values/logout' -t elem -n 'ajax' -v '' \
        -d '/config/values/logout/ajax[position() != 1]' \
        -u '/config/values/logout/ajax' -v 'true' \
        -s '/config/values/logout' -t elem -n 'next' -v '' \
        -d '/config/values/logout/next[position() != 1]' \
        -u '/config/values/logout/next' -v "${my_home_auth_external_logout_redirect_url}" \
        ${eduCConf}
    fi
  else
    sed -i -r 's|<!--\s*SAML||g' tomcat/webapps/edu-sharing/WEB-INF/web.xml
    sed -i -r 's|SAML\s*-->||g' tomcat/webapps/edu-sharing/WEB-INF/web.xml
    xmlstarlet ed -L \
      -s '/config/values' -t elem -n 'loginUrl' -v '' \
      -d '/config/values/loginUrl[position() != 1]' \
      -u '/config/values/loginUrl' -v "${my_path_external}/shibboleth" \
      -s '/config/values' -t elem -n 'logout' -v '' \
      -d '/config/values/logout[position() != 1]' \
      -s '/config/values/logout' -t elem -n 'url' -v '' \
      -d '/config/values/logout/url[position() != 1]' \
      -u '/config/values/logout/url' -v "${my_path_external}/logout" \
      -s '/config/values/logout' -t elem -n 'destroySession' -v '' \
      -d '/config/values/logout/destroySession[position() != 1]' \
      -u '/config/values/logout/destroySession' -v 'false' \
      ${eduCConf}
  fi
}

xmlstarlet ed -L \
  -d '/properties/entry[@key="remote_provider"]' \
  ${homeProp}

[[ -n "${my_home_provider}" ]] && {
  xmlstarlet ed -L \
    -s '/properties' -t elem -n "entry" -v "${my_home_provider}" \
    --var entry '$prev' \
    -i '$entry' -t attr -n "key" -v "remote_provider" \
    ${homeProp}
}

[[ -n "${my_home_url_dynamic}" ]] && {
	xmlstarlet ed -L \
		-s '/properties' -t elem -n "entry" -v "${my_home_url_dynamic}" \
		--var entry '$prev' \
		-i '$entry' -t attr -n "key" -v "url_dynamic" \
		${homeProp}
}


[[ $(hocon -f ${eduSConf} get "repository.mail.from" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.from"
}
[[ -n "${my_mail_from}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.from" '"'"${my_mail_from}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.addReplyTo" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.addReplyTo"
}
[[ -n "${my_mail_addreplyto}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.addReplyTo" '"'"${my_mail_addreplyto}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.register.receiver" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.register.receiver"
}
[[ -n "${my_mail_register_receiver}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.register.receiver" '"'"${my_mail_register_receiver}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.report.receivers" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.report.receivers"
}
[[ -n "${my_mail_report_receiver}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.report.receivers" "[\"${my_mail_report_receiver//,/\",\"}\"]"
}

[[ $(hocon -f ${eduSConf} get "repository.mail.server.smtp.host" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.server.smtp.host"
}
[[ -n "${my_mail_server_smtp_host}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.server.smtp.host" '"'"${my_mail_server_smtp_host}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.server.smtp.port" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.server.smtp.port"
}
[[ -n "${my_mail_server_smtp_port}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.server.smtp.port" '"'"${my_mail_server_smtp_port}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.server.smtp.username" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.server.smtp.username"
}
[[ -n "${my_mail_server_smtp_username}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.server.smtp.username" '"'"${my_mail_server_smtp_username}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.server.smtp.password" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.server.smtp.password"
}
[[ -n "${my_mail_server_smtp_password}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.server.smtp.password" '"'"${my_mail_server_smtp_password}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.mail.server.smtp.authtype" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.mail.server.smtp.authtype"
}
[[ -n "${my_mail_server_smtp_authtype}" ]] && {
  hocon -f ${eduSConf} set "repository.mail.server.smtp.authtype" '"'"${my_mail_server_smtp_authtype}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.disableSNI4Hosts" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.disableSNI4Hosts"
}
[[ -n "${my_http_client_disablesni4hosts}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.disableSNI4Hosts" '"'"${my_http_client_disablesni4hosts}"'"'
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.proxy.host" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.proxy.host"
}
[[ -n "${my_http_client_proxy_host}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.proxy.host" '"'"${my_http_client_proxy_host}"'"'
}

[[ -n "${my_http_client_proxy_nonproxyhosts}" ]] && {
  export CATALINA_OPTS="-Dhttp.nonProxyHosts=\"${my_http_client_proxy_nonproxyhosts//,/|}\" $CATALINA_OPTS"
  export CATALINA_OPTS="-Dhttps.nonProxyHosts=\"${my_http_client_proxy_nonproxyhosts//,/|}\" $CATALINA_OPTS"
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.proxy.nonproxyhosts" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.proxy.nonproxyhosts"
}
[[ -n "${my_http_client_proxy_nonproxyhosts}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.proxy.nonproxyhosts" '"'"${my_http_client_proxy_nonproxyhosts}"'"'
}

[[ -n "${my_http_client_proxy_proxyhost}" ]] && {
  export CATALINA_OPTS="-Dhttp.proxyHost=${my_http_client_proxy_proxyhost} $CATALINA_OPTS"
  export CATALINA_OPTS="-Dhttps.proxyHost=${my_http_client_proxy_proxyhost} $CATALINA_OPTS"
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.proxy.proxyhost" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.proxy.proxyhost"
}
[[ -n "${my_http_client_proxy_proxyhost}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.proxy.proxyhost" '"'"${my_http_client_proxy_proxyhost}"'"'
}

[[ -n "${my_http_client_proxy_proxypass}" ]] && {
  export CATALINA_OPTS="-Dhttp.proxyPass=${my_http_client_proxy_proxypass} $CATALINA_OPTS"
  export CATALINA_OPTS="-Dhttps.proxyPass=${my_http_client_proxy_proxypass} $CATALINA_OPTS"
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.proxy.proxypass" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.proxy.proxypass"
}
[[ -n "${my_http_client_proxy_proxypass}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.proxy.proxypass" '"'"${my_http_client_proxy_proxypass}"'"'
}

[[ -n "${my_http_client_proxy_proxyport}" ]] && {
  export CATALINA_OPTS="-Dhttp.proxyPort=${my_http_client_proxy_proxyport} $CATALINA_OPTS"
  export CATALINA_OPTS="-Dhttps.proxyPort=${my_http_client_proxy_proxyport} $CATALINA_OPTS"
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.proxy.proxyport" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.proxy.proxyport"
}
[[ -n "${my_http_client_proxy_proxyport}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.proxy.proxyport" '"'"${my_http_client_proxy_proxyport}"'"'
}

[[ -n "${my_http_client_proxy_proxyuser}" ]] && {
  export CATALINA_OPTS="-Dhttp.proxyUser=${my_http_client_proxy_proxyuser} $CATALINA_OPTS"
  export CATALINA_OPTS="-Dhttps.proxyUser=${my_http_client_proxy_proxyuser} $CATALINA_OPTS"
}

[[ $(hocon -f ${eduSConf} get "repository.httpclient.proxy.proxyuser" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "repository.httpclient.proxy.proxyuser"
}
[[ -n "${my_http_client_proxy_proxyuser}" ]] && {
  hocon -f ${eduSConf} set "repository.httpclient.proxy.proxyuser" '"'"${my_http_client_proxy_proxyuser}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.default-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.default-src"
}
[[ -n "${my_http_server_csp_default}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.default-src" '"'"${my_http_server_csp_default}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.connect-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.connect-src"
}
[[ -n "${my_http_server_csp_connect}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.connect-src" '"'"${my_http_server_csp_connect}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.img-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.img-src"
}
[[ -n "${my_http_server_csp_img}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.img-src" '"'"${my_http_server_csp_img}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.script-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.script-src"
}
[[ -n "${my_http_server_csp_script}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.script-src" '"'"${my_http_server_csp_script}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.font-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.font-src"
}
[[ -n "${my_http_server_csp_font}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.font-src" '"'"${my_http_server_csp_font}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.frame-ancestors" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.frame-ancestors"
}
[[ -n "${my_http_server_csp_frame}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.frame-ancestors" '"'"${my_http_server_csp_frame}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.media-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.media-src"
}
[[ -n "${my_http_server_csp_media}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.media-src" '"'"${my_http_server_csp_media}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.object-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.object-src"
}
[[ -n "${my_http_server_csp_object}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.object-src" '"'"${my_http_server_csp_object}"'"'
}

[[ $(hocon -f ${eduSConf} get "angular.headers.Content-Security-Policy.style-src" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "angular.headers.Content-Security-Policy.style-src"
}
[[ -n "${my_http_server_csp_style}" ]] && {
  hocon -f ${eduSConf} set "angular.headers.Content-Security-Policy.style-src" '"'"${my_http_server_csp_style}"'"'
}

[[ $(hocon -f ${eduSConf} get "jobs.primaryHostname" 2>/dev/null) ]] && {
  hocon -f ${eduSConf} unset "jobs.primaryHostname"
}
[[ -n "${my_jobs_primary_hostname}" ]] && {
  hocon -f ${eduSConf} set "jobs.primaryHostname" '"'"${my_jobs_primary_hostname}"'"'
}

# clean up empty lines in config after hocon commands
sed -i '/^[[:space:]]*$/d' ${eduSConf}

xmlstarlet ed -L \
  -N x="http://java.sun.com/xml/ns/javaee" \
  -u '/x:web-app/x:session-config/x:session-timeout' -v "${my_http_server_session_timeout}" \
  ${catEduWConf}

########################################################################################################################

# PLUGIN
for entrypoint in bin/plugins/plugin-*/entrypoint.sh; do
  [[ -f $entrypoint ]] && {
    source "$entrypoint" || exit 1
  }
done

########################################################################################################################

exec /opt/bitnami/scripts/tomcat/entrypoint.sh "$@"
