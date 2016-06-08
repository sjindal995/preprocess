package preprocess;

import java.io.*;
import java.util.*;

import org.json.*;
import org.apache.commons.lang.StringUtils;

public class FilterPoIs extends AccDetailsFromLL{
	
	public static void main(String[] args){
		filterAddrPois();
	}
	
	public static void filterAddrPois(){
		try{
			
			// file containing all the pois
			BufferedReader br_final = new BufferedReader(new FileReader(addr_out_file));
			br_final.readLine();
			
			// output file to store best pois for each order along with other details for each account
			BufferedWriter bw_filter = new BufferedWriter(new FileWriter(filtered_addr_file));
			
			int it = 1;
			
			for(String final_line; ((final_line = br_final.readLine()) != null);){
				
				System.out.println(it);
				
				JSONObject acc_details = new JSONObject(final_line);
				
				// retrieve orders made by the account in current line
				JSONArray orders = acc_details.getJSONArray("orders");
				
				// array to store orders along with best matching pois
				JSONArray updated_orders = new JSONArray();
				
				for(int order_index = 0; order_index < orders.length(); order_index++){
					
					JSONObject order = orders.getJSONObject(order_index);
					
					JSONArray order_details = order.getJSONArray("details");
					
					// array to store all the pois corresponding to single order
					JSONArray total_pois = new JSONArray();
					
					// map to prevent duplicate storage of pois
					Map<String, Boolean> poi_found = new HashMap<String, Boolean>();
					
					for(int details_index = 0; details_index < order_details.length(); details_index++){
						
						JSONObject detail_item = order_details.getJSONObject(details_index);
						
						JSONArray pois = detail_item.getJSONArray("pois");
						
						for(int poi_index = 0; poi_index < pois.length(); poi_index++){
							
							JSONObject poi = pois.getJSONObject(poi_index);
							
							// add poi to the array if it has not been seen before and mark found for this as true
							if(poi_found.get(poi.getString("NAME")) == null){
								
								poi_found.put(poi.getString("NAME"), true);
								
								total_pois.put(poi);
							}
						}
					}
					
					
					
					JSONArray best_pois = new JSONArray();
					
					// if total number of pois are greater than 3
					if(total_pois.length() > 3){
						// get best 3 pois
						best_pois = bestLevenPois(total_pois, order.getString("address"));
					}
					else{
						// else keep all the pois
						best_pois = total_pois;
					}
					
					// add the best pois to the order
					order.put("best_pois",best_pois);
					
					// add order to the updated orders array
					updated_orders.put(order);
					total_pois = null;
					poi_found = null;
				}
				
				// replace the orders array in account details woth updated orders array
				acc_details.put("orders", updated_orders);
				
				// write the account details to the file
				bw_filter.write("\r\n" + acc_details.toString());
				
				it++;
			}
			
			br_final.close();
			bw_filter.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static void filterPingPois(){
		try{
			// file containing all the pois
			BufferedReader br_final = new BufferedReader(new FileReader(ping_file));
			br_final.readLine();
			
			// output file to store best pois for each lat/long along with other details for each account
			BufferedWriter bw_filter = new BufferedWriter(new FileWriter(filtered_ping_file));
			
			int it = 1;
			
			for(String line; ((line = br_final.readLine()) != null);){
				
				System.out.println(it);
				
				// Get current account and its details
				JSONObject acc = new JSONObject(line);
				JSONArray acc_details = acc.getJSONArray("acc_details");
				
				// Array to store account details along with best pois for each lat/long
				JSONArray updated_acc_details = new JSONArray();

				for(int details_index = 0; details_index < acc_details.length(); details_index++){
					
					// Details corresponding to single lat/long
					JSONObject detail = acc_details.getJSONObject(details_index);
					
					// PoIs for the lat/long
					JSONArray pois = detail.getJSONArray("pois");
					
					// Store top PoIs
					JSONArray best_pois = new JSONArray();
					for(int poi_index = 0; poi_index < Math.min(3, pois.length()); poi_index++){
						best_pois.put(pois.getJSONObject(poi_index));
					}
					
					// Store the updated details
					detail.put("best_pois", best_pois);
					updated_acc_details.put(detail);
				}
				
				// Replace the account details with the updated one
				acc.put("acc_details", updated_acc_details);
				
				bw_filter.write(acc.toString());
			}
				
			br_final.close();
			bw_filter.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static JSONArray bestLevenPois(JSONArray pois, String address){
		try{
			// leven_arr[i] = [index, leven_distance]
			int[][] leven_arr = new int[pois.length()][2];
			
			// populate the leven_arr with leven distances of each poi from the order address
			for(int poi_index = 0; poi_index < pois.length(); poi_index++){
				
				JSONObject poi = pois.getJSONObject(poi_index);
				
				String poi_name = poi.getString("NAME");
				// obtain leven distance
				int leven_distance = StringUtils.getLevenshteinDistance(poi_name.toLowerCase().replaceAll("\\s", ""), address.toLowerCase().replaceAll("\\s",""));
				
				leven_arr[poi_index][0] = poi_index;
				leven_arr[poi_index][1] = leven_distance;
			}
			
			// sort the leven_arr based on the leven distance
			Arrays.sort(leven_arr, new Comparator<int[]>(){
				public int compare(int[] a, int[] b){
					return (a[1] - b[1]);
				}
			});

			System.out.println(address);
			for(int poi_index = 0; poi_index < pois.length(); poi_index++){
				System.out.println(Integer.toString(leven_arr[poi_index][0]) + " : " + Integer.toString(leven_arr[poi_index][1]) + " : " + pois.getJSONObject(leven_arr[poi_index][0]).getString("NAME"));
			}
			
			JSONArray best_pois = new JSONArray();
			
			//extract 3 best pois
			for(int poi_index = 0; poi_index < 3; poi_index++){	
				best_pois.put(pois.getJSONObject(leven_arr[poi_index][0]));
			}
			
			return best_pois;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static JSONArray bestDistancePois(JSONArray pois, JSONObject location){
		try{
			// convert json array to simple array of JSONObjects
			JSONObject[] poi_arr = new JSONObject[pois.length()];
			
			for(int poi_index = 0; poi_index < pois.length(); poi_index++){
				poi_arr[poi_index] = pois.getJSONObject(poi_index);
			}
			
			// sort the array based on distance field of the JSONObject
			Arrays.sort(poi_arr,new Comparator<JSONObject>(){
				public int compare(JSONObject a, JSONObject b){
					try{
						return ((int) Math.signum(a.getDouble("DISTANCE") - b.getDouble("DISTANCE")));
					}catch (Exception e){
						e.printStackTrace();
					}
					return 0;
				}
			});
			
			for(int poi_index = 0; poi_index < pois.length(); poi_index++){
				System.out.println(Double.toString(poi_arr[poi_index].getDouble("DISTANCE")) + " :: " + poi_arr[poi_index].getString("NAME"));
			}
			
			JSONArray best_pois = new JSONArray();
			
			//extract best 3 pois
			for(int poi_index = 0; poi_index < 3; poi_index++){
				best_pois.put(poi_arr[poi_index]);
			}

			System.err.println("wrong function!!!!!!");			
			return best_pois;
			
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
