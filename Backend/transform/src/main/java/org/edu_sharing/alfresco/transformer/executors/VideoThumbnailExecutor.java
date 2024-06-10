package org.edu_sharing.alfresco.transformer.executors;

import org.alfresco.transform.base.TransformManager;
import org.alfresco.transform.base.executors.AbstractCommandExecutor;
import org.alfresco.transform.base.executors.RuntimeExec;
import org.alfresco.transform.base.util.CustomTransformerFileAdaptor;
import org.alfresco.transform.common.RequestParamMap;

import org.apache.commons.lang3.StringUtils;
import org.edu_sharing.alfresco.transformer.executors.tools.Commands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.alfresco.transform.base.util.Util.stringToLong;

@Component
public class VideoThumbnailExecutor extends AbstractCommandExecutor implements CustomTransformerFileAdaptor {

    private static final Logger logger = LoggerFactory.getLogger(VideoThumbnailExecutor.class);

    public static String ID = "EduSharingVideoThumbnailExecutor";

    @Override
    protected RuntimeExec createTransformCommand() {

        /**
         * ProcessBuilder pb;
         *         		// create gif animation
         * 				if(format.equals(Format.gif)) {
         * 					String FRAME_COUNT = "5";
         * 					String GIF_FRAMERATE = "1";
         * 					pb = new ProcessBuilder("ffmpeg", "-i", sourceFile.getCanonicalPath(), "-t", FRAME_COUNT, "-preset", "ultrafast", "-lavfi", "setpts=0.075*PTS,scale=400:-1", "-r", GIF_FRAMERATE, "-f", "gif", "-y", targetFile.getCanonicalPath());
         * 					//ffmpeg -i <video> -vf  "thumbnail,scale=640:360" -frames:v 1 -ss 1 <image>
         *                                }
         * 				else{
         * 					// create webp animation
         * 					String TIME_COUNT="15";
         * 					String WEBP_FRAMERATE="1";
         * 					String WEBP_QUALITY="20";
         * 					pb = new ProcessBuilder("ffmpeg", "-i",sourceFile.getCanonicalPath(),"-t",TIME_COUNT,"-loop","0","-q",WEBP_QUALITY,"-filter","setpts=0.15*PTS,scale=400:-1","-r",WEBP_FRAMERATE,"-f","webp","-y",targetFile.getCanonicalPath());
         *                }
         */

        RuntimeExec runtimeExec = new RuntimeExec();
        Map<String, String[]> commandsAndArguments = new HashMap<>();
        commandsAndArguments.put(".*", new String[] { "ffmpeg", "SPLIT:${options}"});
        runtimeExec.setCommandsAndArguments(commandsAndArguments);

        /*Map<String, String> defaultProperties = new HashMap<>();
        defaultProperties.put("key", null);
        runtimeExec.setDefaultProperties(defaultProperties);*/

        runtimeExec.setErrorCodes("1");

        return runtimeExec;
    }

    @Override
    public void transform(String sourceMimetype, String targetMimetype, Map<String, String> transformOptions, File sourceFile, File targetFile, TransformManager transformManager) throws Exception {
        logger.info("sourceMimetype:"+sourceMimetype+" targetMimetype:"+targetMimetype+" sourceFile:"+sourceFile +" targetFile:"+targetFile);
        if(transformOptions != null)
            transformOptions.forEach((key, value) -> System.out.println("o:" + key + " " + value));

        List<String> comandAndArgs = null;
        try {
            if (targetMimetype.contains("gif")) {

                String FRAME_COUNT = "5";
                String GIF_FRAMERATE = "1";
                comandAndArgs = Stream.of("-i", sourceFile.getCanonicalPath(), "-t", FRAME_COUNT, "-preset", "ultrafast", "-lavfi", "setpts=0.075*PTS,scale=400:-1", "-r", GIF_FRAMERATE, "-f", "gif", "-y", targetFile.getCanonicalPath())
                        .collect(Collectors.toList());
            }else{
                String TIME_COUNT="15";
                String WEBP_FRAMERATE="1";
                String WEBP_QUALITY="20";
                comandAndArgs = Stream.of("-i",sourceFile.getCanonicalPath(),"-t",TIME_COUNT,"-loop","0","-q",WEBP_QUALITY,"-filter","setpts=0.15*PTS,scale=400:-1","-r",WEBP_FRAMERATE,"-f","webp","-y",targetFile.getCanonicalPath())
                        .collect(Collectors.toList());
            }

            Long timeout = stringToLong(transformOptions.get(RequestParamMap.TIMEOUT));
            //this.run(StringUtils.join(comandAndArgs," "),sourceFile,targetFile,timeout);
            Map<String,String> options = new HashMap<>();
            options.put("options",StringUtils.join(comandAndArgs," "));
            this.run(options,targetFile,timeout);
        }catch (IOException e){
            logger.error(e.getMessage(),e);
        }
    }


    @Override
    public String getTransformerName() {
        return ID;
    }


    @Override
    protected RuntimeExec createCheckCommand() {
        return Commands.getFFMPegRuntimeExec();
    }

}
