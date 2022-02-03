package nsarullo;

import java.io.Serializable;

public class PivotIndexMessage implements Serializable {
	/*This object will be created by both the server and client at various points.
	* They will both use this object to send a message to one another. The server will use
	* this message to ask a random client to choose a random pivot value from its chunk. The
	* client will use this message to respond with either the random value, or a message signaling that
	* it has run out of numbers. The server will then send another one of these messages to another random
	* client if the original client could not pick a pivot. If the client did pick a pivot, the server will
	* take the pivot it received and send that pivot, using this message, to all the other clients that did
	* not pick a pivot value so that those clients can add the value into their chunk for partitioning.*/
	
	@Override
	public String toString() 
	{
		return "Pivot Value "+ pivotValue;
	}

	public static long serialVersionUID=1L;
	//This value will contain a client-selected pivot value or a -Integer.MAX_VALUE if no value could be selected
	private double pivotValue;
	/*This boolean will be set to true if the client being sent this message
	(or the client responded to a message) has been or is being asked to pick a pivot value*/
	private boolean isThePivot;

	public PivotIndexMessage() {
		super();
	}

	public PivotIndexMessage(double x, boolean isThePivot) {
		this.pivotValue = x;
		this.isThePivot = isThePivot;
	}

	public static void setSerialVersionUID(long serialVersionUID) {
		PivotIndexMessage.serialVersionUID = serialVersionUID;
	}

	public void setPivotValue(double pivotValue) {
		this.pivotValue = pivotValue;
	}

	public void setThePivot(boolean isThePivot) {
		this.isThePivot = isThePivot;
	}

	public double getPivotValue() {
		return pivotValue;
	}
	
	public boolean getIsThePivot() {
		return isThePivot;
		
	}
	
}
