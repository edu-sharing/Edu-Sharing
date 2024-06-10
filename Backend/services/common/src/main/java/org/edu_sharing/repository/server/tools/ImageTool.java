package org.edu_sharing.repository.server.tools;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.imageio.ImageIO;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.commons.io.IOUtils;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import org.apache.log4j.Logger;
import org.apache.tika.config.TikaConfig;
import org.apache.tika.detect.Detector;
import org.apache.tika.io.TikaInputStream;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.mime.MediaType;
import org.apache.tika.mime.MimeTypeException;
import org.edu_sharing.alfresco.policy.NodeMimetypeValidationException;

import static org.apache.batik.transcoder.SVGAbstractTranscoder.*;

/** 
 * 
 * @author Torsten
 * Tool for common image tasks like rotating by exif orientation
 */
public class ImageTool {
	public static final int MAX_THUMB_SIZE = 900;

	private static int readImageOrientation(InputStream imageFile)  throws IOException, MetadataException, ImageProcessingException {
        com.drew.metadata.Metadata metadata = ImageMetadataReader.readMetadata(new BufferedInputStream(imageFile));
	    Directory directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
	    int orientation = 1;
	    try {
	        orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
	    } catch (MetadataException me) {
	    }
	    return orientation;
	}
	// Look at http://chunter.tistory.com/143 for information
	private static AffineTransform getExifTransformation(int orientation,int width,int height) {

	    AffineTransform t = new AffineTransform();

	    switch (orientation) {
	    case 1:
	        break;
	    case 2: // Flip X
	        t.scale(-1.0, 1.0);
	        t.translate(-width, 0);
	        break;
	    case 3: // PI rotation 
	        t.translate(width, height);
	        t.rotate(Math.PI);
	        break;
	    case 4: // Flip Y
	        t.scale(1.0, -1.0);
	        t.translate(0, -height);
	        break;
	    case 5: // - PI/2 and Flip X
	        t.rotate(-Math.PI / 2);
	        t.scale(-1.0, 1.0);
	        break;
	    case 6: // -PI/2 and -width
	        t.translate(height, 0);
	        t.rotate(Math.PI / 2);
	        break;
	    case 7: // PI/2 and Flip
	        t.scale(-1.0, 1.0);
	        t.translate(-height, 0);
	        t.translate(0, width);
	        t.rotate(  3 * Math.PI / 2);
	        break;
	    case 8: // PI / 2
	        t.translate(0, width);
	        t.rotate(  3 * Math.PI / 2);
	        break;
	    }

	    return t;
	}
	private static BufferedImage transformImage(BufferedImage image,int orientation) throws Exception {
		int width=image.getWidth();
		int height=image.getHeight();
	    AffineTransformOp op = new AffineTransformOp(getExifTransformation(orientation, width, height), AffineTransformOp.TYPE_BICUBIC);

	    BufferedImage destinationImage = op.createCompatibleDestImage(image, (image.getType() == BufferedImage.TYPE_BYTE_GRAY) ? image.getColorModel() : null );
	    Graphics2D g = destinationImage.createGraphics();
	    g.setBackground(Color.WHITE);
	    g.clearRect(0, 0, destinationImage.getWidth(), destinationImage.getHeight());
	    destinationImage = op.filter(image, destinationImage);
	    return destinationImage;
	}
	/**
	 * 
	 * @param is Input stream containing image data
	 * @param maxSize max size of longest side, or 0 for original size
	 * @return
	 * @throws IOException 
	 */
	public static InputStream autoRotateImage(InputStream is,int maxSize) throws IOException{
		byte[] data=is.readAllBytes();
		try{
			BufferedImage image=ImageIO.read(new ByteArrayInputStream(data));
			ByteArrayOutputStream os=new ByteArrayOutputStream();
			if(maxSize>0)
				image=scaleImage(image,maxSize);
			try{
				int orientation=readImageOrientation(new ByteArrayInputStream(data));
				image=transformImage(image, orientation);
			}catch (Throwable t) {
				// no exif information, no rotation
			}
			
			ImageIO.write(image, "PNG", os);
			return new ByteArrayInputStream(os.toByteArray());
		}
		catch(Throwable t){
			return new ByteArrayInputStream(data);
		}
		
	}
	private static BufferedImage scaleImage(BufferedImage image, int maxSize) {
		double aspect=(double)image.getWidth()/image.getHeight();
		int width=(int) (maxSize*aspect);
		int height=maxSize;
		
		if(aspect>1){
			width=maxSize;
			height=(int) (maxSize/aspect);
			if(image.getWidth()<width)
				return image;
		}
		else{
			if(image.getHeight()<height)
				return image;
		}
		
		Image scaled = image.getScaledInstance(width, height, BufferedImage.SCALE_AREA_AVERAGING);
		BufferedImage buffered = new BufferedImage(width,height, image.getType());
		buffered.getGraphics().drawImage(scaled, 0, 0 , null);
		return buffered;
	}

	/**
	 * checks if given input stream is an image mimetype and throws an exception otherwise
	 * Will also auto convert svg to remove malicious data and auto rotate jpgs based on exif data
	 */
	public static VerifyResult verifyAndPreprocessImage(InputStream is, int maxSize) throws MimeTypeException, IOException {
		byte[] data=IOUtils.toByteArray(is);
		TikaConfig config = TikaConfig.getDefaultConfig();
		Detector detector = config.getDetector();
		TikaInputStream stream = TikaInputStream.get(data);
		Metadata metadata = new Metadata();
		MediaType mediaType = detector.detect(stream, metadata);
		if(!mediaType.getType().equals("image") && !mediaType.getType().equals("text")) {
			throw new NodeMimetypeValidationException("Invalid mime type for image: " + mediaType.getType() + "/" + mediaType.getSubtype());
		}
		if(mediaType.getType().equals("text") || mediaType.equals(MediaType.image("svg+xml"))) {
			try {
				data = convertSvgToPng(new ByteArrayInputStream(data));
				mediaType = MediaType.image("png");
			}catch(Throwable t) {
				Logger.getLogger(ImageTool.class).info("Svg parse error", t);
				throw new NodeMimetypeValidationException("Given file was no valid svg or could not be converted");
			}
		}
		InputStream result = autoRotateImage(new ByteArrayInputStream(data), ImageTool.MAX_THUMB_SIZE);
		return new VerifyResult(result, mediaType);
	}


	public static byte[] convertSvgToPng(InputStream data) throws Exception {
		TranscoderInput input = new TranscoderInput(data);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		TranscoderOutput output = new TranscoderOutput(bos);
		PNGTranscoder transcoder = new PNGTranscoder();
		transcoder.addTranscodingHint(KEY_WIDTH, 1024f);
		// calculated automatically
		// transcoder.addTranscodingHint(KEY_HEIGHT, 1024f);
		transcoder.addTranscodingHint(KEY_MAX_HEIGHT, 1024f);
		transcoder.addTranscodingHint(KEY_ALLOWED_SCRIPT_TYPES, "");
		transcoder.addTranscodingHint(KEY_ALLOW_EXTERNAL_RESOURCES, false);
		transcoder.transcode(input, output);
		return bos.toByteArray();
	}


	@Data
	@AllArgsConstructor
	public static class VerifyResult {
		InputStream inputStream;
		MediaType mediaType;
	}
}