package nsarullo;

import java.util.concurrent.Callable;

public class ParallelPartitioner implements Callable<ParallelPartitionResult> {
    /*This is the chunk of numbers a thread running a ParallelPartitioner will receive.
    * This chunk is a division of the larger chunk in the client that created this object.*/
    double[] chunk;
    ParallelPartitioner(double[] chunk){
        this.chunk = chunk;
    }

    //This function swaps two values in the chunk array
    public void swap(int left, int right) {
        double temp = chunk[left];
        chunk[left] = chunk[right];
        chunk[right] = temp;
    }

    public double[] getChunk(){
        return chunk;
    }

    @Override
    /*This is the callable that will return the future.
    * All elements less than the pivot value (at the end of the array)
    * are swapped to the beginning, one after the other, in no particular order.
    * The pivot value is then swapped after these values less than it, resulting in
    * an array with all the values less than the pivot value in front of the pivot value and
    * all the values greater than the pivot value after it.*/
    public ParallelPartitionResult call() throws Exception {
        //We start at opposite ends of the array
        int left = 0;
        /*The pivotIndex will always be the final element in the array.
        * For any chunks that did not start with the pivot value and had it added,
        * I added it to the end of the array.
        * For the chunk that did start with the pivot value, it was already swapped to the
        * end before the division of the client's main chunk.*/
        int pivotIndex = chunk.length - 1;
        if(chunk.length == 1){
            return new ParallelPartitionResult(chunk, pivotIndex);
        }
        double pivotValue = chunk[pivotIndex];
        int newPivotIndex = left;
        /*Loop from the start of the chunk to the end.
        * If the current value is less than the pivot value,
        * swap that value to the beginning of the array after all
        * other values that we've already found to be less than the pivot value
        * (if we haven't found any less than the pivot value yet, the value less than
        * the pivot value would be swapped to position 0 of the array)*/
        for (int i = left; i < pivotIndex; i++) {
            if (chunk[i] <= pivotValue) {
                swap(newPivotIndex, i);
                newPivotIndex++;
            }
        }
        //Move the pivot to its correct location, after all the values less than it
        swap(pivotIndex, newPivotIndex);
        //return the future object with the updated chunk and the updated pivotIndex
        return new ParallelPartitionResult(chunk, newPivotIndex);
    }
}
