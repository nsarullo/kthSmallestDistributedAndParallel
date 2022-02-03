package nsarullo;

import java.io.Serializable;
 /*The server will send one of these messages to each client.
 * The client will interpret the message's half value to determine
 * whether it is to delete either the numbers in its chunk to left or to the right
 * side of its pivot index.*/
public class HalfDeleteMessage implements Serializable {

	public String toString() 
	{
		return "Message "+ half;
	}

	public static long serialVersionUID=1L;
	
	/*This value will be set by the server and interpreted by the client.
	* if the value = 0.0, the client will interpret that to mean it should delete all elements in its chunk
	* to the left of its pivot index.
	* If the value is 1.0, the client will delete all elements in its chunk
	* to the right of its pivot index.
	* Else if the value is 3.0, the client will begin its shutdown process, as the server is aware it no longer
	* has values in its chunk and is ready (has removed the client from its connections list) for the client to
	* disconnect and shut down.*/
	private double half;

	public HalfDeleteMessage() {
		super();
	}

	public HalfDeleteMessage(double half) {
		this.half = half;
	}

	public double getHalf() {
		return half;
	}
	
}
