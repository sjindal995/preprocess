package preprocess;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class LLDetailsFromAddr extends LatLng2Poi{
	
	public static void accountDetailsFromAddress(){
		try{
			
			// file initializations
			initializeFile(addr_out_file);
			initializeFile(addr_out_file + "_2");
			
			int it=1;
			
			BufferedReader br = new BufferedReader(new FileReader(addr_in_file));
			String orderId, accId, addr1, pincode, state, city, addr2;
	    	List<String> items;
	    	for(String line; ((line = br.readLine()) != null) && (it < 1001); ) {
	    		total_addr++;
	    		// split the line to list of strings
		    	items = Arrays.asList(line.split("\\s*\\|\\s*"));
		    	System.out.println(String.valueOf(it) + " :: " + items + " \n::: " + items.size());
		    	
		    	// extract details from the current line
		    	orderId = items.get(0);
		    	accId = items.get(1);
		    	// addr1 is only the single item after accId in the list
		    	addr1 = items.get(2);
		    	addr2 = items.get(3);
		    	pincode = items.get(items.size()-1);
		    	state = items.get(items.size()-2);
		    	city = items.get(items.size()-3);		    	

		    	it++;
		    	
		    	// setup url for geocoding api
		    	String url = flip_url + "/geocode?addr1=";
				String path = addr1 + "&addr2=" + addr2 + "&city=" + city + "&state=" + state + "&pincode=" + pincode;
				// encode url to replace any special chars in the path
				path = URLEncoder.encode(path, "UTF-8");
				url += path;
				System.out.println("url:::  " + url);
				
				// call flip api for geocoding
				JSONObject geocode_result_obj = getApiResult(url,0);
				Thread.sleep(500);
				
				//entry object stores the details for the accId regarding this order
				JSONObject entry = new JSONObject();
				entry.put("orderId", orderId);
				entry.put("address", addr1 + "," + addr2);
				//jsonarray to store the details of the current order processed
	    		JSONArray details_arr = new JSONArray();

				
				if(!geocode_result_obj.has("message")){
					// format of response of geocode api: {"results":[...]}
					JSONArray geocode_result_arr = (JSONArray) geocode_result_obj.get("results");
					
										
		    		
		    		for(int index = 0; index < geocode_result_arr.length(); index++){
		    			//single object from the array of results obtained from geocode
						JSONObject geocode_result_item = geocode_result_arr.getJSONObject(index);
						
						// array storing the address_component field in the object
						JSONArray geocode_result_item_addr_comp = geocode_result_item.getJSONArray("address_components");
						
						//store the location only if doctype of the item is address_point or poi_point else ignore
						if(geocode_result_item_addr_comp.getJSONObject(0).getString("DOCTYPE").equals("Poi_point") || geocode_result_item_addr_comp.getJSONObject(0).getString("DOCTYPE").equals("Address_point")){
							// get lat/lng details from geometry.location field in the result item
							JSONObject location = geocode_result_item.getJSONObject("geometry").getJSONObject("location");
							
							// get pincode details of the result item
							String pincode1 = (geocode_result_item.getJSONArray("pincodes")).getString(0);
							
							// store the address_component, location and pincode fields in a jsonobject
							JSONObject details = new JSONObject();
							details.put("addr_component", geocode_result_item_addr_comp);
							details.put("location", location);
							details.put("pincode", pincode1);
							
							// add details object to the array of details for this order
							details_arr.put(details);
						}
					}
				}
				else{
					System.err.println(geocode_result_obj.get("message"));
				}
		    		
	    		// store details array in the order entry jsonobject
				entry.put("details", details_arr);
				
				System.out.println("out file: " + addr_out_file);
				
				// open the latest updated file to read from and the complement file to write into
				FileReader fr = new FileReader(addr_out_file);
	            BufferedReader br1 = new BufferedReader(fr);
	            FileWriter fw = new FileWriter(comp(addr_out_file));
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
	                if(js_line.getString("accId").equals(accId)){
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
	            	js_line.put("accId", accId);
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
	            addr_out_file = comp(addr_out_file);
			
		    }
	    	br.close();
	    	File d_file = new File(comp(addr_out_file));
			d_file.delete();
			
			File r_file = new File(addr_out_file);
			File n_file = new File("addr_ll_details");
			r_file.renameTo(n_file);
			addr_out_file = "addr_ll_details";
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
}
