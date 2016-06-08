package preprocess;

import java.net.*;
import java.io.*;

//import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

public class Wrapper{

	// input files
	public static String addr_in_file = "000000_0_addr";
//	public static String addr_in_file = "sample.txt";
	public static String ll_in_file = "000000_0_ll";
	
	public static int total_addr = 0;
	public static int poi_addr = 0;
	
	// flip api url host and port number
	public static String flip_url = "http://fms-sage-app.vip.nm.flipkart.com:8888";
	
	// latest updated output file for poi 2 account mapping 
	public static String poi_file = "poi2acc";
	

	// latest updated output file for details of each account
	public static String addr_ll_file = "addr_ll_details";
	
	public static String addr_out_file = "addr_details";
	public static String ping_file = "ping_details";
	
	public static String filtered_addr_file = "filtered_addr_file";
	public static String filtered_ping_file = "filtered_ping_file";
	
	public static String state_addr_out_file = "filtered_addr_states";
	
	public static String state_ping_out_file = "filtered_ping_states";
	
	public static JSONObject getApiResult(String url, int it){
		try{
			// convert given string to url datatype
			URL target = new URL(url);

			// open a new connection to the target url
			HttpURLConnection conn = (HttpURLConnection) target.openConnection();
			
			// request in a GET api call
			conn.setRequestMethod("GET");
			
			// set connection and read timeouts
			conn.setConnectTimeout(60000);
			conn.setReadTimeout(60000);
			
		    // jsonobject to be returned
	        JSONObject json_result = null;
			
	        BufferedReader reader = null;
	        
	        InputStream response = null;
			try{
				// connect via the open connection
				conn.connect();
				
				// receive response as an inputstream
			    response = conn.getInputStream();
			    
			    // convert entire response into a string 
			    reader = new BufferedReader(new InputStreamReader(response));
		        String line;
		        String result = "";
		        while ((line = reader.readLine()) != null) {
		        	result += line;
		        }
		        
		        json_result = new JSONObject(result);

		        // if buffered reader is opened succesfully, close it
		        if(reader != null){
		        	reader.close();
		        }
		        // if connection created succesfully, close it
		        if(conn != null){
		        	conn.disconnect();
		        }

	        } catch (SocketException | SocketTimeoutException | UnknownHostException e){
	        	e.printStackTrace();
	        	// if buffered reader is opened succesfully, close it
		        if(reader != null){
		        	reader.close();
		        }
		        // if connection created succesfully, close it
		        if(conn != null){
		        	conn.disconnect();
		        }
		        
		        if(e instanceof UnknownHostException){
		        	System.in.read();
		        }
		        
		        // if any network exception => retry at most 5 times
	        	if(it < 5){
	        		Thread.sleep(200);
	        		json_result = getApiResult(url, it+1);
	        		System.out.println(json_result);
	        	}
	        } catch (Exception e){
	        	e.printStackTrace();
	        }
	        
	        return json_result;
		}
		catch( Exception e){
			System.out.println("cause:" + e.getMessage());
			e.printStackTrace();
		}
		return null;
	}
	
	//clear the existing output files else create a new empty file
	public static void initializeFile(String file){
		try{
			File f1 = new File(file);
			if(!f1.exists()){
				f1.createNewFile();
			}else{
				PrintWriter writer = new PrintWriter(f1);
				writer.print("");
				writer.close();
			}
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	
	// obtain the name of other file - one is used for reading other is used for writing
	public static String comp(String cur_file){
		String out = "";
		if(cur_file.equals("addr_ll_details") || cur_file.equals("poi2acc") || cur_file.equals("ll_acc_details")){
			out = cur_file + "_2";
		}
		else{
			out = cur_file.substring(0, cur_file.length()-2);
		}
		return out;
	}
	 
		
}
