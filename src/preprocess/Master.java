package preprocess;

public class Master extends AccDetailsFromLL{
	public static void main(String[] args){
		// store the details for each account in file as well as for each poi in map
		llDetailsFromAddress();
		
		accountDetailsFromAddr();
		
		System.out.println("total addresses: " + Integer.toString(total_addr) + " , poi addresses: " + Integer.toString(poi_addr));
	}
}
