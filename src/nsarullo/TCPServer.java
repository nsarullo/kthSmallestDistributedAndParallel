package nsarullo;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;
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
class TCPServer {
    static int numOfClients = 5;
    static int kth = 1_501_000;
    static int savedLeft = 0;
    static int numOfMasterNums = 10_032_033;

    static ObjectInputStream[] inputStreamList = new ObjectInputStream[numOfClients];
    static ObjectOutputStream[] outputStreamList = new ObjectOutputStream[numOfClients];

    public static void main(String argv[]) throws Exception {
        ServerSocket welcomeSocket = new ServerSocket(6789);

        //Accept and store client connections
        for (int i = 0; i < numOfClients; i++) {
            Socket socket = welcomeSocket.accept();
            inputStreamList[i] = new ObjectInputStream(socket.getInputStream());
            outputStreamList[i] = new ObjectOutputStream(socket.getOutputStream());
        }

        double[] masterNums = createMasterNums(numOfMasterNums);
        testResults(masterNums);
        chunkAndDistribute(masterNums, numOfClients);

        while (true) {

            PivotIndexMessage result = pickAndDistributePivot();

            HalfDeleteMessage[] readSaver = new HalfDeleteMessage[inputStreamList.length];
            /*Note - When the server loops through the array of messages it received from each client, one at a time,
            the server will always wait for the message to be received before checking the next array index for the next message.
            Ex: If inputStreamList[0] is the last to finish, the loop won't skip it!
            It will wait until 0 is complete and then continue on to the next indecencies. */
            for (int i = 0; i < inputStreamList.length; i++) {
                readSaver[i] = (HalfDeleteMessage) inputStreamList[i].readObject();
            }
            double leftTally = 0;
            for (int i = 0; i < inputStreamList.length; i++) {
                leftTally = leftTally + readSaver[i].getHalf();
            }

            System.out.println("TT " + (savedLeft + leftTally));

            /*If the kth smallest number index is equal to the current total left tally taken from all the left
            * values of every client, then the kth smallest number has been found and the server can shut down
            * after printing the result and telling all the clients to shut down.*/
            if (kth == leftTally + savedLeft) {
                double answer = result.getPivotValue();
                System.out.println("The answer is " + answer);
                for (int i = 0; i < outputStreamList.length; i++) {
                    outputStreamList[i].writeObject(new HalfDeleteMessage(2)); //end
                }
                welcomeSocket.close();
                break;
            } //The server tells every client to delete the elements to the left of their pivot index.
            if (kth > (leftTally + savedLeft)) {
                savedLeft += leftTally;
                for (int i = 0; i < outputStreamList.length; i++) {
                    outputStreamList[i].writeObject(new HalfDeleteMessage(0));

                }
                System.out.println("Delete Left \n kth > total left Tally");
            } else { //The server tells every client to delete the elements to the right of their pivot index.
                for (int i = 0; i < outputStreamList.length; i++) {
                    outputStreamList[i].writeObject(new HalfDeleteMessage(1));
                }
            }

        }
    }

    /*In this function, the server sends a message to a random client asking that client to pick a pivot value.
    * If the client can pick a random pivot value from its chunk, it will send the pivot value back to the server.
    * The server will then distribute that pivot value to the other clients.
    * On the other hand, if the chosen client already deleted all the numbers in its chunk and cannot pick a pivot,
    * the client will respond with a -Integer.MAX_VALUE as the pivot value, notifying the server that the server
    * needs to select another client and repeat the process until a pivot value is chosen. */
    public static PivotIndexMessage pickAndDistributePivot() throws IOException, ClassNotFoundException {
        while (true) {
            int clientWithPivot;
            Random rand = new Random();
            clientWithPivot = rand.nextInt(numOfClients);
            outputStreamList[clientWithPivot].writeObject(new PivotIndexMessage(1, true));
            PivotIndexMessage clientPivotReturn = (PivotIndexMessage) inputStreamList[clientWithPivot].readObject();
            double value = clientPivotReturn.getPivotValue();
            if (value == -Integer.MAX_VALUE) {
                removeClient(clientWithPivot);
                continue;
            }
            if (clientPivotReturn.getIsThePivot() == true) {
                for (int i = 0; i < outputStreamList.length; i++) {
                    if (!(i == clientWithPivot)) {
                        outputStreamList[i].writeObject(new PivotIndexMessage(value, false));
                    }
                }
                return clientPivotReturn;
            }
        }
    }

    /*This function removes a client from the input and output stream lists by recreating the lists without
    * the removed streams, effectively disconnecting the client from the server.*/
    public static void removeClient(int clientWithPivot) {
        numOfClients--;
        ArrayList<ObjectInputStream> inputStreamWithoutEmpty = new ArrayList<>();
        ArrayList<ObjectOutputStream> outputStreamWithoutEmpty = new ArrayList<>();
        for (int i = 0; i < inputStreamList.length; i++) {
            if (i != clientWithPivot) {
                inputStreamWithoutEmpty.add(inputStreamList[i]);
                outputStreamWithoutEmpty.add(outputStreamList[i]);
            }
        }
        inputStreamList = new ObjectInputStream[inputStreamWithoutEmpty.size()];
        outputStreamList = new ObjectOutputStream[outputStreamWithoutEmpty.size()];
        for (int i = 0; i < inputStreamList.length; i++) {
            inputStreamList[i] = inputStreamWithoutEmpty.get(i);
            outputStreamList[i] = outputStreamWithoutEmpty.get(i);
        }
    }

    /*The server generates an array of doubles
    The array size is must be selected in the code prior to running the program
    The array can theoretically be of size Integer.MAX if your heap size will allow for this*/
    public static double[] createMasterNums(int totalNumOfNums) {
        double[] masterNums = new double[totalNumOfNums];
        Random rand = new Random();
        for (int i = 0; i < masterNums.length; i++) {
            masterNums[i] = rand.nextDouble() * totalNumOfNums + 1;
        }
        return masterNums;
    }

    /*The server determines standard chunk size based on both the size of the master nums array
    and on how many clients connected to the server.*/
    public static void chunkAndDistribute(double[] masterNums, int numOfClients) throws IOException {
        int chunk = masterNums.length / numOfClients;
        int chunkModulo = masterNums.length % numOfClients;
        int startPoint = 0;
        /*The master array is separated into chunks and sent to clients (one chunk per client)*/
        for (int i = 0; i < outputStreamList.length - 1; i++) {
            double[] numsChunk = new double[chunk];
            for (int j = 0; j < numsChunk.length; j++) {
                numsChunk[j] = masterNums[startPoint + j];
            }
            startPoint = startPoint + chunk;
            outputStreamList[i].writeObject(new ChunkMessage(numsChunk));
        }
        /*The final chunk is the size of a standard chunk + the modulo of elements that would
        not fit evenly in standard chunks*/
        double[] finalChunk = new double[chunk + chunkModulo];
        for(int i = 0; i < finalChunk.length; i++){
            finalChunk[i] = masterNums[startPoint+i];
        }
        outputStreamList[outputStreamList.length-1].writeObject(new ChunkMessage(finalChunk));
    }

    /*Automated test function to make sure the function is producing the correct results.
    * This function will throw every number in the masterNums array into a priority queue
    * and pop out each value up until the kth smallest value is reached and is printed.*/
    public static void testResults(double[] masterNums) {
        PriorityQueue<Double> pq = new PriorityQueue<>();
        for (int i = 0; i < masterNums.length; i++) {
            pq.add(masterNums[i]);
        }
        int count = 0;
        while (!pq.isEmpty()) {
            if (count == kth) {
                System.out.println("The Answer Should Be: " + pq.remove());
            } else {
                pq.remove();
            }
            count++;
        }
    }
}

  
  
