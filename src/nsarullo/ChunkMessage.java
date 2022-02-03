package nsarullo;

import java.io.Serializable;
import java.util.Arrays;

//Used to transport "chunks" of the array the server generates when it is run to each client.
public class ChunkMessage implements Serializable
{
	public static long serialVersionUID = 1L;

	/*The nums array in a ChunkMessage Object will be used to transport
	one chunk of the server's initial starting array to one client.*/
	private double[] nums;
	
	public double[] getNums() {
		return nums;
	}

	public ChunkMessage()
	{
		super();
	}
	
	public ChunkMessage(double[] nums)
	{
		this.nums = nums;
	}

	public String toString() {
		return "Message [x=" + Arrays.toString(nums) + "]";
	}

}
