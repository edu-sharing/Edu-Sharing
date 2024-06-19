FROM ${docker.from.edu_sharing.community.common.alfresco.repository}

########################################################################################################################

USER root

RUN set -eux \
    && mkdir -p alf_safe \
    && mkdir -p tomcat/shared/classes/config/defaults \
    && mkdir -p tomcat/shared/classes/config/cluster/applications \
    && mkdir -p tomcat/shared/classes/config/node \
    && chown -RL worker:worker . \
    && chown -RL worker:worker /tmp \
    && rm \
        tomcat/webapps/alfresco/WEB-INF/lib/jsr305-* \
        tomcat/webapps/alfresco/WEB-INF/lib/spring-security-*

COPY --chown=worker:worker artifacts/edu_sharing-community-repository-backend-tomcat-${org.edu_sharing:edu_sharing-community-repository-backend-tomcat:jar.version}.jar tomcat/lib/
COPY --chown=worker:worker webapp/*.war tomcat/webapps/edu-sharing.war

RUN set -eux \
    && mkdir -p tomcat/webapps/edu-sharing \
	&& unzip tomcat/webapps/edu-sharing.war -d tomcat/webapps/edu-sharing \
	&& rm -f tomcat/webapps/edu-sharing.war \
	&& cp tomcat/webapps/edu-sharing/META-INF/context.xml tomcat/conf/Catalina/localhost/edu-sharing.xml

COPY --chown=worker:worker amps amps/
COPY --chown=worker:worker shared tomcat/shared/

RUN set -eux \
    && chown -RL worker:worker alf_data \
    && chown -RL worker:worker alf_safe \
    && chown -RL worker:worker tomcat/conf \
    && chown -RL worker:worker tomcat/shared \
    && chown -RL worker:worker tomcat/webapps \
    && chown -RL worker:worker tomcat/work \
    && chown -RL worker:worker tomcat/temp

########################################################################################################################

COPY --chown=worker:worker assets/entrypoint.sh assets/reinstall.sh assets/libcheck.sh bin/
RUN set -eux \
	&& find bin -type f -name '*.sh' -exec chmod +x {} \;

########################################################################################################################

USER worker

EXPOSE 8009 8080 8081

ENTRYPOINT ["entrypoint.sh"]
CMD [ "/opt/bitnami/scripts/tomcat/run.sh" ]

VOLUME [ "/opt/alfresco/alf_data", "/opt/alfresco/alf_safe", "/opt/bitnami/tomcat/shared/classes/config/cluster", "/opt/bitnami/tomcat/shared/classes/config/node" ]

########################################################################################################################

LABEL git.branch=${git.branch} \
git.closest.tag.name=${git.closest.tag.fixed} \
git.commit.id=${git.commit.id} \
git.dirty=${git.dirty} \
mvn.project.artifactId=${project.artifactId} \
mvn.project.groupId=${project.groupId} \
mvn.project.version=${project.version}
