package preprocess;

import java.io.*;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONObject;

public class AccDetailsFromLL extends LLDetailsFromAddr{
	
	public static void getAccountDetails(){
		try{
			BufferedReader br_addr = new BufferedReader(new FileReader(addr_out_file));
			br_addr.readLine();
			BufferedWriter bw_final = new BufferedWriter(new FileWriter(final_out_file));
			int it=1;
			for(String addr_line; ((addr_line = br_addr.readLine()) != null);){
				JSONObject acc_details_geocode = new JSONObject(addr_line);
				String accId = acc_details_geocode.getString("accId");
				JSONArray orders = acc_details_geocode.getJSONArray("orders");
				JSONArray updated_orders = new JSONArray();
				System.out.println(it + " <<<< " + accId);
				for(int order_it = 0; order_it < orders.length(); order_it++){
					JSONObject order = orders.getJSONObject(order_it);
					Boolean poi_found = false;
					JSONArray details_arr = order.getJSONArray("details");
					JSONArray updated_arr = new JSONArray();
					for(int details_it = 0; details_it < details_arr.length(); details_it++){
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
					order.put("details", updated_arr);
					updated_orders.put(order);
					if(poi_found) poi_addr++;
				}

				BufferedReader br_ll = new BufferedReader(new FileReader(ll_in_file));
				br_ll.readLine();
				for(String ll_line; ((ll_line = br_ll.readLine()) != null);){
					JSONObject order = accountDetailsFromLatLng(ll_line, accId);
					if(order != null){
						updated_orders.put(order);
					}
				}
				br_ll.close();
				JSONObject acc_details = new JSONObject();
				acc_details.put("accId", accId);
				acc_details.put("orders", updated_orders);
				bw_final.write("\r\n" + acc_details.toString());
				it++;
			}
			br_addr.close();
			bw_final.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	
	public static JSONObject accountDetailsFromLatLng(String line, String accId){
		try{		
			String order_id, acc_id, lat, lng;
			List<String> items = new ArrayList<String>();
			// split the line to list of strings
	    	items = Arrays.asList(line.split("\\s*,\\s*"));
	    	
	    	order_id = items.get(0);
	    	acc_id = items.get(1);
	    	lat = items.get(2);
	    	lng = items.get(3);
	    	
	    	if(!acc_id.equals(accId)) return null;
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
			
			String pincode = "";
			
			//jsonarray to receive state regions received
			JSONObject state = new JSONObject();
			// try to store the poi points present in the result data
			// it is possible that no poi points are present at the provided lat/lng values
			// in that case this will give exception for jsonObject["Poi_point"] not found
			try{
				poi_points = (JSONArray) poi_result_data.get("Poi_point");
				pincode = ((JSONArray) poi_result_data.get("Pincode_region")).getJSONObject(0).getString("NAME");
				state = ((JSONArray) poi_result_data.get("State_region")).getJSONObject(0);
				state.put("DOCTYPE", "State_region");
			} catch (Exception e){
				e.printStackTrace();
			}
			
			//object to store details of current order
			JSONObject details = new JSONObject();						
			// store pois in the details jsonobject storing address_component, location, pincode
			details.put("pois", poi_points);
			// store pincode
			details.put("pincode", pincode);
			
			JSONArray addr_component = new JSONArray();
			addr_component.put(state);
			// store state
			details.put("addr_component", addr_component);
			
			JSONObject location = new JSONObject();
			location.put("lat", Double.parseDouble(lat));
			location.put("lng", Double.parseDouble(lng));
			details.put("location", location);
			
			// object as order made by an account to be stored in orders array corresponding to particular account
			JSONObject entry = new JSONObject();
			entry.put("orderId", order_id);
			
			JSONArray details_arr = new JSONArray();
			details_arr.put(details);
			
			entry.put("details", details_arr);
			return entry;
				
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

}
