package org.nd4j.linalg.aggregates;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.nd4j.linalg.BaseNd4jTest;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.api.ops.aggregates.Batch;
import org.nd4j.linalg.api.ops.aggregates.impl.AggregateAxpy;
import org.nd4j.linalg.api.ops.aggregates.impl.SkipGram;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.factory.Nd4jBackend;

import static org.junit.Assert.assertEquals;

/**
 * @author raver119@gmail.com
 */
@RunWith(Parameterized.class)
public class AggregatesTests extends BaseNd4jTest {

    public AggregatesTests(Nd4jBackend backend) {
        super(backend);
    }

    @Test
    public void testAggregate1() throws Exception {
        INDArray arrayX = Nd4j.ones(10);
        INDArray arrayY = Nd4j.zeros(10);

        INDArray exp1 = Nd4j.ones(10);

        AggregateAxpy axpy = new AggregateAxpy(arrayX, arrayY, 1.0f);

        Nd4j.getExecutioner().exec(axpy);

        assertEquals(exp1, arrayY);
    }


    @Test
    public void testBatchedAggregate1() throws Exception {
        INDArray arrayX1 = Nd4j.ones(10);
        INDArray arrayY1 = Nd4j.zeros(10);

        INDArray arrayX2 = Nd4j.ones(10);
        INDArray arrayY2 = Nd4j.zeros(10);

        INDArray exp1 = Nd4j.create(10).assign(1f);
        INDArray exp2 = Nd4j.create(10).assign(1f);

        AggregateAxpy axpy1 = new AggregateAxpy(arrayX1, arrayY1, 1.0f);
        AggregateAxpy axpy2 = new AggregateAxpy(arrayX2, arrayY2, 1.0f);

        Batch batch = new Batch();
        batch.enqueueAggregate(axpy1);
        batch.enqueueAggregate(axpy2);

        Nd4j.getExecutioner().exec(batch);

        assertEquals(exp1, arrayY1);
        assertEquals(exp2, arrayY2);
    }

    @Test
    public void testBatchedAggregate2() throws Exception {
        INDArray arrayX1 = Nd4j.ones(10);
        INDArray arrayY1 = Nd4j.zeros(10).assign(2.0f);

        INDArray arrayX2 = Nd4j.ones(10);
        INDArray arrayY2 = Nd4j.zeros(10).assign(2.0f);

        INDArray arrayX3 = Nd4j.ones(10);
        INDArray arrayY3 = Nd4j.ones(10);

        INDArray exp1 = Nd4j.create(10).assign(4f);
        INDArray exp2 = Nd4j.create(10).assign(3f);
        INDArray exp3 = Nd4j.create(10).assign(3f);

        AggregateAxpy axpy1 = new AggregateAxpy(arrayX1, arrayY1, 2.0f);
        AggregateAxpy axpy2 = new AggregateAxpy(arrayX2, arrayY2, 1.0f);
        AggregateAxpy axpy3 = new AggregateAxpy(arrayX3, arrayY3, 2.0f);

        Batch batch = new Batch();
        batch.enqueueAggregate(axpy1);
        batch.enqueueAggregate(axpy2);
        batch.enqueueAggregate(axpy3);

        Nd4j.getExecutioner().exec(batch);

        assertEquals(exp1, arrayY1);
        assertEquals(exp2, arrayY2);
        assertEquals(exp3, arrayY3);
    }

    @Test
    public void testBatchedSkipGram1() throws Exception {
        INDArray syn0 = Nd4j.create(10, 10).assign(0.01f);
        INDArray syn1 = Nd4j.create(10, 10).assign(0.02f);
        INDArray syn1Neg = Nd4j.ones(10, 10).assign(0.03f);
        INDArray expTable = Nd4j.create(10000).assign(0.5f);

        double lr = 0.001;

        int idxSyn0_1 = 0;
        int idxSyn0_2 = 3;

        INDArray expSyn0 = Nd4j.create(10).assign(0.01f);
        INDArray expSyn1_1 = Nd4j.create(10).assign(0.020005); // gradient is 0.00005
        INDArray expSyn1_2 = Nd4j.create(10).assign(0.019995f); // gradient is -0.00005


        INDArray syn0row_1 = syn0.getRow(idxSyn0_1);
        INDArray syn0row_2 = syn0.getRow(idxSyn0_2);

        SkipGram op1 = new SkipGram(syn0, syn1, syn1Neg, expTable, idxSyn0_1, new int[]{1, 2}, new int[]{0, 1}, 0, 0, 10, lr);
        SkipGram op2 = new SkipGram(syn0, syn1, syn1Neg, expTable, idxSyn0_2, new int[]{4, 5}, new int[]{0, 1}, 0, 0, 10, lr);


        Batch batch = new Batch();
        batch.enqueueAggregate(op1);
        batch.enqueueAggregate(op2);

        Nd4j.getExecutioner().exec(batch);

        /*
            Since expTable contains all-equal values, and only difference for ANY index is code being 0 or 1, syn0 row will stay intact,
            because neu1e will be full of 0.0f, and axpy will have no actual effect
         */
        assertEquals(expSyn0, syn0row_1);
        assertEquals(expSyn0, syn0row_2);

        // syn1 row 1 modified only once
        assertEquals(expSyn1_1, syn1.getRow(1));
        assertEquals(expSyn1_1, syn1.getRow(4));

        // syn1 row 2 modified only once
        assertEquals(expSyn1_2, syn1.getRow(2));
        assertEquals(expSyn1_2, syn1.getRow(5));
    }

    @Override
    public char ordering() {
        return 'c';
    }
}