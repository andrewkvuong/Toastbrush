package com.aws.lambda.online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.aws.lambda.online.data.*;

public class GetImagesByKeyword implements RequestHandler<RequestDetails, ResponseDetails> {
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
		ResponseDetails rd = select(arg0);
		for(Image i : rd.list)
		{
			checkCurrentScore(i, arg0.CurrentUser);
		}
		return rd;
	}
	
	public static void checkCurrentScore(Image i, String user)
	{
		i.UserScore = 0;
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = null;
			stmt = conn.prepareStatement("SELECT * FROM ImageScoring where User=? AND Image=?");
			stmt.setString(1, user);
			stmt.setString(2, i.Image);
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				i.UserScore = rs.getInt(3);
			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	public static ResponseDetails select(RequestDetails in)
	{
		ResponseDetails rd = new ResponseDetails();
		rd.list = new ArrayList<Image>();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = null;
			switch(in.Order)
			{
			case 1:
				stmt = conn.prepareStatement("SELECT * FROM Images WHERE Name LIKE ? ORDER BY Date ASC LIMIT ? OFFSET ?");
				break;
			case 2:
				stmt = conn.prepareStatement("SELECT * FROM Images WHERE Name LIKE ? ORDER BY Score DESC LIMIT ? OFFSET ?");
				break;
			case 3:
				stmt = conn.prepareStatement("SELECT * FROM Images WHERE Name LIKE ? ORDER BY Score ASC LIMIT ? OFFSET ?");
				break;
			default:
				stmt = conn.prepareStatement("SELECT * FROM Images WHERE Name LIKE ? ORDER BY Date DESC LIMIT ? OFFSET ?");
				break;
			}
			stmt.setString(1, "%"+in.Keyword+"%");
			stmt.setInt(2, in.Limit);
			stmt.setInt(3, in.Offset);
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				Image c = new Image();
			    c.Name = rs.getString(1);
				c.Image = rs.getString(2);
				c.Score = rs.getInt(3);
				c.User = rs.getString(4);
				c.Description = rs.getString(5);
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
