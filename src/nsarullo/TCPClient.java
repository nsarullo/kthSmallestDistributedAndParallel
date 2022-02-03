package nsarullo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
/*General Synopsis of the Program:
    *Interaction Loop
        *Server asks random client to choose pivot value from its chunk
        *Client chooses pivot unless unable
			*Client responds to server
			* If didn't pick, the server asks another client to pick the pivot value
        * Server receives pivot value from the client that chose it
            * Server sends the pivot value to the other clients that did not pick it
        * Clients add pivot value to their chunks, so they can partition around it in parallel
            * The clients then partition around pivot value
            * Next, the clients send a message to server telling the server how many
              numbers to left of pivot index in their chunk
		* Server adds all tallies and calculates how many numbers are to the left of the pivot index,
		   all clients, all chunks considered
			* If kth index > total tally, the server will tell all of the clients to delete the values to the left
			  of their pivot index
			    * Server will store the tally of left values being deleted for use in further calculations later
            *  Else if kth index < total left tally
                * The server will tell all of the clients to delete the values to the right
			      of their pivot index
          * Then we repeat this loop and the server asks another random client to choose a pivot
*/
class TCPClient {
	/*True means the client is currently choosing or has just chosen the pivot value.
	* False means this client will be sent or currently contains a temporary duplicate pivot value
	* which will need to be deleted after partitioning.*/
    static boolean iAmPivot = false;
	//The current location of the pivot value within this client's chunk
    static int pivotIndex;
	//How many values are to the left of the pivot index after partitioning
    static double left;
	//The main array (chunk) of doubles received from the server
    static double[] receivedNums;
	//How many cores the system has available
    static int numOfCores = Runtime.getRuntime().availableProcessors();
	//One thread will be created per core for use in the parallel partitioning process
    static ExecutorService threadPool = Executors.newFixedThreadPool(numOfCores);

    public static void main(String argv[]) throws Exception {
        //Connect to the server and establish I/O streams
        Socket clientSocket = new Socket("Enter IPv4 Here", 6789);
        ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
        ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
        //Wait for the server to send the client a chunk of doubles and read it
        ChunkMessage chunkMessage = (ChunkMessage) inputStream.readObject();
        receivedNums = chunkMessage.getNums();
        //Enter the main loop where all work will occur until the client shuts down
        findKth(inputStream, outputStream);
        /*Client's chunk has run out of numbers or the kth smallest number
         * has been determined so the client disconnects from the server and shuts down.*/
        inputStream.close();
        outputStream.close();
        clientSocket.close();
        threadPool.shutdown();
        System.out.println("Successfully shut down.");
    }

	/*Main loop where all the work in this client will occur and all other messages (after receiving the chunk above)
	* to and from the server will be sent and received.*/
	public static void findKth(ObjectInputStream inputStream, ObjectOutputStream outputStream) throws IOException, ClassNotFoundException, InterruptedException, ExecutionException {
		while (true) {
			/*Choose a pivot value if this client is selected to do so.
			* If not, receive the pivot value sent by the server that came
			* from another client.*/
			pivotIndex = choosePivot(inputStream, outputStream);
			/*If the pivot received or chosen is == -Integer.MAX_VALUE, this client's
			* chunk is empty and needs to shut down, so the loop is broken to do so.*/
			if (pivotIndex == -Integer.MAX_VALUE) {
				break;
			}
			/*Partition the chunk in parallel until all values < pivotValue are to the left of the pivot index and
			all values > pivotValue are to the right of the pivot index*/
			partition();
			/*If the server has sent a message with a double value of 2.0, telling it to shut down, the loop will break
			* here and the client will shut down. Else if the chunk runs out of numbers, the client will also be shut down.
			* Else, the loop will continue after deleting the numbers to the left or right of the pivot index in the chunk.*/
			if (!sendLeftReceiveHalf(inputStream, outputStream)) {
				break;
			}
		}
	}

	/*Here, the server will either request this client to choose a pivot value or send the client a pivot to add to its chunk.
	* If the client can choose a pivot value, it will do so and send it to the server so the server can distribute the
	* pivot value to all the other clients that did not pick a pivot value. Those clients will then add the pivot value
	* to their chunks, so they can partition around it.
	* If the client is selected to choose a pivot and cannot because it has already deleted all the doubles in its chunk,
	* the client will respond to the server with a -Integer.MAX_VALUE, notifying the server that it could not choose a pivot value
	* and the server will have to ask another client to choose the pivot value. If this happens, this client will then shut down and
	* the server will remove the client from its list of connections.*/
    public static int choosePivot(ObjectInputStream inputStream, ObjectOutputStream outputStream) throws IOException, ClassNotFoundException {
        int pivotIndex;
		//Read the PivotIndexMessage from the server
		PivotIndexMessage checkIfPivot = (PivotIndexMessage) inputStream.readObject();
		//This client is chosen to pick a pivot index from its chunk and send the pivot value at that index to the server
		if (checkIfPivot.getIsThePivot() == true) {
			//If this client's chunk is empty, fail case, let the server know the client can't choose a pivot index or value and begin to shut down
			if (receivedNums.length == 0) {
				iAmPivot = false;
				PivotIndexMessage error = new PivotIndexMessage(-Integer.MAX_VALUE, true);
				outputStream.writeObject(error); //Tell the server we failed
				return -Integer.MAX_VALUE;
			}
			//Else, this client has a valid array to select a pivot, so it begins to do so at random
			iAmPivot = true;
			Random rand = new Random();
			pivotIndex = rand.nextInt(receivedNums.length);
			PivotIndexMessage pivotIndexMessageToSend = new PivotIndexMessage(receivedNums[pivotIndex], true);
			//This client sends the randomly chosen pivot value to the server.
			outputStream.writeObject(pivotIndexMessageToSend);

		} else {//Else, another client found a pivot, so this client adds the pivot to a new array
			iAmPivot = false;
			double[] receivedNumsPlusPivot = new double[receivedNums.length + 1];
			for (int i = 0; i < receivedNums.length; i++) {
				receivedNumsPlusPivot[i] = receivedNums[i];
			}
			receivedNumsPlusPivot[receivedNumsPlusPivot.length - 1] = checkIfPivot.getPivotValue();
			pivotIndex = receivedNumsPlusPivot.length - 1;
			receivedNums = new double[receivedNumsPlusPivot.length];
			for (int i = 0; i < receivedNums.length; i++) {
				receivedNums[i] = receivedNumsPlusPivot[i];
			}
		}
		return pivotIndex;
    }

	/*Parallel partitioning process where the chunk is broken down into smaller chunks and
	partitioned utilizing one thread per system core (and per smaller chunk) until all values less
	than the pivotValue are to the left of the pivot index and all values greater than the pivotValue
	are	to the right of the pivot index.*/
	public static void partition() throws InterruptedException, ExecutionException {
		//If the pivot index is not already at the end of the chunk, move it there
		if (pivotIndex != receivedNums.length-1) {
			swap(receivedNums, pivotIndex, receivedNums.length - 1);
			pivotIndex = receivedNums.length - 1;
		}
		//Determine standard chunk size based on available cores
		int chunkSize = receivedNums.length / (numOfCores);
		//Final chunkSize is standard chunkSize + modulo
		int finalChunkSize = chunkSize + (int) (receivedNums.length % numOfCores);
		//Create list of ParallelPartitioner callables to eventually be invoked
		ArrayList<ParallelPartitioner> parallelPartitioners = new ArrayList<>();
		//startPoint of the main chunk
		int startPoint = 0;
		double pivotValue = receivedNums[pivotIndex];
		/*For every thread except the final thread make a new small chunk, fill the chunk with doubles from the
		 * main chunk, create a new parallel partitioner with that chunk, and add it to the list of parallelPartitioners.*/
		for (int i = 0; i < numOfCores - 1; i++) {
			double[] chunk;
			chunk = new double[chunkSize + 1];
			for (int j = 0; j < chunkSize; j++) {
				chunk[j] = receivedNums[startPoint + j];
			}
			startPoint = startPoint + chunkSize;
			if (chunk.length > 0) {
				chunk[chunk.length - 1] = receivedNums[pivotIndex];
				parallelPartitioners.add(new ParallelPartitioner(chunk));
			}
		}
		//Create and fill the final chunk, then add it to a new ParallelParitioner
		double[] finalChunk = new double[finalChunkSize];
		for (int j = 0; j < finalChunk.length; j++) {
			finalChunk[j] = receivedNums[startPoint + j];
		}
		//Add the last ParallelPartitioner to the list of Parallel Partitioners
		parallelPartitioners.add(new ParallelPartitioner(finalChunk));
		//Invoke all the parallelParitioners so the threads run and partitioning begins
		List<Future<ParallelPartitionResult>> futures = threadPool.invokeAll(parallelPartitioners);
		//startPoint of the main chunk again
		startPoint = 0;
		/*Here, the main chunk in the client is overwritten with doubles from the post partitioned small chunks from the
		 * futures that are returned by the threads as they finish running their callables. For each future's chunk
		 * (following the order of futures in the list of futures), every number before the pivot index is gathered and
		 * added back to the main array in sequential order of the main arrays elements (overwriting each double in that
		 * position).*/
		for (Future<ParallelPartitionResult> future : futures) {
			for (int i = 0; i < future.get().pivotIndex; i++) {
				receivedNums[startPoint + i] = future.get().chunk[i];
			}
			startPoint = startPoint + future.get().pivotIndex;
		}
		//Now that the numbers before the pivot are added, add the pivot back
		left = startPoint;
		receivedNums[startPoint] = pivotValue;
		pivotIndex = startPoint;
		startPoint++;
		//Continue adding all the numbers after the pivot index in each future, sequentially
		for (Future<ParallelPartitionResult> future : futures) {
			for (int i = future.get().pivotIndex + 1; i < future.get().chunk.length; i++) {
				receivedNums[startPoint] = future.get().chunk[i];
				startPoint++;
			}
		}
	}

    public static boolean sendLeftReceiveHalf(ObjectInputStream inputStream, ObjectOutputStream outputStream) throws IOException, ClassNotFoundException {
		/*If the pivot index is equal to -Integer.MAX_VALUE then there is no left tally to send, as all numbers in this
		* client's chunk have already been deleted. False is returned, the outer loop is broken, and the client begins
		* to shut down.*/
        if (pivotIndex == -Integer.MAX_VALUE) {
            return false;
        }
		/*The client sends a message to the server containing the tally of how many elements are currently to the left
		of the pivot index*/
        HalfDeleteMessage halfDeleteMessage = new HalfDeleteMessage(left);
        outputStream.writeObject(halfDeleteMessage);
		/*The server tells this client (and every other client) whether to delete the numbers to the left or to the
		 * right of their pivot index.*/
        HalfDeleteMessage receivedSideToDelete = (HalfDeleteMessage) inputStream.readObject();
        double[] tempNums = new double[receivedNums.length];
        for (int i = 0; i < tempNums.length; i++) {
            tempNums[i] = receivedNums[i];
        }
        int leftI = (int) left;
		//Delete Right: Worker saves all the numbers to the left of the pivot index
        if (receivedSideToDelete.getHalf() == 1.0) {
			//If this client has the original pivot value, add an extra space for it to be added into the new array
            if (iAmPivot) {
                receivedNums = new double[leftI + 1];
            } else {
                receivedNums = new double[leftI];
            }
            for (int i = 0; i < receivedNums.length; i++) {
                receivedNums[i] = tempNums[i];
            }
            return true;
        }//Delete Left: Worker saves all the numbers to the right of the pivot index
		else if (receivedSideToDelete.getHalf() == 0.0) {
            if (iAmPivot && tempNums.length - leftI > 0) {
                receivedNums = new double[tempNums.length - leftI];
            } else {
                receivedNums = new double[tempNums.length - (leftI + 1)];
                leftI++;
            }
            for (int i = 0; i < receivedNums.length; i++) {
                receivedNums[i] = tempNums[i + leftI];
            }
            return true;
        } else {
            return false;
        }
    }

	/*Used to swap the pivot value to the final position of the array
	 * before partitioning, if it is not already in the final position.
	 * i.e. if this client chose the pivot value.*/
	public static void swap(double[] array, int left, int right) {
		double temp = array[left];
		array[left] = array[right];
		array[right] = temp;
	}

}