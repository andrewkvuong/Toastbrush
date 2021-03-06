package com.aws.lambda.online;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.UUID;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.aws.lambda.online.data.*;

public class EditUserImageMethod implements RequestHandler<RequestDetails, ResponseDetails> {
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
		// Check Email
		ResponseDetails rd = new ResponseDetails();
		rd.Success = "false";
		// Check Username
		boolean ret = checkName(arg0.User);
		if(ret)
		{
			rd.Success = "name";
			return rd;
		}	
		rd.Success = "true";
		String fileName = UUID.randomUUID().toString(); 
		
		// Update Database
		update(arg0.User, fileName);
		
		// Upload Image
		uploadImageS3(arg0.Encoded, fileName);
		return rd;
	}
	
	public static boolean checkName(String name)
	{
		boolean retVal = true;
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Users WHERE User=?");
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
	
	public static ResponseDetails update(String user, String desc)
	{
		ResponseDetails rd = new ResponseDetails();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("UPDATE Users SET Photo=? WHERE User=?");
			stmt.setString(1, desc);
			stmt.setString(2, user);
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rd;
	}
	
	public static void uploadImageS3(String encoded, String key_name) {
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
		}

		BasicAWSCredentials cred = new BasicAWSCredentials("AKIAJ5QZHCTHQ2J64WEA", "JueUUidavxiD/P66cdwTRADDc3PERIA97Xuh5BMw");
		AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(cred);
		AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).withRegion(clientRegion).build();

		try {
		    s3.putObject(bucket_name, key_name, file);
		    System.out.println("Upload Complete");
		} catch (AmazonServiceException e) {
		    System.err.println(e.getErrorMessage());
		}
	}
}
