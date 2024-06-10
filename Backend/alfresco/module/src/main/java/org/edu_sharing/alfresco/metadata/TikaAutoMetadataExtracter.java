package org.edu_sharing.alfresco.metadata;

//import org.alfresco.repo.content.MimetypeMap;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
//import org.apache.tika.config.TikaConfig;
//import org.apache.tika.metadata.Metadata;
//import org.apache.tika.metadata.TIFF;
//import org.apache.tika.mime.MediaType;
//import org.apache.tika.parser.AutoDetectParser;
//import org.apache.tika.parser.Parser;
//import org.edu_sharing.alfresco.apache.tika.parser.mp4.MP4Parser;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.Serializable;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.Map;


public class TikaAutoMetadataExtracter //extends org.alfresco.transformer.metadataExtractors.AbstractTikaMetadataExtractor
{

//    protected static Logger logger = LoggerFactory.getLogger(TikaAutoMetadataExtracter.class);
//    private static AutoDetectParser parser;
//    private static TikaConfig config;
//    private static String EXIF_IMAGE_HEIGHT_TAG = "Exif Image Height";
//    private static String EXIF_IMAGE_WIDTH_TAG = "Exif Image Width";
//    private static String JPEG_IMAGE_HEIGHT_TAG = "Image Height";
//    private static String JPEG_IMAGE_WIDTH_TAG = "Image Width";
//
//    public static ArrayList<String> SUPPORTED_MIMETYPES;
//    private static ArrayList<String> buildMimeTypes(TikaConfig tikaConfig)
//    {
//        config = tikaConfig;
//        parser = new AutoDetectParser(config);
//
//        /**
//         * edu-sharing fix
//         */
//        MP4Parser mp4Parser = new MP4Parser();
//        Map<MediaType,Parser> newParsers = new HashMap<>();
//        //overwrite the default MP4Parser
//        for(Map.Entry<MediaType,Parser> entry : parser.getParsers().entrySet()){
//            if(entry.getValue().getClass().equals(org.apache.tika.parser.mp4.MP4Parser.class)){
//                newParsers.put(entry.getKey(),mp4Parser);
//            }else{
//                newParsers.put(entry.getKey(),entry.getValue());
//            }
//        }
//        parser.setParsers(newParsers);
//        /**
//         * finished edu-sharing fix
//         */
//
//
//        SUPPORTED_MIMETYPES = new ArrayList<>();
//        for(MediaType mt : parser.getParsers().keySet())
//        {
//            // Add the canonical mime type
//            SUPPORTED_MIMETYPES.add( mt.toString() );
//
//            // And add any aliases of the mime type too - Alfresco uses some
//            //  non canonical forms of various mimetypes, so we need all of them
//            for(MediaType alias : config.getMediaTypeRegistry().getAliases(mt))
//            {
//                SUPPORTED_MIMETYPES.add( alias.toString() );
//            }
//        }
//        return SUPPORTED_MIMETYPES;
//    }
//
//    public TikaAutoMetadataExtracter()
//    {
//        super( logger );
//    }
//
//    /**
//     * Does auto-detection to select the best Tika
//     *  Parser.
//     */
//    @Override
//    protected Parser getParser()
//    {
//        return parser;
//    }
//
//    /**
//     * Because some editors use JPEG_IMAGE_HEIGHT_TAG when
//     * saving JPEG images , a more reliable source for
//     * image size are the values provided by Tika
//     * and not the exif/tiff metadata read from the file
//     * This will override the tiff:Image size
//     * which gets embedded into the alfresco node properties
//     * for jpeg files that contain such exif information
//     */
//    @Override
//    protected Map<String, Serializable> extractSpecific(Metadata metadata,
//                                                        Map<String, Serializable> properties, Map<String,String> headers)
//    {
//
//        if(MimetypeMap.MIMETYPE_IMAGE_JPEG.equals(metadata.get(Metadata.CONTENT_TYPE)))
//        {
//            //check if the image has exif information
//            if(metadata.get(EXIF_IMAGE_WIDTH_TAG) != null && metadata.get(EXIF_IMAGE_HEIGHT_TAG) != null )
//            {
//                //replace the exif size properties that will be embedded in the node with
//                //the guessed dimensions from Tika
//                putRawValue(TIFF.IMAGE_LENGTH.getName(), extractSize(metadata.get(JPEG_IMAGE_HEIGHT_TAG)), properties);
//                putRawValue(TIFF.IMAGE_WIDTH.getName(), extractSize(metadata.get(JPEG_IMAGE_WIDTH_TAG)), properties);
//            }
//        }
//        return properties;
//    }
//
//    /**
//     * Exif metadata for size also returns the string "pixels"
//     * after the number value , this function will
//     * stop at the first non digit character found in the text
//     * @param sizeText string text
//     * @return the size value
//     */
//    private String extractSize(String sizeText)
//    {
//        StringBuilder sizeValue = new StringBuilder();
//        for(char c : sizeText.toCharArray())
//        {
//            if(Character.isDigit(c))
//            {
//                sizeValue.append(c);
//            }
//            else
//            {
//                break;
//            }
//        }
//        return sizeValue.toString();
//    }

}

