package com.aws.lambda.online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.aws.lambda.online.data.*;

public class DeleteImage implements RequestHandler<RequestDetails, ResponseDetails> {
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
		// TODO Auto-generated method stub
		
		ResponseDetails r = new ResponseDetails();
		r.success = removeImage(arg0.Image);
		return r;
	}
	
	public String removeImage(String image)
	{
		ResponseDetails rd = new ResponseDetails();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("DELETE FROM Images WHERE Image=?");
			stmt.setString(1,image);
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return "false";
		}
		return "true";
	}
}
