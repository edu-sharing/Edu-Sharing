package org.edu_sharing.service.version;

import java.io.Serializable;

public class RepositoryVersionInfo implements Serializable {
    public String repository;
    public Version version;
//    public VersionMaven maven;
    public VersionGit git;

    public VersionBuild build;

    public static class VersionBuild implements Serializable {
        public String timestamp;
    }

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

//
//    public static class VersionMaven implements Serializable {
//        public HashMap<String, String> bom;
//        public VersionProject project;
//
//        public static class VersionProject implements Serializable {
//            public String artifactId;
//            public String groupId;
//            public String version;
//        }
//    }
}
