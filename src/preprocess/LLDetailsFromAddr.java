package preprocess;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class LLDetailsFromAddr extends LatLng2Poi{
	
	public static void llDetailsFromAddress(){
		try{
			
			
			int it=0;
			
			// Input file format: ["orderId | accId | addr1 | addr2 | city | state | pincode"]
			BufferedReader br = new BufferedReader(new FileReader(addr_in_file));
			
			// Output file
			BufferedWriter bw = new BufferedWriter(new FileWriter(addr_ll_file));
			
			String orderId, accId, addr1, pincode, state, city, addr2;
	    	List<String> items;
	    	
	    	// Store account id and details collected till the previous iteration
	    	String prev_accId = "";
	    	JSONArray prev_acc_orders = new JSONArray();
	    	for(String line; ((line = br.readLine()) != null) && (it < 101); ) {
	    		try{
	    			
		    		total_addr++;
		    		
		    		// Split the line string to list of strings
			    	items = Arrays.asList(line.split("\\s*\\|\\s*"));
			    	
			    	// Extract details from the list
			    	orderId = items.get(0);
			    	accId = items.get(1);
			    	addr1 = items.get(2);
			    	addr2 = items.get(3);
			    	pincode = items.get(items.size()-1);
			    	state = items.get(items.size()-2);
			    	city = items.get(items.size()-3);		    	
			    	
			    	// If account id in current iteration is not same as account id in previous iteration
			    	if(!accId.equals(prev_accId)){
			    		// If previous account id is not empty
			    		if(!prev_accId.equals("")){
			    			// Write the account details to the output file
				    		JSONObject acc_entry = new JSONObject();
				    		acc_entry.put("accId", prev_accId);
				    		acc_entry.put("orders", prev_acc_orders);
				    		
				    		bw.write("\r\n"+ acc_entry.toString());
				    		bw.flush();
			    		}
			    		
			    		// Empty the previous account details
			    		int start = prev_acc_orders.length();
			    		for(int order_index = start - 1; order_index >= 0; order_index--){
			    			prev_acc_orders.remove(order_index);
			    		}
			    		// Change the previous account id variable
			    		prev_accId = accId;
			    		it++;
			    	}
			    	
			    	// Setup url for geocoding api
			    	String url = flip_url + "/geocode?addr1=";
					String path = addr1 + "&addr2=" + addr2 + "&city=" + city + "&state=" + state + "&pincode=" + pincode;
					
					// Encode url to replace any special chars in the path
					path = URLEncoder.encode(path, "UTF-8");
					url += path;
			    	System.out.println(String.valueOf(it) + " :: " + items);
					System.out.println("url:::  " + url);
					
					// Call flip api for geocoding
					JSONObject geocode_result_obj = getApiResult(url,0);
					Thread.sleep(200);
					
					// Object to store current order details
					JSONObject order = new JSONObject();
					order.put("orderId", orderId);
					order.put("address", addr1 + " | " + addr2 + " | " + city + " | " + state + " | " + pincode);
					
					// Jsonarray to store the details of the current order processed
		    		JSONArray details_arr = new JSONArray();
	
					// If invalid response obtained
					if(geocode_result_obj.has("message")){
						System.err.println(geocode_result_obj.getString("message"));
						continue;
					}
					
					// Format of response of geocode api: {"results":[...]}
					JSONArray geocode_result_arr = (JSONArray) geocode_result_obj.get("results");				
		    		
		    		for(int index = 0; index < geocode_result_arr.length(); index++){
		    			// Single object from the array of results obtained from geocode
						JSONObject geocode_result_item = geocode_result_arr.getJSONObject(index);
						
						// Array storing the address_component field in the object
						JSONArray geocode_result_item_addr_comp = geocode_result_item.getJSONArray("address_components");
						
						// Store the location only if doctype of the item is address_point or poi_point else ignore
						if(geocode_result_item_addr_comp.getJSONObject(0).getString("DOCTYPE").equals("Poi_point") || geocode_result_item_addr_comp.getJSONObject(0).getString("DOCTYPE").equals("Address_point")){
							// Get lat/lng details from geometry.location field in the result item
							JSONObject location = geocode_result_item.getJSONObject("geometry").getJSONObject("location");
														
							// Store the address_component, location and pincode fields in a jsonobject
							JSONObject details = new JSONObject();
							details.put("addr_component", geocode_result_item_addr_comp);
							details.put("location", location);
							
							// Add details object to the array of details for this order
							details_arr.put(details);
						}
					}
						
		    		// Store details array in the order jsonobject
					order.put("details", details_arr);
					
					// Add current order to the cumulative orders stored for the account
					prev_acc_orders.put(order);
	    		}catch (Exception e){
	    			e.printStackTrace();
	    		}
				
		    }
	    	
	    	// If previous account id is not empty when file is finished
	    	if(!prev_accId.equals("")){
	    		// Write the account details to the output file
	    		JSONObject acc_entry = new JSONObject();
		    	acc_entry.put("accId", prev_accId);
	    		acc_entry.put("orders", prev_acc_orders);
	    		
	    		bw.write("\r\n"+ acc_entry.toString());
	    		bw.flush();
	    	}
	    	
	    	br.close();
	    	bw.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
}
