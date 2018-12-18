package com.aws.lambda.online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Date;

import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.aws.lambda.online.data.*;

public class GetImageInfoMethod implements RequestHandler<RequestDetails, ResponseDetails> {
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
		return select(arg0.File);
	}
	
	public static ResponseDetails select(String fileName)
	{
		ResponseDetails rd = new ResponseDetails();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Images WHERE Image=?");
			stmt.setString(1, fileName);
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
			    rd.Name = rs.getString(1);
				rd.File = rs.getString(2);
				rd.Score = rs.getInt(3);
				rd.User = rs.getString(4);
				rd.Description = rs.getString(5);
				rd.Date = rs.getTimestamp(6);
			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rd;
	}
}
