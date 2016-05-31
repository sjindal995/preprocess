package preprocess;

import org.json.JSONArray;
import org.json.JSONObject;

public class LatLng2Poi extends Wrapper{

	public static JSONArray getPois(String lat, String lng){
		try{
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
			
			// try to store the poi points present in the result data
			// it is possible that no poi points are present at the provided lat/lng values
			// in that case this will give exception for jsonObject["Poi_point"] not found
			try{
				poi_points = (JSONArray) poi_result_data.get("Poi_point");
			} catch (Exception e){
			}
			return poi_points;
		} catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}

}
