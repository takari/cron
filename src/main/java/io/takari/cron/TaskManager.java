package io.takari.cron;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fc.cron.CronExpression;

public class TaskManager {
  private final Logger logger = LoggerFactory.getLogger(TaskManager.class);
  private final ScheduledExecutorService scheduler;
  private final ExecutorService executor;
  private final Map<String, Future<?>> taskFutures;

  public TaskManager() {
    this(Runtime.getRuntime().availableProcessors() * 2);
  }

  public TaskManager(int workerThreadsForExecutor) {
    executor = new ThreadPoolExecutor(0, workerThreadsForExecutor, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>());
    scheduler = Executors.newSingleThreadScheduledExecutor();
    taskFutures = new HashMap<String, Future<?>>();
  }

  public void executeTask(final Task task) {
    executor.execute(wrapTaskInRunnable(task));
  }

  public void executeTaskOnSchedule(final Task task, final String schedule) {
    executeTaskOnSchedule(task, new CronExpression(schedule));
  }

  public void executeTaskOnSchedule(final Task task, final CronExpression schedule) {
    Runnable taskRunnable = new Runnable() {
      @Override
      public void run() {
        executor.submit(wrapTaskInRunnable(task));
        long delay = delayFor(schedule);
        if (delay >= 0) {
          scheduler.schedule(this, delay, TimeUnit.MILLISECONDS);
        }
      }
    };
    // check if the delay is -1 but we should validate the schedule is valid
    long delay = delayFor(schedule);
    if (delay >= 0) {
      Future<?> taskFuture = scheduler.schedule(taskRunnable, delay, TimeUnit.MILLISECONDS);
      taskFutures.put(task.getId(), taskFuture);
    }
  }

  public void cancelTask(String taskId) {
    Future<?> taskFuture = taskFutures.get(taskId);
    if (taskFuture != null) {
      taskFuture.cancel(true);
    }
  }

  public void stop() throws Exception {
    scheduler.shutdown();
    executor.shutdown();
  }

  private long delayFor(CronExpression schedule) {
    DateTime now = new DateTime();
    DateTime nextValidTimeToExecuteTask = schedule.nextTimeAfter(now);
    if (nextValidTimeToExecuteTask == null) {
      return -1;
    }
    return nextValidTimeToExecuteTask.getMillis() - now.getMillis();
  }

  private Runnable wrapTaskInRunnable(final Task task) {
    return new Runnable() {
      @Override
      public void run() {
        try {
          task.execute();
        } catch (OutOfMemoryError e) {
          logger.error("FATAL error caught, exiting", e);
          System.exit(1);
        } catch (Error e) {
          logger.error("FATAL error caught.", e);
          System.exit(1);
        } catch (Exception e) {
          logger.error("Uncaught exception in SafeRunnable", e);
        } finally {
          //
          // Listener for registering the success/failure of an individual execution of a job
          //
        }
      }
    };
  }

  public static void main(String[] args) throws Exception {
    TaskManager taskExecutor = new TaskManager();
    taskExecutor.executeTaskOnSchedule(new Task() {
      int count = 0;
      @Override
      public String getId() {
        return "executor";
      }

      @Override
      public void execute() {
        count++;
        System.out.println("Executing run " + count);
      }
    }, "0/10 * * * * *");
    Thread.sleep(30000);
    taskExecutor.stop();
  }
}
