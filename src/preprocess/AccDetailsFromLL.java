package preprocess;

import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccDetailsFromLL extends LLDetailsFromAddr{
	
	public static void accountDetailsFromAddr(){
		try{
			
			// read the lat long details file
			BufferedReader br_addr = new BufferedReader(new FileReader(addr_ll_file));
			
			//empty first line
			br_addr.readLine();
			
			// output file containing poi details for the accounts as well 
			BufferedWriter bw_out = new BufferedWriter(new FileWriter(addr_out_file));
			
			int it=1;
			
			for(String addr_line; ((addr_line = br_addr.readLine()) != null);){
				
				// convert string line to jsonobject
				JSONObject acc_details_geocode = new JSONObject(addr_line);
				
				// Account ID of the current account processed
				String accId = acc_details_geocode.getString("accId");
				
				// orders made by the current account
				JSONArray orders = acc_details_geocode.getJSONArray("orders");
				
				// JSONArray to store orders with pois included
				JSONArray updated_orders = new JSONArray();
				
				System.out.println(it + " <<<< " + accId);
				
				// for each order made by the account
				for(int order_it = 0; order_it < orders.length(); order_it++){
					// retrieve an order
					JSONObject order = orders.getJSONObject(order_it);
					
					// boolean variable to check if there exists a poi corresponding to this address
					Boolean poi_found = false;
					
					// get details of the current order
					JSONArray details_arr = order.getJSONArray("details");
					// create new array to store details along with pois
					JSONArray updated_arr = new JSONArray();
					
					for(int details_it = 0; details_it < details_arr.length(); details_it++){
						// retrieve one lat/long from the order details
						JSONObject details_item = details_arr.getJSONObject(details_it);
						JSONObject location = details_item.getJSONObject("location");
						Double lat = location.getDouble("lat");
						Double lng = location.getDouble("lng");
						
						// get pois for the lat longs obtained
						JSONArray poi_points = getPois(Double.toString(lat), Double.toString(lng));

						if(poi_points.length() > 0) poi_found = true;
						
						// store pois in the details jsonobject storing address_component, location, pincode
						details_item.put("pois", poi_points);
						
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
			// input file with format : accId,lat,lng
			BufferedReader br_in = new BufferedReader(new FileReader(ll_in_file));
			
			// accId and details for that account in the previous iteration
			String prev_accId = "";
			JSONArray prev_acc_details = new JSONArray();
			
			// output file to store state, pincode, pois for each account 
			BufferedWriter bw_ping = new BufferedWriter(new FileWriter(ping_file));
			
			for(String line; ((line = br_in.readLine()) != null);){
				
				String accId, lat, lng;

				// split the line to list of strings and retrieve different fields
				List<String> items = new ArrayList<String>();
		    	items = Arrays.asList(line.split("\\s*,\\s*"));
		    	
		    	accId = items.get(0);
		    	lat = items.get(1);
		    	lng = items.get(2);
		    	
		    	// if current account is not same as previous account
		    	if(!accId.equals(prev_accId)){
		    		// if previous account is not empty
		    		if(!prev_accId.equals("")){
		    			
		    			//write the previous account details to the output file
		    			JSONObject acc_entry = new JSONObject();
			    		acc_entry.put("accId", prev_accId);
			    		acc_entry.put("acc_details", prev_acc_details);
		    			bw_ping.write("\r\n" + acc_entry.toString());
		    			bw_ping.flush();
		    		}
		    		
		    		// clear the previous JSONArray
		    		int start = prev_acc_details.length();
		    		for(int index = start - 1; index >= 0; index--){
		    			prev_acc_details.remove(index);
		    		}
		    		
		    		// change the previous account id variable
		    		prev_accId = accId;
		    	}
		    	
		    	// url for nearby map data api
		    	String url = flip_url + "/nearby_map_data?point=";
				url += lat + "," + lng + "&max_dist=0.05";
				//no need to encode this url as no special chars possible
				
				// call flip api for nearby_map_data
				JSONObject poi_result_obj = getApiResult(url, 0);
				Thread.sleep(500);
				
				// format of nearby_map_data response: {"nearby_map_data":{...}}
				JSONObject poi_result_data = (JSONObject) poi_result_obj.get("nearby_map_data");
				
				// initialize jsonarray to store retrieved poi points
				JSONArray poi_points = new JSONArray();
				
				String pincode = "";
				
				//jsonobject to receive state region
				JSONObject state = new JSONObject();
				
				// retrieve pincode
				try{
					pincode = ((JSONArray) poi_result_data.get("Pincode_region")).getJSONObject(0).getString("NAME");
				} catch (Exception e){
					e.printStackTrace();
				}
				
				// retrieve state
				try{
					state = ((JSONArray) poi_result_data.get("State_region")).getJSONObject(0);
				} catch (Exception e){
					e.printStackTrace();
				}
				
				// retrieve poi_points
				try{
					poi_points = (JSONArray) poi_result_data.get("Poi_point");
				} catch (Exception e){
					e.printStackTrace();
				}
				
				//object to store details of current line
				JSONObject details = new JSONObject();
				details.put("pois", poi_points);
				details.put("pincode", pincode);
				details.put("state", state);
				
				JSONObject location = new JSONObject();
				location.put("lat", Double.parseDouble(lat));
				location.put("lng", Double.parseDouble(lng));
				details.put("location", location);
				
				// append the details to the account details
				prev_acc_details.put(details);
			}
			
			// if previous account id is not null after entire file is read
			if(!prev_accId.equals("")){
				//write account details to the output file
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
