import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;
import javax.net.ssl.SSLHandshakeException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

public class AmazonS3BucketFileUpload {

	private static final String SUFFIX = "/";

	private static File file;
	
	private static String regionname;
	
	private static String bucketname;

	private static String accesskeyid;

	private static String secretaccesskey;
	
	private static final String TEMPDIR = "tempimages/";
	
	private static String notValidURL = "Invalid";
	
	
	public AmazonS3BucketFileUpload() {
		// TODO Auto-generated constructor stub
	}

	public static void main(String[] args) throws IOException {
		amazonS3BucketFileUploadOps();

	}

	public static void amazonS3BucketFileUploadOps()  {
		
		try {

			BasicAWSCredentials credentials = new BasicAWSCredentials(accesskeyid, secretaccesskey);
			
			AmazonS3 amazonS3Client = AmazonS3ClientBuilder.standard().enablePathStyleAccess().withRegion(regionname).withCredentials(new AWSStaticCredentialsProvider(credentials)).build();
			
			// Please add any Image URL
			String imageURL = "http://pareshvaniya.com/";
			
			// create bucket - name must be unique for all S3 users
			String bucketName = "paresh-test";

			// create folder into bucket
			String folderName = "XXXXX";
			
			createFolder(bucketName, folderName, amazonS3Client);

			String fileName = "images.jpeg";
			
			String uploaded_image_URL = uploadImageInAmazonS3Bucket(folderName,amazonS3Client,fileName,imageURL);
			
			System.out.println("Amazon s3 bukcet image uploaded url : "+uploaded_image_URL);
			
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {

			System.err.println(file.getAbsolutePath());
			Path fileToDeletePath = Paths.get(file.getAbsolutePath());
			try {
				Files.delete(fileToDeletePath);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	
	private static String uploadImageInAmazonS3Bucket(String folderName, AmazonS3 amazonS3Client, String fileName,
			String imageURL) throws IOException {
		String imageURL_New = StringUtils.EMPTY;
		InputStream input = null;
		ByteArrayInputStream byteArrayInputStream = null;
		File tempLocalfile = null;
		boolean validURL = false;
		try {
			// upload file to folder
			String uploadfileName = folderName + SUFFIX + fileName;

			tempLocalfile = new File(TEMPDIR + fileName);

			validURL = checkIfImageExists(imageURL);
			
			if(validURL)
			{	
				FileUtils.copyURLToFile(new URL(imageURL), tempLocalfile);
	
				input = new FileInputStream(tempLocalfile);
	
				ObjectMetadata metadata = new ObjectMetadata();
	
				byte[] bytes = IOUtils.toByteArray(input);
	
				metadata.setContentLength(bytes.length);
				metadata.setContentType("image/jpeg");
	
				//metadata.addUserMetadata("Metadata field");
	
				byteArrayInputStream = new ByteArrayInputStream(bytes);
	
				amazonS3Client.putObject(new PutObjectRequest(bucketname, uploadfileName, byteArrayInputStream, metadata)
						.withCannedAcl(CannedAccessControlList.PublicRead));
				imageURL_New = amazonS3Client.getUrl(bucketname, uploadfileName).toString();
	
				System.out.println("Image File - " + fileName
						+ " successfully uploaded in Amazon S3 Bucket ");
			}else{
				System.out.println("ImageURL : "+ imageURL +" is incorrect");
				imageURL_New = notValidURL;
			}

		} catch (Exception ex) {
			System.out.println("Exception occured in uploadImageInAmazonS3Bucket"
					+ ex);
		} finally {
			if(validURL){
				input.close();
				byteArrayInputStream.close();
				// Removing Local Temp Images
				Path fileToDeletePath = Paths.get(tempLocalfile.getAbsolutePath());
				Files.delete(fileToDeletePath);
			}
		}

		return imageURL_New;
	}
	
	

	public static void createFolder(String bucketName, String folderName, AmazonS3 client) throws IOException {
		InputStream emptyContent = new ByteArrayInputStream(new byte[0]);
		try {

			boolean exists = client.doesObjectExist(bucketName, folderName + SUFFIX);

			if (!exists) {

				// create meta-data for your folder and set content-length to 0
				ObjectMetadata metadata = new ObjectMetadata();
				metadata.setContentLength(0);
				
				//metadata.addUserMetadata("");

				// create a PutObjectRequest passing the folder name suffixed by
				PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, folderName + SUFFIX, emptyContent,
						metadata);

				client.putObject(putObjectRequest);

				System.out.println("Folder Name - " + folderName + " successfully created in Amazon S3 Bucket");

			}
		} catch (Exception e) {
			//exception handling
		} finally {
			emptyContent.close();
		}
	}

	public static boolean checkIfImageExists(String targetUrl) {
		try {
			BufferedImage image = ImageIO.read(new URL(targetUrl));
		
			if (image != null) {
				return true;
			} else {
				return false;
			}

		} catch (Exception e) {

        	if (ExceptionUtils.indexOfThrowable(e, SSLHandshakeException.class) != -1) {
        	    return true;
        	}
        	return false;
		}
	}

	

		
}
