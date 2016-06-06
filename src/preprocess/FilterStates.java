package preprocess;

import java.io.*;
import java.util.*;
import org.json.*;

public class FilterStates extends FilterPoIs{
	public static void main(String[] args){
		
	}
	
	public static void filterAddrStates(String in_file){
		try{
			BufferedReader br_in = new BufferedReader(new FileReader(in_file));
			br_in.readLine();
			BufferedWriter br_out = new BufferedWriter(new FileWriter(state_out_file));
			
			for(String line; ((line = br_in.readLine()) != null);){
				JSONObject acc = new JSONObject(line);
				String accId = acc.getString("accId");
				JSONArray orders = acc.getJSONArray("orders");
				Map<String, Integer> state_count = new HashMap<String, Integer>();
				for(int order_index = 0; order_index < orders.length(); order_index++){
					JSONObject order = orders.getJSONObject(order_index);
					JSONArray details_arr = order.getJSONArray("details");
					for(int details_index = 0; details_index < details_arr.length(); details_index++){
						JSONObject details = details_arr.getJSONObject(details_index);
						String pincode = details.getString("pincode");
						JSONArray addr_component = details.getJSONArray("addr_component"); 
						String state = addr_component.getJSONObject(addr_component.length()-1).getString("NAME");
						if(!state_count.containsKey(state)) state_count.put(state, 1);
						else{
							int val = state_count.get(state);
							state_count.put(state,val+1);
						}
					}
				}
				Set<String> set = state_count.keySet();
			    List<String> state_keys = new ArrayList<String>(set);
			    final Map<String,Integer> f_state_count = state_count;
			    Collections.sort(state_keys, new Comparator<String>() {
			        @Override
			        public int compare(String s1, String s2) {
			            return (f_state_count.get(s2) - f_state_count.get(s1));
			        }
			    });
			    JSONArray best_states = new JSONArray();
			    for(int b_state_index = 0; b_state_index < Math.min(3, state_keys.size()); b_state_index++){
			    	JSONObject b_state = new JSONObject();
			    	b_state.put("name", state_keys.get(b_state_index));
			    	b_state.put("count", state_count.get(state_keys.get(b_state_index)));
			    	best_states.put(b_state);
			    }
				state_count = null;
				acc.put("best_states", best_states);
				br_out.write("\r\n" + acc.toString());
			}
			br_in.close();
			br_out.close();
		} catch (Exception e){
			e.printStackTrace();
		}
		
	}
}
