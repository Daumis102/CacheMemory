package ads2.cw1;

/**
 * Created by wim on 28/11/2017.
 * The public interface of this class is provided by Cache
 * All other methods are private. 
 * You must implement/complete all these methods
 * You are allow to create helper methods to do this, put them at the end of the class 
 */
import ads2.cw1.Cache;

import java.util.Stack;
import java.util.HashMap;
import java.util.Set;

class FullyAssocLiFoCache implements Cache {

    final private static boolean VERBOSE = false;

    final private int CACHE_SZ;
    final private int CACHELINE_SZ;
    final private int CL_MASK;
    final private int CL_SHIFT;

    // WV: because the cache replacement policy is "Last In First Out" you only need to know the "Last Used" location
    // "Last Used" means accessed for either read or write
    // The helper functions below contain all needed assignments to last_used_loc so I recommend you use these.

    private int last_used_loc;
    // WV: Your other data structures here
    // Hint: You need 4 data structures
    // - One for the cache storage
    private int[][] cache_storage;    
    // - One to manage locations in the cache
    private int[] location_stack;
    private int stack_head;
    // And because the cache is Fully Associative:
    // - One to translate between memory addresses and cache locations (Maps cache line address to a position in cache_storage)
    private HashMap<Integer, Integer> address_to_cache_loc;
    // - One to translate between cache locations and memory addresses  (Maps cache_storage elements to their cache line addresses)
    private HashMap<Integer, Integer> cache_loc_to_address;
    


    FullyAssocLiFoCache(int cacheSize, int cacheLineSize) {

        CACHE_SZ =  cacheSize;
        CACHELINE_SZ = cacheLineSize;
        CL_MASK = CACHELINE_SZ - 1;
        Double cls = Math.log(CACHELINE_SZ)/Math.log(2);
        CL_SHIFT = cls.intValue();

        last_used_loc = CACHE_SZ/CACHELINE_SZ - 1;
        // WV: Your initialisations here
        
        cache_storage = new int[CACHE_SZ/CACHELINE_SZ][CACHELINE_SZ];
        location_stack = new int[CACHE_SZ/CACHELINE_SZ];
        address_to_cache_loc = new HashMap<Integer,Integer>();
        cache_loc_to_address = new HashMap<Integer,Integer>();
       
    }

    public void flush(int[] ram, Status status) {
        if (VERBOSE) System.out.println("Flushing cache");
        // WV: Your other data structures here

        status.setFlushed(true);
    }

    public int read(int address,int[] ram,Status status) {
        return read_data_from_cache( ram, address, status);
    }

    public void write(int address,int data, int[] ram,Status status) {
        write_data_to_cache(ram, address, data, status);
    }

    private void write_data_to_cache(int[] ram, int address, int data, Status status){
        status.setReadWrite(false); // i.e. a write
        status.setAddress(address);
        status.setData(data);
        status.setEvicted(false);
        // The cache policy is write-back, so the writes are always to the cache. 
        // The update policy is write allocate: on a write miss, a cache line is loaded to cache, followed by a write operation. 
        
        // check if this address is in cache
        if(address_in_cache_line(address)) {
        	// data is already in the cache - update it
        	update_cache_entry(address, data);
        } else {
        	
        	// load cache_line from the memory
        	
        	int[] cache_line = read_from_mem_on_miss(ram, address);
        	int memAddr = cache_line_address(address);
        	int freeLocation = get_next_free_location();
        	
        	// Check if cache is full: if it is - write to the last used location, if its not - write to the next free location
        	if(cache_is_full()) { //write to the last used location
        		
        		// we need to write data from memory location we are going to overwrite back to memory
        		write_to_mem_on_evict(ram, freeLocation);
        		// update cache line in cache storage
        		cache_storage[freeLocation] = cache_line;
        		// update lookup tables
        		update_maps(memAddr,freeLocation);
        		//write new value in
        		update_cache_entry(address, data);
        		
        		
        		
        	} else { //write to the next free location
        		
        		// get next free location
        		int newAddress = get_next_free_location(); // available address to store a cache line in cache_storage
        		// update cache line in cache storage
        		cache_storage[newAddress] = cache_line;
        		// update cache line with new value
        		update_cache_entry(address, data);
        		// update lookup tables
        		update_maps(memAddr, newAddress);
        		
        	}
        }
    }

    private int read_data_from_cache(int[] ram,int address, Status status){
        status.setReadWrite(true); // i.e. a read
        status.setAddress(address);
        status.setEvicted(false);
        status.setHitOrMiss(true); // i.e. a hit
        // Your code here
        // Reads are always to the cache. On a read miss you need to fetch a cache line from the DRAM
        // If the data is not yet in the cache (read miss),fetch it from the DRAM
        // Get the data from the cache
         // ...
        
        int data;
        if(address_in_cache_line(address)) {
        	data = fetch_cache_entry(address);
        } else {
        	
        	int[] cache_line = read_from_mem_on_miss(ram, address);
        	int memAddr = cache_line_address(address);
        	//read new value from it
    		data = cache_line[cache_entry_position(address)];
        	
    		int memToWrite = get_next_free_location();
    		
    		// now update the cache
    			
        	if(cache_is_full()) {
        		// if cache is full prepare new location
        		write_to_mem_on_evict(ram, memToWrite);
        	}
        	
    		// update cache line in cache storage
    		cache_storage[memToWrite] = cache_line;
    		// update lookup tables
    		update_maps(memAddr,memToWrite);      
        }
        status.setData(data);
        return data;
    }

    // You might want to use the following methods as helpers
    
    public void update_maps(int memAddr, int catcheAddr) {
    	address_to_cache_loc.put(memAddr, catcheAddr);
    	cache_loc_to_address.put(catcheAddr, memAddr);
    }
    
    
    
    // On read miss, fetch a cache line    
    // * changed from type void to int[] *
    private int[] read_from_mem_on_miss(int[] ram,int address){
        int[] cache_line = new int[CACHELINE_SZ];
        int loc;
        // Your code here
         // ...

        last_used_loc=loc;
        return cache_line;
   }

    // On write, modify a cache line
    private void update_cache_entry(int address, int data){
         // get location of cache line in cache memory from the map
        int cache_line_addr = address_to_cache_loc.get(cache_line_address(address));
        // get offset of address location in cache line
        int offset = cache_entry_position(address);
        // replace data
        cache_storage[cache_line_addr][offset] = data;
        // update last used location
        last_used_loc=cache_line_addr;
       }

    // When we fetch a cache entry, we also update the last used location
    private int fetch_cache_entry(int address){
        int[] cache_line;
        
        // get location of cache line in cache memory from the map
        int cache_line_addr = address_to_cache_loc.get(cache_line_address(address));
        
        // get cache_line from cache storage
        cache_line = cache_storage[cache_line_addr];
        
        last_used_loc=cache_line_addr;
        return cache_line[cache_line_address(address)];
    }

    private int get_next_free_location(){
        int loc;
    	if(cache_is_full()) {
    		// we need to write data from last used memory location back to memory
    		loc = last_used_loc;
    	} else {
    		loc = location_stack[stack_head];
    	}
    	
    	
        return loc;
    }

    private void evict_location(int loc){
         location_stack[stack_head] = loc;
         stack_head++;
         
    }

    private boolean cache_is_full(){
         if(stack_head == 0) {
        	 return true;
         }
         return false;
        
    }

    /**
     * Writes data from cache to memory when it is being deleted from cache
     * @param ram
     * @param loc
     */
    private void write_to_mem_on_evict(int[] ram, int loc){

        int evicted_cl_address;
        int[] cache_line;
        if (VERBOSE) System.out.println("Cache line to RAM: ");
        // Your code here
         // ...
        

        evict_location(loc);
    }

    // REAL NAME address_in_cache
    private boolean address_in_cache_line(int address) {
    	
    	// get the address of cache line and check if this cache line exists in cache
    	return address_to_cache_loc.containsKey(cache_line_address(address));
        
    }
    
    // return address of a cache_line in which given address should live
    private int cache_line_address(int address) {
        return address>>CL_SHIFT;
    }

    /**
     * 
     * @param cl_address the whole address of the data
     * @return
     */
    private int cache_entry_position(int cl_address) {
        return cl_address & CL_MASK;
    }

    /**
     * 
     * @param cl_address the whole address of the data
     * @return
     */
    private int mem_address(int cl_address) {
        return cl_address<<CL_SHIFT;
    }

}
