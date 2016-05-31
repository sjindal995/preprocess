package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccDetailsFromLatLng extends Wrapper{
	
	
	public static void main(String[] args){
		
		// store the details for each account in file as well as for each poi in map
		accountDetailsFromLatLng();
		
		File d_file = new File(comp(ll_out_file));
		d_file.delete();
		
		File r_file = new File(ll_out_file);
		File n_file = new File("ll_acc_file");
		r_file.renameTo(n_file);
		
		//display final file name in which result is stored
		System.out.println("completed : " + ll_out_file);
		
	}
	
	public static void accountDetailsFromLatLng(){
		try{
			
			// file initializations
			initializeFile(ll_out_file);
			initializeFile(ll_out_file + "_2");
			
			int it=1;
			
			BufferedReader br = new BufferedReader(new FileReader(poi_in_file));
			String order_id, acc_id, lat, lng;
			List<String> items = new ArrayList<String>();
			for(String line; ((line = br.readLine()) != null) && (it < 200); ) {
	    		// split the line to list of strings
		    	items = Arrays.asList(line.split("\\s*,\\s*"));
		    	System.out.println(String.valueOf(it) + " :: " + items);
		    	
		    	order_id = items.get(0);
		    	acc_id = items.get(1);
		    	lat = items.get(2);
		    	lng = items.get(3);
		    	
		    	String url = flip_url + "/nearby_map_data?point=";
				url += lat + "," + lng + "&max_dist=0.05";
				//no need to encode this url as no special chars possible
				
				//call flip api for nearby_map_data
				JSONObject poi_result_obj = getApiResult(url, 0);
				Thread.sleep(500);
				
				// format of nearby_map_data response: {"nearby_map_data":{...}}
				JSONObject poi_result_data = (JSONObject) poi_result_obj.get("nearby_map_data");
				
				// initialize jsonarray to store retrieved poi points
				JSONArray poi_points = new JSONArray();
				
				// jsonarray to store pincodes recieved
				JSONArray pincode = new JSONArray();
				
				//jsonarray to receive state regions received
				JSONArray state = new JSONArray();
				// try to store the poi points present in the result data
				// it is possible that no poi points are present at the provided lat/lng values
				// in that case this will give exception for jsonObject["Poi_point"] not found
				try{
					poi_points = (JSONArray) poi_result_data.get("Poi_point");
					pincode = (JSONArray) poi_result_data.get("Pincode_region");
					state = (JSONArray) poi_result_data.get("State_region");
					
				} catch (Exception e){
					e.printStackTrace();
				}
				
				//object to store details of current order
				JSONObject details = new JSONObject();						
				// store pois in the details jsonobject storing address_component, location, pincode
				details.put("pois", poi_points);
				// store pincode
				details.put("pincode", pincode);
				// store state
				details.put("state", state);
				
				// object as order made by an account to be stored in orders array corresponding to particular account
				JSONObject entry = new JSONObject();
				entry.put("orderId", order_id);
				entry.put("details", details);
				
				// open file to read latest data and another file to write the updated data
				FileReader fr = new FileReader(ll_out_file);
	            BufferedReader br1 = new BufferedReader(fr);
	            FileWriter fw = new FileWriter(comp(ll_out_file));
	            BufferedWriter bw = new BufferedWriter(fw);
	            
	            // boolean variable to store if accID currently processed is present in the file
	            Boolean acc_found = false;
	            
	            String line1;
	            // first line is empty or null
	            br1.readLine();
	            
	            while ((line1 = br1.readLine()) != null) {
	            	
	            	// convert the line string to jsonobject
	            	JSONObject js_line = new JSONObject(line1);
	            	
	            	// if the accId in the current line is the accId being currently processed
	                if(js_line.getString("accId").equals(acc_id)){
	                	// add the order entry to the array storing all orders corresponding to this accId
	                	js_line.getJSONArray("orders").put(entry);
	                	
	                	// write the updated jsonobject to the new file (complement file)
	                	bw.write("\r\n" + js_line.toString());
	                	bw.flush();
	                	
	                	// mark that this accId has been found in the file
	                	acc_found = true;
	                }
	                else{
	                	// else just copy the line from one file to its complement without any change 
	                	bw.write("\r\n"+line1);
	                	bw.flush();
	                }
	            }
	            
	            // if the account is encountered for the first time
	            if(!acc_found){
	            	// create jsonobject to store the accId and corresponding orders made by this account
	            	JSONObject js_line = new JSONObject();
	            	js_line.put("accId", acc_id);
	            	JSONArray orders = new JSONArray();
	            	
	            	// add the current order processed to the order array for this accId
	            	orders.put(entry);
	            	js_line.put("orders", orders);
	            	
	            	// write the object for this account to the file in a new line
	            	bw.write("\r\n" + js_line.toString());
	            	bw.flush();
	            	br1.close();
	            	bw.close();
	            }
	            
	            //change the latest updated file to its complement
	            ll_out_file = comp(ll_out_file);
		    }
	    	br.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
