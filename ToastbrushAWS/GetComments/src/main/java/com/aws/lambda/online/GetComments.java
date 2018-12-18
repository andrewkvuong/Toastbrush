package com.aws.lambda.online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.aws.lambda.online.data.*;

public class GetComments implements RequestHandler<RequestDetails, ResponseDetails> {
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
		return select(arg0);
	}
	
	public static ResponseDetails select(RequestDetails in)
	{
		ResponseDetails rd = new ResponseDetails();
		rd.list = new ArrayList<Comment>();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = null;
			switch(in.Order)
			{
			case 1:
				stmt = conn.prepareStatement("SELECT * FROM Comments WHERE File=? ORDER BY Date ASC LIMIT ? OFFSET ?");
				break;
			case 2:
				stmt = conn.prepareStatement("SELECT * FROM Comments  WHERE File=? ORDER BY Score DESC LIMIT ? OFFSET ?");
				break;
			case 3:
				stmt = conn.prepareStatement("SELECT * FROM Comments  WHERE File=? ORDER BY Score ASC LIMIT ? OFFSET ?");
				break;
			default:
				stmt = conn.prepareStatement("SELECT * FROM Comments  WHERE File=? ORDER BY Date DESC LIMIT ? OFFSET ?");
				break;
			}
			stmt.setString(1, in.File);
			stmt.setInt(2, in.Limit);
			stmt.setInt(3, in.Offset);
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				Comment c = new Comment();
			    c.Owner = rs.getString(1);
				c.ID = rs.getInt(2);
				c.File = rs.getString(3);
				c.Score = rs.getInt(4);
				c.Comment = rs.getString(5);
				c.Date = rs.getTimestamp(6);
				rd.list.add(c);
			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rd;
	}
}
