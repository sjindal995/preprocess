package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class PoiToAcc extends AccDetailsFromLL{
	
	public static void main(String[] args){
		
		// store the details for each account in file as well as for each poi in map
		createPoi2Acc("acc_details");
		
		File d_file = new File(comp(poi_file));
		d_file.delete();
		
		File r_file = new File(poi_file);
		File n_file = new File("poi2acc");
		r_file.renameTo(n_file);
		
		//display final file name in which result is stored
		System.out.println("completed : " + poi_file);
		
	}
	
	
	public static void createPoi2Acc(String acc_file){
		try{
			// initilize poi2acc files
			initializeFile(poi_file);
			initializeFile(poi_file + "_2");
			
			// open file containing account details
			FileReader fr_acc = new FileReader(acc_file);
	        BufferedReader br_acc = new BufferedReader(fr_acc);
	        
	        // first line is empty
	        br_acc.readLine();
	        
	        //iterate over each account
	        for(String line; (line = br_acc.readLine()) != null;){
	        	// list of booleans storing if the poi present in the particular account details
	        	// has been encountered while reading the latest updated poi2acc file
	        	List<Boolean> poi_found = new ArrayList<Boolean>();
	        	
	        	// list storing pois corresponding to the current account
				List<String> acc_pois = new ArrayList<String>();
				
				// get jsonobject from string read from file
				JSONObject js_line = new JSONObject(line);
				
				// get account id
				String accId = js_line.getString("accId");
				// get orders made from this account
				JSONArray order_arr = js_line.getJSONArray("orders");
				
				// iterate over the orders
				for(int order_index = 0; order_index < order_arr.length(); order_index++){
					// get current order
					JSONObject order = order_arr.getJSONObject(order_index);
					// get current order details storing various lat longs and pois
					// obtained from geocoding and nearby_map_data api
					JSONArray order_details = order.getJSONArray("details");

					// iterate over each lat/lng
					for(int details_index = 0; details_index < order_details.length(); details_index++){
						// retrieve object from the array
						JSONObject detail_item = order_details.getJSONObject(details_index);
						
						// get array of POIs from the jsonobject in fields "pois"
						JSONArray pois = detail_item.getJSONArray("pois");
						
						// iterate over all the pois in the array
						for(int poi_index = 0; poi_index < pois.length(); poi_index++){
							// get mmi_id of poi
							String poi_mmi_id = pois.getJSONObject(poi_index).getString("MMI_ID");
							
							// if this poi is not encountered earlier
							if(!acc_pois.contains(poi_mmi_id)){
								// add poi to the array and add false value to found array at its index
								acc_pois.add(poi_mmi_id);
								poi_found.add(false);
							}
						}
					}
				}
				
				// read from the latest updated poi file and write to its complement file
				FileReader fr_poi = new FileReader(poi_file);
	            BufferedReader br_poi = new BufferedReader(fr_poi);
	            FileWriter fw_poi = new FileWriter(comp(poi_file));
	            BufferedWriter bw_poi = new BufferedWriter(fw_poi);

	            //first line is empty
	            br_poi.readLine();

	            //iterate over every poi
	            for(String line_poi; (line_poi = br_poi.readLine()) != null;){
	            	// retrieve object stored in format {"mmi_id":"","accs":""}
	            	JSONObject js_poi = new JSONObject(line_poi);
	            	// get mmi_id
	            	String mmi_id = js_poi.getString("mmi_id");
	            	// get accounts corresponding to the poi
	            	JSONArray acc_arr = js_poi.getJSONArray("accs");
	            
	            	// store all the accounts in the list
	            	List<String> acc_list = new ArrayList<String>();
	            	for(int acc_arr_index = 0; acc_arr_index < acc_arr.length(); acc_arr_index++){
	            		acc_list.add(acc_arr.getString(acc_arr_index));
	            	}
	            	
	            	// if current poi being read from poi file is also found in the current account processed
	            	if(acc_pois.contains(mmi_id)){
	            		// get index of the poi in the array storing all pois for the current account
	            		int index = acc_pois.indexOf(mmi_id);
	            	
	            		// set poi_found for the poi as true
	            		poi_found.set(index, true);
	            		
	            		// add the account id to the list of accounts corresponding to the poi
	            		acc_list.add(accId);
	            		
	            		// update accs field of the object
	            		js_poi.put("accs", acc_list);
	            		
	            		// write to the output file
	            		bw_poi.write("\r\n" + js_poi.toString());
	            		bw_poi.flush();
	            	}
	            	else{
	            		// else if current poi read from the input file is not in the pois of account being processed 
	            		bw_poi.write("\r\n" + line_poi);
	            		bw_poi.flush();
	            	}
	            	
	            }

	            // if there is still poi left corresponding to the account which is not found in input file
	            // i.e. poi encountered for first time
	            while(poi_found.contains(false)){
	            	// get the index of the poi in the array
	            	int index = poi_found.indexOf(false);
	            	// get the poi mmi_id at the index retrieved in previous step
	            	String mmi_id = acc_pois.get(index);
	       
	            	// construct a list of String type to store the accounts corresponding to this poi
	            	List<String> acc_list = new ArrayList<String>();
	            	// add current account id to this list
	            	acc_list.add(accId);
	            	
	            	// create jsonobject to store mmi_id and corresponding accounts
	            	JSONObject js_poi = new JSONObject();
	            	js_poi.put("mmi_id", mmi_id);
	            	js_poi.put("accs", acc_list);
	            
	            	//write the object to the output file
	            	bw_poi.write("\r\n"+js_poi.toString());
	            	bw_poi.flush();
	            	
	            	// set poi found true for this poi
	            	poi_found.set(index,true);
	            }
	            br_poi.close();
	            bw_poi.close();
	            
	            // change the latest updated poi file 
	            poi_file = comp(poi_file);
	        }
	        br_acc.close();
		} catch(Exception e){
			e.printStackTrace();
		}
	}

}
