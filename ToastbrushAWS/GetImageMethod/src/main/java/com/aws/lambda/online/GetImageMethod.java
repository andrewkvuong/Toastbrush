package com.aws.lambda.online;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Base64;

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

public class GetImageMethod implements RequestHandler<RequestDetails, ResponseDetails> {
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
		return downloadObject(arg0.Name);
	}
	
	public static ResponseDetails downloadObject(String key_name) {
		Regions clientRegion = Regions.US_WEST_1;
		String bucket_name = "toastbrush";

        System.out.format("Downloading %s from S3 bucket %s...\n", key_name, bucket_name);
        BasicAWSCredentials cred = new BasicAWSCredentials("AKIAJ5QZHCTHQ2J64WEA", "JueUUidavxiD/P66cdwTRADDc3PERIA97Xuh5BMw");
		AWSStaticCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(cred);
		final AmazonS3 s3 = AmazonS3ClientBuilder.standard().withCredentials(credentialsProvider).withRegion(clientRegion).build();
        try {
            S3Object o = s3.getObject(bucket_name, key_name);
            InputStream s3is = o.getObjectContent();
            byte[] bytes = IOUtils.toByteArray(s3is);
            String encoded = new String(bytes);
            ResponseDetails rd = new ResponseDetails();
            rd.Encoded = encoded;
            s3is.close();
            return rd;
        } catch (AmazonServiceException e) {
            System.err.println(e.getErrorMessage());
            System.exit(1);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        System.out.println("Done!");
        return null;
	}
}
