package org.edu_sharing.service.version;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class RepositoryVersionInfo implements Serializable {
    public Version version;
    public VersionMaven maven;
    public VersionGit git;

    public static class Version implements Serializable {
        public String full;
        public String major;
        public String minor;
        public String patch;
        public String qualifier;
        public String build;
    }

    public static class VersionGit implements Serializable {
        public String branch;
        public VersionGitCommit commit;
        public static class VersionGitCommit implements Serializable {
            public String id;
            public VersionTimestamp timestamp;
        }
    }
    public static class VersionTimestamp implements Serializable {
        public String datetime;
    }


    public static class VersionMaven implements Serializable {
        public Map<String, String> bom;
        public VersionProject project;

        public static class VersionProject implements Serializable {
            public String artifactId;
            public String groupId;
            public String version;
        }
    }
}
