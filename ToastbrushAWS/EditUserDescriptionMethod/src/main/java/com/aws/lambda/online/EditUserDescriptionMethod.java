package com.aws.lambda.online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.aws.lambda.online.data.*;

public class EditUserDescriptionMethod implements RequestHandler<RequestDetails, ResponseDetails> {
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
		ResponseDetails r = update(arg0.User, arg0.Description);
		return r;
	}
	
	public static ResponseDetails update(String user, String desc)
	{
		ResponseDetails rd = new ResponseDetails();
		rd.Success = "true";
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("UPDATE Users SET Description=? WHERE User=?");
			stmt.setString(1, desc);
			stmt.setString(2, user);
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
			rd.Success = "false";
		}
		return rd;
	}
}
