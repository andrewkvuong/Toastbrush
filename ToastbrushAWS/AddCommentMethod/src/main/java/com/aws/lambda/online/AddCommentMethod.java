package com.aws.lambda.online;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
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

public class AddCommentMethod implements RequestHandler<RequestDetails, ResponseDetails> {
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
		boolean ret = checkFile(arg0.File);
		if(ret)
		{
			rd.Success = "file";
			return rd;
		}
		// Check Username
		ret = checkName(arg0.User);
		if(ret)
		{
			rd.Success = "name";
			return rd;
		}	
		
		// Update database
		rd.Success = insertComment(arg0.User, arg0.File, arg0.Comment);
		return rd;
	}
	
	public static String insertComment(String user, String photo, String desc)
	{
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO Comments(Owner, File, Score, Comment, Date) VALUES(?, ?, 0, ?, ?)");
			stmt.setString(1, user);
			stmt.setString(2, photo);
			stmt.setString(3, desc);
			stmt.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			return "false";
		}
		return "true";
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
	
	public static boolean checkFile(String file)
	{
		boolean retVal = true;
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Images WHERE Image=?");
			stmt.setString(1, file);
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
}
