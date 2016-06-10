package preprocess;

import java.io.*;
import java.util.*;
import org.json.*;

public class FilterStates extends FilterPoIs{
	public static void main(String[] args){
//		filterAddrStates(filtered_addr_file);
		filterPingStates(filtered_ping_file);
	}
	
	public static void filterAddrStates(String in_file){
		try{
			BufferedReader br_in = new BufferedReader(new FileReader(in_file));
			br_in.readLine();
			BufferedWriter bw_out = new BufferedWriter(new FileWriter(state_addr_out_file));
			for(String line; ((line = br_in.readLine()) != null);){
				JSONObject acc = new JSONObject(line);
				JSONArray orders = acc.getJSONArray("orders");
				Map<String, Integer> state_count = new HashMap<String, Integer>();
				for(int order_index = 0; order_index < orders.length(); order_index++){
					JSONObject order = orders.getJSONObject(order_index);
					JSONArray state_pincodes = order.getJSONArray("state_pincodes");
					for(int details_index = 0; details_index < state_pincodes.length(); details_index++){
						JSONObject state_pin = state_pincodes.getJSONObject(details_index); 
						String state = state_pin.getJSONObject("state").getString("NAME");
						if(!state_count.containsKey(state)) state_count.put(state, 1);
						else{
							int val = state_count.get(state);
							state_count.put(state,val+1);
						}
					}
				}
				JSONArray best_states = getBestStates(state_count);
				state_count = null;
				acc.put("best_states", best_states);
				bw_out.write("\r\n" + acc.toString());
			}
			br_in.close();
			bw_out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
	
	public static void filterPingStates(String in_file){
		try{
			BufferedReader br_in = new BufferedReader(new FileReader(in_file));
			br_in.readLine();
			BufferedWriter bw_out = new BufferedWriter(new FileWriter(state_ping_out_file));
			int it = 1;
			for(String line; ((line = br_in.readLine()) != null); it++){
				System.out.println("it: " + Integer.toString(it));
				JSONObject acc = new JSONObject(line);
				JSONArray acc_details = acc.getJSONArray("acc_details");
				Map<String, Integer> state_count = new HashMap<String, Integer>();
				for(int details_index = 0; details_index < acc_details.length(); details_index++){
					JSONObject details = acc_details.getJSONObject(details_index); 
					String state = details.getJSONObject("state").getString("NAME");
					if(!state_count.containsKey(state)) state_count.put(state, 1);
					else{
						int val = state_count.get(state);
						state_count.put(state,val+1);
					}
				}
				
				JSONArray best_states = getBestStates(state_count);
			    state_count = null;
				acc.put("best_states", best_states);
				bw_out.write("\r\n" + acc.toString());
			}
			br_in.close();
			bw_out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	public static JSONArray getBestStates(final Map<String, Integer> state_count){
		try{
			Set<String> set = state_count.keySet();
		    List<String> state_keys = new ArrayList<String>(set);
		    Collections.sort(state_keys, new Comparator<String>() {
		        @Override
		        public int compare(String s1, String s2) {
		            return (state_count.get(s2) - state_count.get(s1));
		        }
		    });
		    JSONArray best_states = new JSONArray();
		    for(int b_state_index = 0; b_state_index < Math.min(3, state_keys.size()); b_state_index++){
		    	JSONObject b_state = new JSONObject();
		    	b_state.put("name", state_keys.get(b_state_index));
		    	b_state.put("count", state_count.get(state_keys.get(b_state_index)));
		    	best_states.put(b_state);
		    }
		    return best_states;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
}
