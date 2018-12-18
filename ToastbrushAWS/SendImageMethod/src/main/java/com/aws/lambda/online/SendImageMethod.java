package com.aws.lambda.online;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;

import org.apache.commons.io.IOUtils;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.S3Object;
import com.aws.lambda.online.data.*;

public class SendImageMethod implements RequestHandler<RequestDetails, ResponseDetails> {
	public static Connection getConnection() throws Exception {
		try {
			String driver = "com.mysql.jdbc.Driver";
			String url = "jdbc:mysql://toastbrush.cmte8s92qvfc.us-west-1.rds.amazonaws.com:3306/toastbrush";
			String username = "andrewkvuong";
			String password = "toastbrush";
			Class.forName(driver);
			System.out.println("Attempting connection");
			Connection conn = DriverManager.getConnection(url, username, password);
			System.out.println("Connected");
			return conn;
		} catch (Exception e) {
			System.out.println(e);
		}
		return null;
	}
	
	public ResponseDetails handleRequest(RequestDetails arg0, Context arg1) {
		ResponseDetails rd = new ResponseDetails();
		if(!checkName(arg0.Name))
		{
			rd.success = "false";
			return rd;
		}
		String fileName = UUID.randomUUID().toString(); 
		insertImageDB(arg0.Name, fileName, arg0.User, arg0.Description);

		rd.success = uploadImageS3(arg0.Encoded, fileName);
		return rd;
	}
	
	public static boolean checkName(String name)
	{
		boolean retVal = true;
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Images WHERE Name=?");
			stmt.setString(1, name);
			ResultSet rs = stmt.executeQuery();
			if(rs.next())
			{
				retVal = false;
			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}
	
	
	public static String uploadImageS3(String encoded, String key_name) {
		Regions clientRegion = Regions.US_WEST_1;
		String bucket_name = "toastbrush";
		byte[] decodedBytes = encoded.getBytes();	
		File file = null;
		try {
			file = File.createTempFile("temp", null, null);
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(decodedBytes);
			fos.close();
		} catch (IOException e1) {
			e1.printStackTrace();
			return "false";
		}

		BasicAWSCredentials cred = new BasicAWSCredentials("AKIAJ5QZHCTHQ2J64WEA", "JueUUidavxiD/P66cdwTRADDc3PERIA97Xuh5BMw");
		AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(cred);
		AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).withRegion(clientRegion).build();

		try {
		    s3.putObject(bucket_name, key_name, file);
		    System.out.println("Upload Complete");
		    return "true";
		} catch (AmazonServiceException e) {
		    System.err.println(e.getErrorMessage());
		    return "false";
		}
	}
	
	public static void insertImageDB(String name, String file, String user, String desc)
	{
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO Images (Name, Image, Score, User, Description, Date) VALUES (?, ?, 0, ?, ?,?)");
			stmt.setString(1, name);
			stmt.setString(2, file);
			stmt.setString(3, user);
			stmt.setString(4, desc);
			stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
