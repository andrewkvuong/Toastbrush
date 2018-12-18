package com.aws.lambda.online;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.aws.lambda.online.data.*;

public class VoteComment implements RequestHandler<RequestDetails, ResponseDetails> {
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
		int prev = getScore(arg0.ID, arg0.User);
		updateCommentScore(arg0.ID, arg0.User, arg0.Value);
		int diff = -1*(prev - arg0.Value);
		update(arg0.ID, diff);
		ResponseDetails r = new ResponseDetails();
		r.Success = "true";
		return r;
	}
	
	public static int getScore(int ID, String User)
	{
		int retVal = 0;
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("SELECT * FROM CommentScoring WHERE ID=? AND User=?");
			stmt.setInt(1, ID);
			stmt.setString(2, User);
			
			ResultSet rs = stmt.executeQuery();
			while(rs.next())
			{
				retVal = rs.getInt(3);
			}
			rs.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return retVal;
	}
	
	public static ResponseDetails updateCommentScore(int id, String user, int val)
	{
		ResponseDetails rd = new ResponseDetails();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("INSERT INTO CommentScoring VALUES(?, ?, ?) ON DUPLICATE KEY UPDATE Value=VALUES(Value)");
			stmt.setString(1,user);
			stmt.setInt(2, id);
			stmt.setInt(3, val);
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rd;
	}
	
	public static ResponseDetails update(int id, int diff)
	{
		ResponseDetails rd = new ResponseDetails();
		try {
			Connection conn = getConnection();
			PreparedStatement stmt = conn.prepareStatement("UPDATE Comments SET Score=Score + ? WHERE ID=?");
			stmt.setInt(1, diff);
			stmt.setInt(2, id);
			stmt.executeUpdate();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return rd;
	}
}
