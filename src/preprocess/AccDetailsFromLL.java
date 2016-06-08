package preprocess;

import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccDetailsFromLL extends LLDetailsFromAddr{
	
	public static void accountDetailsFromAddr(){
		try{
			
			// Read the lat long details file
			BufferedReader br_addr = new BufferedReader(new FileReader(addr_ll_file));
			
			// Empty first line
			br_addr.readLine();
			
			// Output file containing poi details for the accounts 
			BufferedWriter bw_out = new BufferedWriter(new FileWriter(addr_out_file));
			
			int it=1;
			
			for(String addr_line; ((addr_line = br_addr.readLine()) != null);){
				
				// Convert string line to jsonobject
				JSONObject acc_details_geocode = new JSONObject(addr_line);
				
				// Account ID of the current account processed
				String accId = acc_details_geocode.getString("accId");
				
				// Orders made by the current account
				JSONArray orders = acc_details_geocode.getJSONArray("orders");
				
				// JSONArray to store orders with pois included
				JSONArray updated_orders = new JSONArray();
				
				System.out.println(it + " <<<< " + accId);
				
				for(int order_it = 0; order_it < orders.length(); order_it++){
					// Retrieve an order
					JSONObject order = orders.getJSONObject(order_it);
					
					// Variable to check if there exists a poi corresponding to this address
					Boolean poi_found = false;
					
					// Get details of the current order
					JSONArray details_arr = order.getJSONArray("details");

					// Create new array to store details along with pois
					JSONArray updated_arr = new JSONArray();
					
					for(int details_it = 0; details_it < details_arr.length(); details_it++){

						// Retrieve one lat/long from the order details
						JSONObject details_item = details_arr.getJSONObject(details_it);
						JSONObject location = details_item.getJSONObject("location");
						Double lat = location.getDouble("lat");
						Double lng = location.getDouble("lng");
						
						// Get pois for the lat longs obtained
						JSONObject poi_result = getPois(Double.toString(lat), Double.toString(lng));
						JSONArray poi_points = poi_result.getJSONArray("pois");
						JSONObject state_region = poi_result.getJSONObject("state");
						JSONObject pincode = poi_result.getJSONObject("pincode");
						
						// If poi exists then mark poi_found as true
						if(poi_points.length() > 0) poi_found = true;
						
						// store pois in the details jsonobject storing address_component, location, pincode
						details_item.put("pois", poi_points);
						details_item.put("state", state_region);
						details_item.put("pincode", pincode);
						
						updated_arr.put(details_item);
					}
					
					// store updated array in order object under "details" key
					order.put("details", updated_arr);
					updated_orders.put(order);
					
					// if found the address with atleast 1 poi
					if(poi_found) poi_addr++;
				}

				// write current account details to output file
				JSONObject acc_details = new JSONObject();
				acc_details.put("accId", accId);
				acc_details.put("orders", updated_orders);
				bw_out.write("\r\n" + acc_details.toString());
				
				it++;
			}
			br_addr.close();
			bw_out.close();
			
			
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static void accountDetailsFromPings(){
		try{
			// Input file with format : accId,lat,lng
			BufferedReader br_in = new BufferedReader(new FileReader(ll_in_file));
			
			// AccId and details for that account in the previous iteration
			String prev_accId = "";
			JSONArray prev_acc_details = new JSONArray();
			
			// Output file to store state, pincode, pois for each account 
			BufferedWriter bw_ping = new BufferedWriter(new FileWriter(ping_file));
			
			int it = 0;
			
			for(String line; ((line = br_in.readLine()) != null) && (it < 10);){
				
				String accId, lat, lng;

				// Split the line to list of strings and retrieve different fields
				List<String> items = new ArrayList<String>();
		    	items = Arrays.asList(line.split("\\s*,\\s*"));
		    	
		    	accId = items.get(0);
		    	lat = items.get(1);
		    	lng = items.get(2);
		    	
		    	// If current account is not same as previous account
		    	if(!accId.equals(prev_accId)){
		    		// If previous account is not empty
		    		if(!prev_accId.equals("")){
		    			
		    			//Write the previous account details to the output file
		    			JSONObject acc_entry = new JSONObject();
			    		acc_entry.put("accId", prev_accId);
			    		acc_entry.put("acc_details", prev_acc_details);
		    			bw_ping.write("\r\n" + acc_entry.toString());
		    			bw_ping.flush();
		    		}
		    		
		    		// Clear the previous JSONArray
		    		int start = prev_acc_details.length();
		    		for(int index = start - 1; index >= 0; index--){
		    			prev_acc_details.remove(index);
		    		}
		    		
		    		// Change the previous account id variable
		    		prev_accId = accId;
		    		
		    		it++;
		    	}
		    	
		    	// If invalid lat/long ignore
		    	if(Double.parseDouble(lat) == 0 && Double.parseDouble(lng) == 0){
		    		continue;
		    	}
		    	
		    	System.out.println("it: " + Integer.toString(it) + " , lat: " + lat + " , lng: " + lng);
		    	
		    	// Retrieve PoIs, state and pincode for the lat/long
		    	JSONObject poi_result = getPois(lat,lng);
		    	
				JSONArray poi_points = poi_result.getJSONArray("pois");
				JSONObject pincode = poi_result.getJSONObject("pincode");
				JSONObject state = poi_result.getJSONObject("state");
				
				
				// Object to store details of current line
				JSONObject details = new JSONObject();
				details.put("pois", poi_points);
				details.put("pincode", pincode);
				details.put("state", state);
				
				JSONObject location = new JSONObject();
				location.put("lat", Double.parseDouble(lat));
				location.put("lng", Double.parseDouble(lng));
				details.put("location", location);
				
				// Append the details to the account details
				prev_acc_details.put(details);
			}
			
			// If previous account id is not null after entire file is read
			if(!prev_accId.equals("")){
				// Write account details to the output file
	    		JSONObject acc_entry = new JSONObject();
		    	acc_entry.put("accId", prev_accId);
	    		acc_entry.put("acc_details", prev_acc_details);
	    		
	    		bw_ping.write("\r\n"+ acc_entry.toString());
	    		bw_ping.flush();
	    	}
			
			br_in.close();
			bw_ping.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}

}
