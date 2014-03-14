/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 *     http://duracloud.org/license/
 */
package org.duracloud.mill.workman;

import org.duracloud.common.queue.task.Task;
import org.duracloud.common.queue.TaskQueue;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 * @author Daniel Bernstein
 * 
 */
public class TaskWorkerImplTest {
    private Task task;
    private TaskQueue queue;
    private TaskQueue deadLetterQueue;
    private TaskProcessor processor;
    private TaskProcessorFactory factory;

    @Before
    public void setUp() throws Exception {
        task = EasyMock.createMock(Task.class);
        processor = EasyMock.createMock(TaskProcessor.class);
        queue = EasyMock.createMock(TaskQueue.class);
        deadLetterQueue = EasyMock.createMock(TaskQueue.class);

        factory = EasyMock.createMock(TaskProcessorFactory.class);
        EasyMock.expect(task.getVisibilityTimeout()).andReturn(
                1);
        EasyMock.expect(factory.create(EasyMock.isA(Task.class))).andReturn(
                processor);

    }

    @After
    public void tearDown() throws Exception {
        EasyMock.verify(processor, queue, deadLetterQueue, task, factory);
    }

    private void replay() throws Exception {
        EasyMock.replay(processor, queue, deadLetterQueue, task, factory);
    }

    @Test
    public void testRun() throws Exception {
        processor.execute();
        EasyMock.expectLastCall().andAnswer(new IAnswer<Object>() {
            @Override
            public Object answer() throws Throwable {
                Thread.sleep(2000);
                return null;
            }
        });

        queue.extendVisibilityTimeout(EasyMock.isA(Task.class));
        EasyMock.expectLastCall().times(2, 4);
        queue.deleteTask(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();

        replay();
        TaskWorkerImpl w = new TaskWorkerImpl(task, factory, queue, deadLetterQueue);
        w.init();
        w.run();
        // sleep to make sure that the internal timer task is being cancelled.
        Thread.sleep(2000);
    }

    private void runWithProcessorException() throws Exception {
        processor.execute();
        EasyMock.expectLastCall().andThrow(new TaskExecutionFailedException());
        replay();
        TaskWorkerImpl w = new TaskWorkerImpl(task, factory, queue, deadLetterQueue);
        w.init();
        w.run();
        // sleep to make sure that the internal timer task is being cancelled.
        Thread.sleep(3000);
    }

    @Test
    public void testRunWithProcessorExceptionFirstAttempt() throws Exception {
        EasyMock.expect(task.getAttempts()).andReturn(0);
        queue.requeue(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();
        runWithProcessorException();
    }

    @Test
    public void testRunWithProcessorExceptionLastAttempt() throws Exception {
        EasyMock.expect(task.getAttempts()).andReturn(4);
        queue.deleteTask(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();
        deadLetterQueue.put(EasyMock.isA(Task.class));
        EasyMock.expectLastCall();
        runWithProcessorException();
    }

}
