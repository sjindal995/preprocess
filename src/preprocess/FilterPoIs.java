package preprocess;

import java.io.*;
import java.util.*;

import org.json.*;
import org.apache.commons.lang.StringUtils;

public class FilterPoIs extends AccDetailsFromLL{
	
	public static void main(String[] args){
		filterPois();
	}
	
	public static void filterPois(){
		try{
			BufferedReader br_final = new BufferedReader(new FileReader(final_out_file));
			br_final.readLine();
			BufferedWriter bw_filter = new BufferedWriter(new FileWriter(filtered_file));
			int it = 1;
			for(String final_line; ((final_line = br_final.readLine()) != null);){
				System.out.println(it);
				JSONObject acc_details = new JSONObject(final_line);
//				String accId = acc_details.getString("accId");
				JSONArray orders = acc_details.getJSONArray("orders");
				JSONArray updated_orders = new JSONArray();
				for(int order_index = 0; order_index < orders.length(); order_index++){
					JSONObject order = orders.getJSONObject(order_index);
					JSONArray order_details = order.getJSONArray("details");
					JSONArray total_pois = new JSONArray();
					Map<String, Boolean> poi_found = new HashMap<String, Boolean>();
					for(int details_index = 0; details_index < order_details.length(); details_index++){
						JSONObject detail_item = order_details.getJSONObject(details_index);
						JSONArray pois = detail_item.getJSONArray("pois");
						for(int poi_index = 0; poi_index < pois.length(); poi_index++){
							JSONObject poi = pois.getJSONObject(poi_index);
							if(poi_found.get(poi.getString("NAME")) == null){
								poi_found.put(poi.getString("NAME"), true);
								total_pois.put(poi);
							}
						}
					}
					JSONArray best_pois = new JSONArray();
					if(total_pois.length() > 3){
						if(order.has("address")){
							best_pois = bestLevenPois(total_pois, order.getString("address"));
						}
						else{
							best_pois = bestDistancePois(total_pois, order.getJSONObject("location"));
						}
					}
					else{
						best_pois = total_pois;
					}
					order.put("best_pois",best_pois);
					updated_orders.put(order);
				}
				acc_details.put("orders", updated_orders);
				bw_filter.write("\r\n" + acc_details.toString());
				it++;
			}
			br_final.close();
			bw_filter.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static JSONArray bestLevenPois(JSONArray pois, String address){
		try{
			int[][] leven_arr = new int[pois.length()][2];
			for(int poi_index = 0; poi_index < pois.length(); poi_index++){
				JSONObject poi = pois.getJSONObject(poi_index);
				String poi_name = poi.getString("NAME");
				int leven_distance = StringUtils.getLevenshteinDistance(poi_name.toLowerCase().replaceAll("\\s", ""), address.toLowerCase().replaceAll("\\s",""));
				leven_arr[poi_index][0] = poi_index;
				leven_arr[poi_index][1] = leven_distance;
			}
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
			for(int poi_index = 0; poi_index < Math.min(3,pois.length()); poi_index++){
				best_pois.put(pois.getJSONObject(leven_arr[poi_index][0]));
			}
			
			return best_pois;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public static JSONArray bestDistancePois(JSONArray pois, JSONObject location){
		System.err.println("wrong function!!!!!!");
		return null;
	}
}
