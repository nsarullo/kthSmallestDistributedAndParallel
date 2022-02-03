package nsarullo;

public class ParallelPartitionResult {
    /*This is the object returned by the ParallelPartitioner as a future. It will
    * contain the updated chunk of numbers with all the values less than the
    * pivot value before the pivot index, and all those greater than the pivot value
    * after the pivot index. In addition, this object also contains the current index of the pivot value.*/
    double[] chunk;
    int pivotIndex;
    ParallelPartitionResult(double[] chunk, int pivotIndex){
        this.chunk = chunk;
        this.pivotIndex = pivotIndex;
    }
}
